package com.choosephd.security;

import com.choosephd.entity.ScrapeAudit;
import com.choosephd.repository.ScrapeAuditMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 反爬虫告警 scheduler — 定期扫描 scrape_audit 表，单 IP 24h 拦截超阈值触发告警。
 *
 * <p>触发频率：每 10 分钟（{@code fixedRate = 600000}）。
 * <p>告警阈值：默认 50 次/24h（{@code scrape.alert.threshold-per-24h}）。
 * <p>告警渠道：当前仅打 {@code WARN} 日志，留口子接 webhook / 邮件 / 钉钉机器人。
 *
 * <p>反爬虫告警不应误伤内网（127.0.0.1/192.168.x），因此 dev 环境阈值可调高或
 * 完全关闭（{@code scrape.alert.enabled = false}）。
 */
@Component
public class ScrapeAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(ScrapeAlertScheduler.class);

    private final ScrapeAuditMapper scrapeAuditMapper;
    private final boolean enabled;
    private final int thresholdPer24h;
    private final int windowHours;
    private final int sampleLimit;

    public ScrapeAlertScheduler(ScrapeAuditMapper scrapeAuditMapper,
                                @Value("${scrape.alert.enabled:true}") boolean enabled,
                                @Value("${scrape.alert.threshold-per-24h:50}") int thresholdPer24h,
                                @Value("${scrape.alert.window-hours:24}") int windowHours,
                                @Value("${scrape.alert.sample-limit:500}") int sampleLimit) {
        this.scrapeAuditMapper = scrapeAuditMapper;
        this.enabled = enabled;
        this.thresholdPer24h = thresholdPer24h;
        this.windowHours = windowHours;
        this.sampleLimit = sampleLimit;
    }

    /**
     * 主调度任务 — 扫描高危 IP + 输出告警。
     * 注：{@code fixedRate} 而非 {@code cron}，避免 timezone 漂移。
     */
    @Scheduled(fixedRate = 600_000L, initialDelay = 60_000L)
    public void scanRiskyIps() {
        if (!enabled) {
            log.debug("ScrapeAlertScheduler disabled by scrape.alert.enabled=false");
            return;
        }
        try {
            doScan();
        } catch (Exception e) {
            log.error("ScrapeAlertScheduler scan failed", e);
        }
    }

    void doScan() {
        LocalDateTime since = LocalDateTime.now().minusHours(windowHours);
        List<ScrapeAudit> recent = scrapeAuditMapper.listRecent(sampleLimit);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ScrapeAudit a : recent) {
            if (a.getCreatedAt() != null && a.getCreatedAt().isAfter(since)) {
                counts.merge(a.getIp(), 1L, Long::sum);
            }
        }
        int riskyCount = 0;
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            if (e.getValue() >= thresholdPer24h) {
                riskyCount++;
                // 真实部署应接 webhook / 邮件 / 钉钉机器人；当前仅日志
                log.warn("ALERT scrape: ip={} count={} window={}h threshold={} (action: consider block)",
                        e.getKey(), e.getValue(), windowHours, thresholdPer24h);
            }
        }
        if (riskyCount > 0) {
            log.warn("ScrapeAlertScheduler: {} risky IPs in last {}h (threshold={})",
                    riskyCount, windowHours, thresholdPer24h);
        } else {
            log.debug("ScrapeAlertScheduler: clean, {} unique IPs in last {}h",
                    counts.size(), windowHours);
        }
    }

    // 暴露为包内可见，方便单测
    boolean isEnabled() { return enabled; }
    int getThreshold() { return thresholdPer24h; }
}