package com.choosephd.security;

/**
 * Filter / Interceptor 顺序常量集中定义，避免 magic number 散落各处。
 *
 * <p>执行顺序：
 * <pre>
 *   ANTI_SCRAPE (200) → RATE_LIMIT (250) → SPRING_SECURITY (300)
 *                   → AUTH_INTERCEPTOR (500)
 * </pre>
 */
public final class Ordered {

    /** 反爬虫 UA 黑名单 filter — 应早于鉴权，避免被拦截请求仍消耗鉴权算力。 */
    public static final int ANTI_SCRAPE = 200;

    /** Spring Security 默认过滤器链。 */
    public static final int SPRING_SECURITY = 300;

    /** AuthInterceptor 在 WebConfig 中通过 addPathPatterns 注册，order 500。 */
    public static final int AUTH_INTERCEPTOR = 500;

    /** L3 频率限制 filter — 紧跟 AntiScrapeFilter，已通过 UA 黑名单的合法请求才进入限流。 */
    public static final int RATE_LIMIT = 250;

    private Ordered() {}
}
