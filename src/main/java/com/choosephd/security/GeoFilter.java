package com.choosephd.security;

import com.choosephd.geo.CountryCode;
import com.choosephd.geo.GeoContext;
import com.choosephd.geo.GeoContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * 地理信息 filter — 解析客户端国家，写入 {@link GeoContextHolder}。
 *
 * <p>解析优先级（生产部署到 nginx/CDN 后通常由反向代理写入 header）：
 * <ol>
 *   <li>请求 header {@code X-Country} — ISO 3166-1 alpha-2（nginx/CDN 写入最稳）</li>
 *   <li>请求 header {@code CF-IPCountry} — Cloudflare 专用</li>
 *   <li>请求 header {@code X-Forwarded-For} 首段 — 仅开发环境，本地 127.0.0.1 视为 CN</li>
 *   <li>兜底 {@link CountryCode#UNKNOWN}（不影响业务，前端按默认配置渲染）</li>
 * </ol>
 *
 * <p>本过滤器不引入 MaxMind GeoIP2 依赖 — 留学站 90% 流量来自国内，
 * 头部传播已足够；未来如需精确度提升，单独引入 GeoLite2-Country.mmdb 即可。
 *
 * <p>执行顺序：在 {@link AntiScrapeFilter} (200) 之后，避免恶意请求触发
 * Geo 解析浪费算力；放在 {@link RateLimitFilter} 之后则限流前拿到 Geo 信息
 * （按 IP 国家差异化限流）。
 */
@Component
@Order(Ordered.GEO)
public class GeoFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GeoFilter.class);

    /** 本机 IPv4 回环（开发环境） */
    private static final Pattern LOCALHOST_V4 = Pattern.compile("^(127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|0:0:0:0:0:0:0:1|::1)$");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        GeoContext ctx = resolveContext(request);
        GeoContextHolder.set(ctx);
        // 把国家代码写到 response header，前端可读取做客户端 fallback
        response.setHeader("X-Resolved-Country", ctx.getCountryCode());
        filterChain.doFilter(request, response);
    }

    /**
     * 解析 GeoContext — 包内可见方便单测。
     */
    GeoContext resolveContext(HttpServletRequest request) {
        String ip = extractClientIp(request);

        // 1. X-Country 显式声明（最优先 — 反向代理/CDN 写入）
        String headerCountry = request.getHeader("X-Country");
        if (headerCountry != null && !headerCountry.isBlank()) {
            return buildContext(headerCountry.trim(), ip);
        }

        // 2. Cloudflare
        String cfCountry = request.getHeader("CF-IPCountry");
        if (cfCountry != null && !cfCountry.isBlank() && !"XX".equalsIgnoreCase(cfCountry)) {
            return buildContext(cfCountry.trim(), ip);
        }

        // 3. 本机 / 私有 IP → 视为 CN（开发环境）
        if (LOCALHOST_V4.matcher(ip).matches() || isPrivateIp(ip)) {
            return buildContext(CountryCode.CN.getCode(), ip);
        }

        // 4. 兜底 UNKNOWN
        log.debug("Geo resolve unknown: ip={} (production should set X-Country header)", ip);
        return GeoContext.unknown(ip);
    }

    private GeoContext buildContext(String code, String ip) {
        CountryCode cc = CountryCode.fromCode(code);
        return new GeoContext(cc.getCode(), cc.getName(), cc.getTimezone(),
                cc.getCurrency(), cc.getLocale(), ip);
    }

    /**
     * 解析 X-Forwarded-For 首段（真实客户端 IP）— 反向代理标准写法。
     */
    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }

    /** 私有 IP 段（10.x / 172.16-31.x / 192.168.x）— 视为内网。 */
    private static boolean isPrivateIp(String ip) {
        if (ip == null) return false;
        if (ip.startsWith("10.")) return true;
        if (ip.startsWith("192.168.")) return true;
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length >= 2) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {}
            }
        }
        return false;
    }
}
