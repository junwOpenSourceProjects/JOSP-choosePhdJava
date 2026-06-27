package com.choosephd.controller;

import com.choosephd.common.ApiResult;
import com.choosephd.geo.GeoContext;
import com.choosephd.geo.GeoContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 调试端点 — 返回当前请求的 GeoContext，供前端联调 Geo 适配。
 *
 * <p>访问 {@code GET /api/v1/geo/me} 即拿到当前客户端被识别出的国家 + 时区 + 货币 + locale。
 * 部署到 nginx 后，前端可通过 {@code X-Country} header 模拟不同国家访问：
 * <pre>
 *   curl -H 'X-Country: US' http://localhost:8081/api/v1/geo/me
 *   curl -H 'X-Country: GB' http://localhost:8081/api/v1/geo/me
 *   curl -H 'X-Country: JP' http://localhost:8081/api/v1/geo/me
 * </pre>
 *
 * <p>仅在 dev 环境暴露；生产可由 {@code application-prod.yml} 关闭或移除 controller。
 */
@RestController
@RequestMapping("/api/v1/geo")
public class GeoDebugController {

    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me() {
        GeoContext ctx = GeoContextHolder.get();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("countryCode", ctx.getCountryCode());
        data.put("countryName", ctx.getCountryName());
        data.put("timezone", ctx.getTimezone());
        data.put("currency", ctx.getCurrency());
        data.put("locale", ctx.getLocale());
        data.put("ip", ctx.getIp());
        data.put("unknown", ctx.isUnknown());
        return ApiResult.ok(data);
    }
}
