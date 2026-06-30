package com.choosephd.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

/**
 * 响应体混淆 Filter — 未登录用户访问敏感 API 时，将 JSON 响应体 XOR + Base64 编码，
 * 使浏览器 DevTools Network 标签中无法直接读取原始排名数据。
 *
 * <p>使用 Spring 官方的 ContentCachingResponseWrapper 确保可靠捕获响应体。
 *
 * <p>放行条件（任一满足即不混淆）：
 * <ul>
 *   <li>请求携带有效 JWT（已登录用户看真实数据）</li>
 *   <li>请求路径不在敏感列表中</li>
 * </ul>
 */
@Component
@Order(Ordered.RESPONSE_OBFUSCATION)
public class ResponseObfuscationFilter extends OncePerRequestFilter {

    private static final String OBFUSCATED_HEADER = "X-Obfuscated";
    private static final byte[] XOR_KEY = "JOSP-choosePhd-2026-net-tab-obfuscation-v1".getBytes(StandardCharsets.UTF_8);

    private static final Set<String> SENSITIVE_PATH_PREFIXES = Set.of(
            "/api/v1/universities",
            "/api/v1/sources",
            "/api/v1/rankings",
            "/api/v1/compare",
            "/api/v1/trends"
    );

    /** AI 爬虫/搜索引擎 UA 白名单 — 放行不混淆，保障 GEO 索引收录。 */
    private static final Set<String> AI_CRAWLER_UAS = Set.of(
            // OpenAI
            "GPTBot", "ChatGPT-User",
            // Anthropic
            "anthropic-ai", "Claude-Web",
            // Google (Gemini / Search)
            "Google-Extended", "Googlebot",
            // Microsoft
            "Bingbot",
            // 百度
            "Baiduspider",
            // DeepSeek
            "DeepSeekBot",
            // 字节跳动 (豆包)
            "Bytespider", "ByteSpider",
            // 阿里 (通义千问)
            "TongyiBot", "Tongyi", "YisouSpider",
            // 其他
            "PerplexityBot", "YandexBot", "DuckDuckBot",
            "Applebot", "Slurp", "Twitterbot", "facebookexternalhit"
    );

    public ResponseObfuscationFilter() {
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        // AI 爬虫 / 搜索引擎 → 放行原样（GEO 收录需要 LLM 看到 Top 10 数据）
        // 除此以外所有人（免费/付费/登录/未登录）一律走混淆
        if (isAiCrawler(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 路径不在敏感列表 → 放行原样
        String path = request.getRequestURI();
        boolean sensitive = SENSITIVE_PATH_PREFIXES.stream().anyMatch(path::startsWith);
        if (!sensitive) {
            filterChain.doFilter(request, response);
            return;
        }

        // Spring 官方的 ContentCachingResponseWrapper — 可靠捕获响应体
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrapper);

        // 获取捕获的原始 JSON bytes
        byte[] original = wrapper.getContentAsByteArray();
        if (original.length > 0) {
            // XOR 混淆 + Base64 编码
            byte[] encoded = Base64.getEncoder().encode(xorBytes(original));

            // 设置标记 header，前端 useApi 据此解码
            response.setHeader(OBFUSCATED_HEADER, "1");
            response.setContentType("text/plain;charset=UTF-8");
            response.setContentLength(encoded.length);
            response.getOutputStream().write(encoded);
            response.getOutputStream().flush();
        } else {
            // 空响应直接放行
            wrapper.copyBodyToResponse();
        }
    }

    /** 检测 AI 爬虫/搜索引擎 UA — 放行不混淆，保障 GEO 收录。 */
    private boolean isAiCrawler(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua == null) return false;
        String uaLower = ua.toLowerCase();
        for (String bot : AI_CRAWLER_UAS) {
            if (uaLower.contains(bot.toLowerCase())) return true;
        }
        return false;
    }

    private byte[] xorBytes(byte[] input) {
        byte[] output = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = (byte) (input[i] ^ XOR_KEY[i % XOR_KEY.length]);
        }
        return output;
    }
}
