package com.choosephd.security;

import com.choosephd.common.ApiResult;
import com.choosephd.entity.ScrapeAudit;
import com.choosephd.repository.ScrapeAuditMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
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
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * L3 频率限制 filter — IP 维度 token bucket，超限返 429。
 *
 * <p>执行顺序在 {@link com.choosephd.security.Ordered#ANTI_SCRAPE} 之后，
 * 已通过 UA 黑名单的合法请求才会进入限流。
 *
 * <p>限流规则（按路径前缀区分）：
 * <ul>
 *   <li>{@code /api/v1/universities}（列表） — 30 req/min/IP</li>
 *   <li>其他 API — 60 req/min/IP</li>
 * </ul>
 *
 * <p>超出限流后：
 * <ol>
 *   <li>返 429 + JSON 响应，告知 Retry-After</li>
 *   <li>写入 {@code scrape_audit} 表（IP/路径/原因 "rate_limit"）</li>
 * </ol>
 *
 * <p>桶内存 ConcurrentHashMap 保存，每 5 分钟清理 idle 桶（> 10 分钟未访问即回收）。
 *
 * @see com.choosephd.security.AntiScrapeFilter
 * @see com.choosephd.entity.ScrapeAudit
 */
@Component
@Order(Ordered.RATE_LIMIT)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** 列表 API 限流 30 req/min。 */
    private static final long LIST_LIMIT_PER_MINUTE = 30;

    /** 默认 API 限流 60 req/min。 */
    private static final long DEFAULT_LIMIT_PER_MINUTE = 60;

    /** IP + bucket key 缓存。 */
    private final ConcurrentHashMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();

    /** 简单 ID 生成器（基于毫秒 + 计数器，全局递增）。 */
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    private final ScrapeAuditMapper scrapeAuditMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(ScrapeAuditMapper scrapeAuditMapper) {
        this.scrapeAuditMapper = scrapeAuditMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();
        long limit = isListPath(path) ? LIST_LIMIT_PER_MINUTE : DEFAULT_LIMIT_PER_MINUTE;
        String bucketKey = ip + "|" + (isListPath(path) ? "list" : "default");

        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> new BucketEntry(createBucket(limit))).bucket;
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L + 1;
        log.warn("Rate limit reject: ip={} path={} retry_after={}s", ip, path, retryAfterSeconds);
        writeAudit(ip, request, 429, "rate_limit");
        writeTooManyRequests(response, retryAfterSeconds);
    }

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

    /** 桶条目，附带最后访问时间用于 idle 清理。 */
    private record BucketEntry(Bucket bucket, long lastAccessMs) {
        BucketEntry(Bucket bucket) {
            this(bucket, System.currentTimeMillis());
        }
    }
}
