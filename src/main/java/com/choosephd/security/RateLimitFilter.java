package com.choosephd.security;

import com.choosephd.common.ApiResult;
import com.choosephd.entity.ScrapeAudit;
import com.choosephd.repository.ScrapeAuditMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Order(Ordered.RATE_LIMIT)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** 列表 API 限流 30 req/min（未登录 IP 维度）。 */
    private static final long LIST_LIMIT_PER_MINUTE = 30;

    /** 默认 API 限流 60 req/min。 */
    private static final long DEFAULT_LIMIT_PER_MINUTE = 60;

    /** 登录用户每日列表请求上限（防脚本用 JWT 批量抓取）。 */
    private static final long USER_DAILY_LIST_LIMIT = 500;

    /** 登录用户每日总请求上限。 */
    private static final long USER_DAILY_TOTAL_LIMIT = 2000;

    /** 新注册账号（24h内）每日列表请求上限 — 仅为正常用户的 1/5。 */
    private static final long NEW_USER_DAILY_LIST_LIMIT = 100;

    /** 新注册账号每日总请求上限。 */
    private static final long NEW_USER_DAILY_TOTAL_LIMIT = 400;

    /** 单 IP 每日列表请求总上限（跨所有账号合计）。 */
    private static final long IP_DAILY_LIST_LIMIT = 1000;

    /** 单 IP 每日总请求上限（跨所有账号合计）。 */
    private static final long IP_DAILY_TOTAL_LIMIT = 5000;

    private final ConcurrentHashMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    /** 登录用户每日请求计数：key = userId */
    private final ConcurrentHashMap<Long, DailyCounter> userDailyCounters = new ConcurrentHashMap<>();

    /** IP 每日请求计数（跨账号合计）：key = ip */
    private final ConcurrentHashMap<String, DailyCounter> ipDailyCounters = new ConcurrentHashMap<>();

    private final ScrapeAuditMapper scrapeAuditMapper;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(ScrapeAuditMapper scrapeAuditMapper, JwtService jwtService) {
        this.scrapeAuditMapper = scrapeAuditMapper;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();

        // ---- 第 1 关：IP 维度 token bucket（每分钟） ----
        long limit = isListPath(path) ? LIST_LIMIT_PER_MINUTE : DEFAULT_LIMIT_PER_MINUTE;
        String bucketKey = ip + "|" + (isListPath(path) ? "list" : "default");

        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> new BucketEntry(createBucket(limit))).bucket;
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L + 1;
            log.warn("Rate limit reject (IP/min): ip={} path={} retry_after={}s", ip, path, retryAfterSeconds);
            writeAudit(ip, request, 429, "rate_limit");
            writeTooManyRequests(response, retryAfterSeconds);
            return;
        }

        // ---- 第 2 关：IP 每日总量（跨所有账号合计） ----
        DailyCounter ipDay = ipDailyCounters.computeIfAbsent(ip, k -> new DailyCounter());
        long ipUsed;
        if (isListPath(path)) {
            ipUsed = ipDay.incList();
            if (ipUsed > IP_DAILY_LIST_LIMIT) {
                log.warn("Rate limit reject (IP daily list): ip={} used={}", ip, ipUsed);
                writeAudit(ip, request, 429, "ip_daily_list_limit");
                writeTooManyRequests(response, 3600);
                return;
            }
        }
        ipUsed = ipDay.incTotal();
        if (ipUsed > IP_DAILY_TOTAL_LIMIT) {
            log.warn("Rate limit reject (IP daily total): ip={} used={}", ip, ipUsed);
            writeAudit(ip, request, 429, "ip_daily_total_limit");
            writeTooManyRequests(response, 3600);
            return;
        }

        // ---- 第 3 关：登录用户每日配额（区分新老用户） ----
        UserJwtInfo jwtInfo = extractUserInfo(request);
        if (jwtInfo != null) {
            long listCap = jwtInfo.isNewAccount ? NEW_USER_DAILY_LIST_LIMIT : USER_DAILY_LIST_LIMIT;
            long totalCap = jwtInfo.isNewAccount ? NEW_USER_DAILY_TOTAL_LIMIT : USER_DAILY_TOTAL_LIMIT;

            DailyCounter counter = userDailyCounters.computeIfAbsent(jwtInfo.userId, k -> new DailyCounter());
            long used;
            if (isListPath(path)) {
                used = counter.incList();
                if (used > listCap) {
                    log.warn("Rate limit reject (user daily list): userId={} isNew={} used={} cap={}",
                            jwtInfo.userId, jwtInfo.isNewAccount, used, listCap);
                    writeAudit(ip, request, 429, "user_daily_list_limit");
                    writeTooManyRequests(response, 3600);
                    return;
                }
            }
            used = counter.incTotal();
            if (used > totalCap) {
                log.warn("Rate limit reject (user daily total): userId={} isNew={} used={} cap={}",
                        jwtInfo.userId, jwtInfo.isNewAccount, used, totalCap);
                writeAudit(ip, request, 429, "user_daily_total_limit");
                writeTooManyRequests(response, 3600);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从 JWT 提取 userId 和新号标记（createdAt < 24h 前）。
     * 返回 null 表示无有效 JWT。
     */
    private UserJwtInfo extractUserInfo(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = authHeader.substring(7);
            if (!jwtService.validateToken(token)) return null;
            Claims claims = jwtService.parseToken(token);
            Long userId = Long.valueOf(claims.getSubject());

            // 检查账号创建时间：< 24h → 新号观察期
            boolean isNew = false;
            String createdAtStr = claims.get("createdAt", String.class);
            if (createdAtStr != null) {
                LocalDateTime createdAt = LocalDateTime.parse(createdAtStr);
                isNew = createdAt.isAfter(LocalDateTime.now().minusHours(24));
            }

            return new UserJwtInfo(userId, isNew);
        } catch (Exception e) {
            return null;
        }
    }

    private record UserJwtInfo(Long userId, boolean isNewAccount) {}

    private Bucket createBucket(long perMinute) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(perMinute)
                        .refillIntervally(perMinute, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private static boolean isListPath(String path) {
        return path.startsWith("/api/v1/universities") && !path.contains("/export");
    }

    private void writeAudit(String ip, HttpServletRequest req, int statusCode, String reason) {
        try {
            ScrapeAudit audit = new ScrapeAudit();
            audit.setId(idSequence.incrementAndGet());
            audit.setIp(ip);
            String ua = req.getHeader("User-Agent");
            audit.setUserAgent(ua != null && ua.length() > 500 ? ua.substring(0, 500) : ua);
            audit.setMethod(req.getMethod());
            String path = req.getRequestURI();
            audit.setPath(path != null && path.length() > 500 ? path.substring(0, 500) : path);
            audit.setStatusCode(statusCode);
            audit.setRejectReason(reason);
            audit.setCreatedAt(LocalDateTime.now());
            scrapeAuditMapper.insert(audit);
        } catch (Exception e) {
            log.error("scrape_audit insert failed: status={} reason={}", statusCode, reason, e);
        }
    }

    private void writeTooManyRequests(HttpServletResponse response, long retryAfter) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResult<Void> body = ApiResult.error(429, "Too Many Requests, retry after " + retryAfter + "s");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private record BucketEntry(Bucket bucket, long lastAccessMs) {
        BucketEntry(Bucket bucket) {
            this(bucket, System.currentTimeMillis());
        }
    }

    /**
     * 每日请求计数器。跨天自动重置。
     */
    private static class DailyCounter {
        volatile LocalDate date = LocalDate.now();
        final AtomicLong listCount = new AtomicLong(0);
        final AtomicLong totalCount = new AtomicLong(0);

        long incList() {
            resetIfNewDay();
            return listCount.incrementAndGet();
        }

        long incTotal() {
            resetIfNewDay();
            return totalCount.incrementAndGet();
        }

        private void resetIfNewDay() {
            LocalDate today = LocalDate.now();
            if (!today.equals(date)) {
                synchronized (this) {
                    if (!today.equals(date)) {
                        date = today;
                        listCount.set(0);
                        totalCount.set(0);
                    }
                }
            }
        }
    }
}
