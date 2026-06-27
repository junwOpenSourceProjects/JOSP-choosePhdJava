package com.choosephd.geo;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 静态访问点 — controller / service 从当前请求拿到 GeoContext。
 *
 * <p>底层走 Spring {@link RequestContextHolder}（per-request 线程局部），
 * 不必显式清理。
 */
public final class GeoContextHolder {

    /** request attribute key — {@code GeoFilter} 写入的 key。 */
    public static final String ATTR_GEO_CONTEXT = "geoContext";

    private GeoContextHolder() {}

    /**
     * 拿到当前请求的 GeoContext；若无（内部线程调用）返 {@code unknown("0.0.0.0")}。
     */
    public static GeoContext get() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return GeoContext.unknown("0.0.0.0");
        }
        Object ctx = attrs.getRequest().getAttribute(ATTR_GEO_CONTEXT);
        if (ctx instanceof GeoContext) {
            return (GeoContext) ctx;
        }
        return GeoContext.unknown(attrs.getRequest().getRemoteAddr());
    }

    /**
     * 写入 GeoContext — 仅 {@code GeoFilter} 调用。
     */
    public static void set(GeoContext ctx) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            attrs.getRequest().setAttribute(ATTR_GEO_CONTEXT, ctx);
        }
    }

    /**
     * 拿到原始 HttpServletRequest — 内部使用，调用方应在 filter 内。
     */
    public static RequestAttributes currentAttributes() {
        return RequestContextHolder.getRequestAttributes();
    }
}
