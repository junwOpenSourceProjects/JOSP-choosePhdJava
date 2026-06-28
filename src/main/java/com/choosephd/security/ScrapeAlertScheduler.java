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
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反爬虫告警 scheduler — 定期扫描 scrape_audit 表，单 IP 24h 拦截超阈值触发告警。
 *
 * <p>触发频率：每 10 分钟（{@code fixedRate = 600000}）。
 * <p>告警阈值：默认 50 次/24h（{@code scrape.alert.threshold-per-24h}）。
 * <p>告警冷却：默认 24h 同 IP 不重复发（{@code scrape.alert.cooldown-hours}），避免 10 分钟 spam log 与外部告警通道滥用。
 * <p>告警渠道：可配置 {@code scrape.alert.channels=log,webhook}（{@link AlertNotifier} 实现 Bean）。
 *
 * <p>反爬虫告警不应误伤内网（127.0.0.1/192.168.x），因此 dev 环境阈值可调高或
 * 完全关闭（{@code scrape.alert.enabled = false}）。
 */
@Component
public class ScrapeAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(ScrapeAlertScheduler.class);

    private final ScrapeAuditMapper scrapeAuditMapper;
    private final List<AlertNotifier> notifiers;
    private final boolean enabled;
    private final int thresholdPer24h;
    private final int windowHours;
    private final int sampleLimit;
    private final int alertCooldownHours;

    /** IP → 上次告警时间 (in-memory), 跨重启重置, 但避 10 分钟重复 spam. */
    private final Map<String, LocalDateTime> lastAlertByIp = new ConcurrentHashMap<>();

    public ScrapeAlertScheduler(ScrapeAuditMapper scrapeAuditMapper,
                                List<AlertNotifier> notifiers,
                                @Value("${scrape.alert.enabled:true}") boolean enabled,
                                @Value("${scrape.alert.threshold-per-24h:50}") int thresholdPer24h,
                                @Value("${scrape.alert.window-hours:24}") int windowHours,
                                @Value("${scrape.alert.sample-limit:500}") int sampleLimit,
                                @Value("${scrape.alert.cooldown-hours:24}") int alertCooldownHours) {
        this.scrapeAuditMapper = scrapeAuditMapper;
        this.notifiers = notifiers == null ? List.of() : notifiers;
        this.enabled = enabled;
        this.thresholdPer24h = thresholdPer24h;
        this.windowHours = windowHours;
        this.sampleLimit = sampleLimit;
        this.alertCooldownHours = alertCooldownHours;
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
        int suppressedCount = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            if (e.getValue() >= thresholdPer24h) {
                riskyCount++;
                String ip = e.getKey();
                LocalDateTime lastAlert = lastAlertByIp.get(ip);
                if (lastAlert != null && lastAlert.isAfter(now.minusHours(alertCooldownHours))) {
                    // 冷却期内, 跳过重复告警, 不调用 notifier
                    suppressedCount++;
                    log.debug("ScrapeAlertScheduler: IP {} in cooldown until {}, skip alert",
                            ip, lastAlert.plusHours(alertCooldownHours));
                    continue;
                }
                notifyRisk(ip, e.getValue());
                lastAlertByIp.put(ip, now);
            }
        }
        if (riskyCount > 0) {
            log.warn("ScrapeAlertScheduler: {} risky IPs in last {}h (threshold={}), {} suppressed by cooldown",
                    riskyCount, windowHours, thresholdPer24h, suppressedCount);
        } else {
            log.debug("ScrapeAlertScheduler: clean, {} unique IPs in last {}h", counts.size(), windowHours);
        }
    }

    private void notifyRisk(String ip, long count) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("ip", ip);
        extra.put("count", count);
        extra.put("window", windowHours + "h");
        extra.put("threshold", thresholdPer24h);
        extra.put("action", "consider block");
        String title = "🚨 ALERT scrape risky IP";
        String message = String.format("IP %s hit %d blocks in last %dh (threshold=%d)",
                ip, count, windowHours, thresholdPer24h);
        for (AlertNotifier n : notifiers) {
            try {
                n.send(title, message, extra);
            } catch (Exception e) {
                log.warn("AlertNotifier {} failed: {}", n.channelName(), e.getMessage());
            }
        }
    }

    // 暴露为包内可见，方便单测
    boolean isEnabled() { return enabled; }
    int getThreshold() { return thresholdPer24h; }
    int getNotifierCount() { return notifiers.size(); }
    /** 单测 + 端到端 verify 用, 看 in-memory 冷却状态. */
    int getTrackedIpCount() { return lastAlertByIp.size(); }
}