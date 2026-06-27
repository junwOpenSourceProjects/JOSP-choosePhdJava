package com.choosephd.security;

/**
 * Filter / Interceptor 顺序常量集中定义，避免 magic number 散落各处。
 *
 * <p>执行顺序：{@link #ANTI_SCRAPE} (200) → Spring Security 默认链 (300)
 * → {@link com.choosephd.security.AuthInterceptor} (WebConfig 注册 500)。
 */
public final class Ordered {

    /** 反爬虫 UA 黑名单 filter — 应早于鉴权，避免被拦截请求仍消耗鉴权算力。 */
    public static final int ANTI_SCRAPE = 200;

    /** Spring Security 默认过滤器链。 */
    public static final int SPRING_SECURITY = 300;

    /** AuthInterceptor 在 WebConfig 中通过 addPathPatterns 注册，order 500。 */
    public static final int AUTH_INTERCEPTOR = 500;

    private Ordered() {}
}
