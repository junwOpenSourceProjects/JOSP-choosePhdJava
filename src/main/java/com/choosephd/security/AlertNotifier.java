package com.choosephd.security;

import java.util.Map;

/**
 * 告警通知通道接口 — {@link ScrapeAlertScheduler} 命中阈值时调用。
 *
 * <p>当前实现：
 * <ul>
 *   <li>{@link LogAlertNotifier} — 写 WARN 日志（默认启用，无外部依赖）</li>
 *   <li>{@link WebhookAlertNotifier} — POST JSON 到任意 HTTP URL（钉钉 / 飞书 / Slack / 自建系统通用）</li>
 * </ul>
 *
 * <p>启用方式：{@code application.yml} 配置 {@code scrape.alert.channels=log,webhook}，
 * 同时设置 {@code scrape.alert.webhook-url}。
 *
 * <p>新通道只需另写一个实现类加 {@code @Component}（按 channel name 命名 Bean）即可。
 */
public interface AlertNotifier {

    /** 返回此实现对应的 channel 名称，与 yml 配置项匹配。 */
    String channelName();

    /**
     * 发送告警 — 失败不抛异常（告警通道故障不应阻塞 scheduler 主循环）。
     *
     * @param title 告警标题（短）
     * @param message 告警正文（长）
     * @param extra 额外上下文（如 IP / count / threshold）— 序列化为 JSON 字段
     */
    void send(String title, String message, Map<String, Object> extra);
}