package com.choosephd.controller;

import com.choosephd.common.ApiResult;
import com.choosephd.entity.ScrapeAudit;
import com.choosephd.repository.ScrapeAuditMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 反爬虫审计查询端点 — admin 用。
 *
 * <p>提供 4 类查询：
 * <ul>
 *   <li>{@code GET /api/v1/admin/scrape-audit/recent?limit=50} — 最近 N 条拦截记录</li>
 *   <li>{@code GET /api/v1/admin/scrape-audit/ip-count?ip=...&hours=24} — 单 IP 时间窗口计数</li>
 *   <li>{@code GET /api/v1/admin/scrape-audit/risky-ips?hours=24&threshold=50} — 高危 IP 列表</li>
 *   <li>{@code GET /api/v1/admin/scrape-audit/export?hours=24} — CSV 流式导出（admin 下载）</li>
 * </ul>
 *
 * <p>AdminInterceptor 兜底（项目 commit 54ffcfd），无 admin 权限返 403。
 */
@RestController
@RequestMapping("/api/v1/admin/scrape-audit")
public class ScrapeAuditAdminController {

    /** CSV 导出上限 — 防全表拖死内存，admin 真要全量走 archive 工具。 */
    private static final int CSV_EXPORT_LIMIT = 10_000;

    /** CSV 时间戳后缀 — 例 {@code 20260628_143022}。 */
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

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

    /**
     * CSV 流式导出 — admin 下载审计记录用于离线分析 / 归档。
     *
     * <p>默认导出最近 24h 全部拦截记录（上限 {@link #CSV_EXPORT_LIMIT} 条），
     * 通过 {@code ?hours=N} 指定时间窗口（1-168 / 7d），{@code ?hours=0} 表示导出全部历史（仍受 {@link #CSV_EXPORT_LIMIT} 限制）。
     *
     * <p>返回 RFC 7231 兼容 {@code text/csv; charset=UTF-8}，文件名带时间戳：
     * {@code scrape-audit_<hours>h_20260628_143022.csv}。
     *
     * <p>CSV 字段顺序：{@code id, created_at, ip, method, path, status_code, reject_reason, user_agent}，
     * 含 BOM 头兼容 Excel 中文打开乱码问题。
     *
     * @param hours 时间窗口（小时），{@code 0} = 全部历史，{@code null} = 默认 24h
     * @return  CSV 字节流 ResponseEntity
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(defaultValue = "24") int hours) {
        int effectiveHours = Math.max(0, Math.min(168, hours));
        List<ScrapeAudit> rows = (effectiveHours == 0)
                ? scrapeAuditMapper.listRecent(CSV_EXPORT_LIMIT)
                : scrapeAuditMapper.listSince(LocalDateTime.now().minusHours(effectiveHours), CSV_EXPORT_LIMIT);

        StringBuilder sb = new StringBuilder(4096);
        // UTF-8 BOM — Excel 打开含中文的 CSV 不乱码
        sb.append('\uFEFF');
        // 表头
        sb.append("id,created_at,ip,method,path,status_code,reject_reason,user_agent\n");
        // 数据行 — 用 RFC 4180 转义（双引号包裹 + 内部双引号转义 + 含逗号/换行/双引号时强制引号）
        for (ScrapeAudit r : rows) {
            sb.append(csvField(r.getId())).append(',')
              .append(csvField(r.getCreatedAt() == null ? "" : r.getCreatedAt().toString())).append(',')
              .append(csvField(r.getIp())).append(',')
              .append(csvField(r.getMethod())).append(',')
              .append(csvField(r.getPath())).append(',')
              .append(csvField(r.getStatusCode())).append(',')
              .append(csvField(r.getRejectReason())).append(',')
              .append(csvField(r.getUserAgent())).append('\n');
        }

        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "scrape-audit_" + (effectiveHours == 0 ? "all" : effectiveHours + "h") + "_"
                + LocalDateTime.now().format(FILE_TS) + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        // filename* RFC 5987 — 兼容中文文件名 + 现代浏览器
        headers.set("Content-Disposition",
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename);

        return new ResponseEntity<>(body, headers, org.springframework.http.HttpStatus.OK);
    }

    /**
     * RFC 4180 CSV 字段转义：
     * <ul>
     *   <li>null → 空字符串</li>
     *   <li>非字符串（数字等）→ 直接 toString</li>
     *   <li>字符串含 {@code ,} {@code "} {@code \n} {@code \r} → 整字段双引号包裹 + 内部 {@code "} 替换为 {@code ""}</li>
     *   <li>否则原样输出</li>
     * </ul>
     */
    private static String csvField(Object value) {
        if (value == null) return "";
        String s = value.toString();
        if (s.indexOf(',') < 0 && s.indexOf('"') < 0 && s.indexOf('\n') < 0 && s.indexOf('\r') < 0) {
            return s;
        }
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}