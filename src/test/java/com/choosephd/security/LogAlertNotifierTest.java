package com.choosephd.security;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LogAlertNotifier 单元测试 — 不依赖 SLF4J appender，只验证 channelName 和 send 不抛异常。
 */
class LogAlertNotifierTest {

    private final LogAlertNotifier notifier = new LogAlertNotifier();

    @Test
    void channelName_isLog() {
        assertEquals("log", notifier.channelName());
    }

    @Test
    void send_withFullExtra_doesNotThrow() {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("ip", "203.0.113.7");
        extra.put("count", 87);
        extra.put("window", "24h");
        extra.put("threshold", 50);

        // 仅验证不抛异常 — 实际 log 走 SLF4J NoOp appender (test scope)
        assertDoesNotThrow(() -> notifier.send("🚨 ALERT scrape risky IP",
                "IP 203.0.113.7 hit 87 blocks in last 24h (threshold=50)", extra));
    }

    @Test
    void send_withNullExtra_doesNotThrow() {
        assertDoesNotThrow(() -> notifier.send("title", "message", null));
    }

    @Test
    void send_withBlankMessage_doesNotThrow() {
        assertDoesNotThrow(() -> notifier.send("title", "", Map.of("ip", "x")));
    }
}
