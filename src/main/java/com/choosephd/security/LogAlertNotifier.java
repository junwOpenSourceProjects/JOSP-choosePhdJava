package com.choosephd.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 日志告警实现 — 默认启用，无外部依赖。
 *
 * <p>命中阈值时打 {@code WARN} 日志，包含 IP / count / window / threshold / action。
 * 任何 log appender（file / syslog / ELK）都可捕获。
 */
@Component
public class LogAlertNotifier implements AlertNotifier {

    private static final Logger log = LoggerFactory.getLogger(LogAlertNotifier.class);

    @Override
    public String channelName() {
        return "log";
    }

    @Override
    public void send(String title, String message, Map<String, Object> extra) {
        StringBuilder sb = new StringBuilder();
        sb.append(title);
        if (extra != null && !extra.isEmpty()) {
            sb.append(" ");
            extra.forEach((k, v) -> sb.append(k).append('=').append(v).append(' '));
        }
        if (message != null && !message.isBlank()) {
            sb.append("— ").append(message);
        }
        log.warn(sb.toString().trim());
    }
}