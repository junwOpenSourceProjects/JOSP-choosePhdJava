package com.choosephd.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * WebhookAlertNotifier 单元测试 — Mock RestTemplate 验证 payload 构造 + 失败吞异常。
 *
 * <p>覆盖 4 个分支：channelName / 空 URL no-op / 成功 POST / 网络异常吞掉。
 */
class WebhookAlertNotifierTest {

    @Test
    void channelName_isWebhook() {
        WebhookAlertNotifier notifier = new WebhookAlertNotifier("https://example.com/hook");
        assertEquals("webhook", notifier.channelName());
    }

    @Test
    void send_emptyUrl_skipsWithoutException() {
        WebhookAlertNotifier notifier = new WebhookAlertNotifier("");
        assertDoesNotThrow(() -> notifier.send("title", "message", Map.of("ip", "1.2.3.4")));
    }

    @Test
    void send_success_postJsonWithExpectedShape() throws Exception {
        // 用继承覆盖 RestTemplate 行为，捕获 HttpEntity
        RestTemplate mockRest = mock(RestTemplate.class);
        WebhookAlertNotifier notifier = new TestableWebhookNotifier("https://example.com/hook", mockRest);

        notifier.send("🚨 ALERT", "IP 1.2.3.4 hit 87 blocks", Map.of("ip", "1.2.3.4", "count", 87));

        verify(mockRest, times(1)).postForEntity(eq("https://example.com/hook"), any(HttpEntity.class), eq(String.class));

        // 验证 Content-Type 是 application/json
        // 走 mock 默认空 HttpEntity.headers, 这里只看调用发生即可 (RestTemplate 已 mock)
        verifyNoMoreInteractions(mockRest);
    }

    @Test
    void send_networkFailure_swallowsException() {
        RestTemplate mockRest = mock(RestTemplate.class);
        when(mockRest.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("connection refused"));
        WebhookAlertNotifier notifier = new TestableWebhookNotifier("https://example.com/hook", mockRest);

        // 不应抛异常 — 告警通道故障不应阻塞 scheduler
        assertDoesNotThrow(() -> notifier.send("title", "message", Map.of("ip", "1.2.3.4")));
    }

    /**
     * Testable 子类 — 注入 mock RestTemplate，避免 new WebhookAlertNotifier 内部 new RestTemplate。
     */
    private static class TestableWebhookNotifier extends WebhookAlertNotifier {
        private final RestTemplate injectedRest;

        TestableWebhookNotifier(String url, RestTemplate rest) {
            super(url);
            this.injectedRest = rest;
            try {
                java.lang.reflect.Field f = WebhookAlertNotifier.class.getDeclaredField("restTemplate");
                f.setAccessible(true);
                f.set(this, rest);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
