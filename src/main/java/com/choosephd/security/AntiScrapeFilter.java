package com.choosephd.security;

import com.choosephd.common.ApiResult;
import com.choosephd.entity.ScrapeAudit;
import com.choosephd.repository.ScrapeAuditMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * 反爬虫 UA 黑名单拦截器 — 在 Spring Security 链前执行，过滤掉明显脚本化请求。
 *
 * <p>触发规则（任一命中即返 403）：
 * <ul>
 *   <li>User-Agent 缺失或为空字符串</li>
 *   <li>匹配预编译的脚本 UA 关键词（python-requests / curl / wget / scrapy / HeadlessChrome
 *       / PhantomJS / Selenium / Puppeteer / Playwright / Go-http-client / okhttp / axios）</li>
 * </ul>
 *
 * <p>真实浏览器（Chrome / Safari / Firefox）的 UA 含 {@code Mozilla/5.0} 等合法前缀不会被拦。
 * 仅过滤可执行脚本与自动化框架的明显标记，避免误伤真实用户。
 *
 * <p>命中后按项目统一 {@link ApiResult} 格式返 JSON，{@code Content-Type: application/json;charset=UTF-8}，
 * 与 {@link AuthInterceptor} 保持一致；同时写入 {@code scrape_audit} 表供 admin 审计与告警。
 *
 * @see com.choosephd.security.SecurityConfig
 * @see com.choosephd.security.RateLimitFilter
 */
@Component
@Order(Ordered.ANTI_SCRAPE)
public class AntiScrapeFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AntiScrapeFilter.class);

    /** 预编译的脚本 UA 关键词 — 大小写不敏感。 */
    private static final List<Pattern> BLOCKED_UA_PATTERNS = List.of(
            Pattern.compile("python-requests", Pattern.CASE_INSENSITIVE),
            Pattern.compile("python-urllib", Pattern.CASE_INSENSITIVE),
            Pattern.compile("curl/", Pattern.CASE_INSENSITIVE),
            Pattern.compile("wget/", Pattern.CASE_INSENSITIVE),
            Pattern.compile("scrapy", Pattern.CASE_INSENSITIVE),
            Pattern.compile("HeadlessChrome", Pattern.CASE_INSENSITIVE),
            Pattern.compile("PhantomJS", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Selenium", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Puppeteer", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Playwright", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Go-http-client", Pattern.CASE_INSENSITIVE),
            Pattern.compile("okhttp", Pattern.CASE_INSENSITIVE),
            Pattern.compile("axios/", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Apache-HttpClient", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Java/", Pattern.CASE_INSENSITIVE)
    );

    private final ScrapeAuditMapper scrapeAuditMapper;

    /** 简单 ID 生成器（基于毫秒 + 计数器，全局递增）。 */
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AntiScrapeFilter(ScrapeAuditMapper scrapeAuditMapper) {
        this.scrapeAuditMapper = scrapeAuditMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ua = request.getHeader("User-Agent");
        String reason = matchBlockedReason(ua);
        if (reason != null) {
            log.warn("Anti-scrape reject: ip={} ua=\"{}\" path={} reason={}",
                    request.getRemoteAddr(), ua, request.getRequestURI(), reason);
            writeAudit(request, 403, reason);
            writeForbidden(response, reason);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 命中规则则返回原因字符串；未命中返回 null。
     * 暴露为包内可见，方便未来扩展允许名单或动态规则。
     */
    String matchBlockedReason(String ua) {
        if (ua == null || ua.trim().isEmpty()) {
            return "Missing User-Agent";
        }
        for (Pattern p : BLOCKED_UA_PATTERNS) {
            if (p.matcher(ua).find()) {
                return "Blocked UA pattern: " + p.pattern();
            }
        }
        return null;
    }

    private void writeAudit(HttpServletRequest request, int statusCode, String reason) {
        try {
            ScrapeAudit audit = new ScrapeAudit();
            audit.setId(idSequence.incrementAndGet());
            audit.setIp(request.getRemoteAddr());
            String ua = request.getHeader("User-Agent");
            audit.setUserAgent(ua != null && ua.length() > 500 ? ua.substring(0, 500) : ua);
            audit.setMethod(request.getMethod());
            String path = request.getRequestURI();
            audit.setPath(path != null && path.length() > 500 ? path.substring(0, 500) : path);
            audit.setStatusCode(statusCode);
            audit.setRejectReason(reason.length() > 100 ? reason.substring(0, 100) : reason);
            audit.setCreatedAt(LocalDateTime.now());
            scrapeAuditMapper.insert(audit);
        } catch (Exception e) {
            log.error("scrape_audit insert failed", e);
        }
    }

    private void writeForbidden(HttpServletResponse response, String reason) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResult<Void> body = ApiResult.error(403, "Forbidden: " + reason);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
