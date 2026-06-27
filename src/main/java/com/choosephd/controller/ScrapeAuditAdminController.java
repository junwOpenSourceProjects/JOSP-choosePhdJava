package com.choosephd.controller;

import com.choosephd.common.ApiResult;
import com.choosephd.entity.ScrapeAudit;
import com.choosephd.repository.ScrapeAuditMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 反爬虫审计查询端点 — admin 用。
 *
 * <p>提供 3 类查询：
 * <ul>
 *   <li>{@code GET /api/v1/admin/scrape-audit/recent?limit=50} — 最近 N 条拦截记录</li>
 *   <li>{@code GET /api/v1/admin/scrape-audit/ip-count?ip=...&hours=24} — 单 IP 时间窗口计数</li>
 *   <li>{@code GET /api/v1/admin/scrape-audit/risky-ips?hours=24&threshold=50} — 高危 IP 列表</li>
 * </ul>
 *
 * <p>当前未加鉴权拦截（与 {@code /admin/import/status} 一致），后续应接入 admin role JWT。
 */
@RestController
@RequestMapping("/api/v1/admin/scrape-audit")
public class ScrapeAuditAdminController {

    private final ScrapeAuditMapper scrapeAuditMapper;

    public ScrapeAuditAdminController(ScrapeAuditMapper scrapeAuditMapper) {
        this.scrapeAuditMapper = scrapeAuditMapper;
    }

    @GetMapping("/recent")
    public ApiResult<List<ScrapeAudit>> recent(@RequestParam(defaultValue = "50") int limit) {
        if (limit < 1) limit = 1;
        if (limit > 500) limit = 500;
        return ApiResult.ok(scrapeAuditMapper.listRecent(limit));
    }

    @GetMapping("/ip-count")
    public ApiResult<Map<String, Object>> ipCount(@RequestParam String ip,
                                                  @RequestParam(defaultValue = "24") int hours) {
        if (hours < 1) hours = 1;
        if (hours > 168) hours = 168; // 最多 7 天
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        long count = scrapeAuditMapper.countByIpSince(ip, since);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ip", ip);
        data.put("hours", hours);
        data.put("count", count);
        data.put("since", since.toString());
        return ApiResult.ok(data);
    }

    /**
     * 高危 IP 列表 — 暂用 listRecent + 内存聚合，后续可加 SQL GROUP BY。
     * 当前数据量 < 千条级别，内存聚合足够。
     */
    @GetMapping("/risky-ips")
    public ApiResult<Map<String, Object>> riskyIps(@RequestParam(defaultValue = "24") int hours,
                                                   @RequestParam(defaultValue = "50") int threshold) {
        int effectiveHours = Math.max(1, Math.min(168, hours));
        int effectiveThreshold = Math.max(1, threshold);
        LocalDateTime since = LocalDateTime.now().minusHours(effectiveHours);

        List<ScrapeAudit> recent = scrapeAuditMapper.listRecent(500);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ScrapeAudit a : recent) {
            if (a.getCreatedAt() != null && a.getCreatedAt().isAfter(since)) {
                counts.merge(a.getIp(), 1L, Long::sum);
            }
        }
        final int th = effectiveThreshold;
        long riskyCount = counts.values().stream().filter(c -> c >= th).count();
        java.util.List<Map<String, Object>> top = counts.entrySet().stream()
                .filter(e -> e.getValue() >= th)
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(20)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("ip", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hours", effectiveHours);
        data.put("threshold", effectiveThreshold);
        data.put("total_ips", counts.size());
        data.put("risky_ip_count", riskyCount);
        data.put("top", top);
        return ApiResult.ok(data);
    }
}