package com.choosephd.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用 Webhook 告警实现 — POST JSON 到任意 HTTP URL。
 *
 * <p>启用条件：{@code scrape.alert.channels} 含 {@code webhook}（默认逗号分隔）。
 * 需同时设置 {@code scrape.alert.webhook-url}，否则此实现 no-op（仅日志告警）。
 *
 * <p>Payload 格式（兼容钉钉/飞书/Slack/自建系统）：
 * <pre>
 * {
 *   "title": "...",
 *   "message": "...",
 *   "extra": {...},
 *   "timestamp": "2026-06-27T22:50:00",
 *   "source": "choosephd-scraper"
 * }
 * </pre>
 *
 * <p>钉钉机器人需额外 sign 签名（{@code scrape.alert.webhook-secret}），当前实现暂
 * 不内置 — 用户自行在 webhook URL 拼接或中间层加签名。
 */
@Component
@ConditionalOnProperty(name = "scrape.alert.channels", havingValue = "webhook", matchIfMissing = false)
public class WebhookAlertNotifier implements AlertNotifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookAlertNotifier.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String webhookUrl;

    public WebhookAlertNotifier(@Value("${scrape.alert.webhook-url:}") String webhookUrl) {
        this.restTemplate = new RestTemplate();
        this.restTemplate.getMessageConverters().forEach(c -> {});  // 默认 Jackson 已够
        this.objectMapper = new ObjectMapper();
        this.webhookUrl = webhookUrl;
    }

    @Override
    public String channelName() {
        return "webhook";
    }

    @Override
    public void send(String title, String message, Map<String, Object> extra) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("WebhookAlertNotifier enabled but scrape.alert.webhook-url is empty; skipping");
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("title", title);
            payload.put("message", message);
            payload.put("extra", extra);
            payload.put("timestamp", java.time.LocalDateTime.now().toString());
            payload.put("source", "choosephd-scraper");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = objectMapper.writeValueAsString(payload);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(webhookUrl, entity, String.class);
        } catch (RestClientException | com.fasterxml.jackson.core.JsonProcessingException e) {
            // 告警通道故障不应阻塞 scheduler 主循环 — 仅 log 记录
            log.error("Webhook alert POST failed to {}: {}", webhookUrl, e.getMessage());
        }
    }
}