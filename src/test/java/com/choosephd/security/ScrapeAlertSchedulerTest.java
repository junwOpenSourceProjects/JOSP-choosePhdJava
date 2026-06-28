package com.choosephd.security;

import com.choosephd.entity.ScrapeAudit;
import com.choosephd.repository.ScrapeAuditMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ScrapeAlertScheduler 单元测试 — Mock ScrapeAuditMapper + 2 AlertNotifier，验证：
 * <ul>
 *   <li>disabled 状态 no-op</li>
 *   <li>未超阈值不告警</li>
 *   <li>超阈值精确触发所有 notifier</li>
 *   <li>notifier 抛异常不影响其他 notifier</li>
 *   <li>mapper 抛异常被 try/catch 兜底</li>
 * </ul>
 */
class ScrapeAlertSchedulerTest {

    private ScrapeAuditMapper mapper;
    private AlertNotifier logNotifier;
    private AlertNotifier webhookNotifier;
    private ScrapeAlertScheduler scheduler;

    @BeforeEach
    void setUp() {
        mapper = mock(ScrapeAuditMapper.class);
        logNotifier = mock(AlertNotifier.class);
        when(logNotifier.channelName()).thenReturn("log");
        webhookNotifier = mock(AlertNotifier.class);
        when(webhookNotifier.channelName()).thenReturn("webhook");

        scheduler = new ScrapeAlertScheduler(mapper,
                List.of(logNotifier, webhookNotifier),
                true,   // enabled
                50,     // thresholdPer24h
                24,     // windowHours
                500,    // sampleLimit
                24);    // alertCooldownHours
    }

    @Test
    void scanRiskyIps_disabled_skipsBeforeCallingMapper() {
        // enabled 检查在 scanRiskyIps (public), 不在 doScan (package-private)
        ScrapeAlertScheduler disabled = new ScrapeAlertScheduler(mapper, List.of(logNotifier),
                false, 50, 24, 500, 24);

        disabled.scanRiskyIps();

        verifyNoInteractions(mapper);
        verifyNoInteractions(logNotifier);
    }

    @Test
    void doScan_emptyRecent_noNotifiersCalled() {
        when(mapper.listRecent(anyInt())).thenReturn(List.of());

        scheduler.doScan();

        verify(mapper).listRecent(500);
        verifyNoInteractions(logNotifier);
        verifyNoInteractions(webhookNotifier);
        assertEquals(2, scheduler.getNotifierCount());
    }

    @Test
    void doScan_belowThreshold_noNotifiersCalled() {
        // 24h 内 49 次 (threshold=50), 应不触发
        LocalDateTime now = LocalDateTime.now();
        List<ScrapeAudit> records = new ArrayList<>();
        for (int i = 0; i < 49; i++) {
            ScrapeAudit a = new ScrapeAudit();
            a.setIp("203.0.113.7");
            a.setCreatedAt(now.minusMinutes(i));
            records.add(a);
        }
        when(mapper.listRecent(anyInt())).thenReturn(records);

        scheduler.doScan();

        verifyNoInteractions(logNotifier);
        verifyNoInteractions(webhookNotifier);
    }

    @Test
    void doScan_aboveThreshold_callsAllNotifiers_withExpectedPayload() {
        // 51 次同一 IP, threshold=50 → 触发
        LocalDateTime now = LocalDateTime.now();
        List<ScrapeAudit> records = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            ScrapeAudit a = new ScrapeAudit();
            a.setIp("203.0.113.99");
            a.setCreatedAt(now.minusMinutes(i));
            records.add(a);
        }
        when(mapper.listRecent(anyInt())).thenReturn(records);

        scheduler.doScan();

        ArgumentCaptor<String> titleCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> msgCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> extraCap = ArgumentCaptor.forClass(Map.class);

        verify(logNotifier, times(1)).send(titleCap.capture(), msgCap.capture(), extraCap.capture());
        verify(webhookNotifier, times(1)).send(anyString(), anyString(), any());

        assertEquals("🚨 ALERT scrape risky IP", titleCap.getValue());
        assertTrue(msgCap.getValue().contains("203.0.113.99"));
        assertTrue(msgCap.getValue().contains("51"));
        assertEquals("203.0.113.99", extraCap.getValue().get("ip"));
        assertEquals(51L, extraCap.getValue().get("count"));
        assertEquals("24h", extraCap.getValue().get("window"));
        assertEquals(50, extraCap.getValue().get("threshold"));
        assertEquals("consider block", extraCap.getValue().get("action"));
    }

    @Test
    void doScan_oneNotifierThrows_otherStillCalled() {
        LocalDateTime now = LocalDateTime.now();
        List<ScrapeAudit> records = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            ScrapeAudit a = new ScrapeAudit();
            a.setIp("1.1.1.1");
            a.setCreatedAt(now.minusMinutes(i));
            records.add(a);
        }
        when(mapper.listRecent(anyInt())).thenReturn(records);
        doThrow(new RuntimeException("log appender down")).when(logNotifier)
                .send(anyString(), anyString(), any());

        scheduler.doScan();

        // logNotifier 抛了异常, 但 webhookNotifier 仍被调用 (try/catch 隔离)
        verify(logNotifier).send(anyString(), anyString(), any());
        verify(webhookNotifier).send(anyString(), anyString(), any());
    }

    @Test
    void doScan_mapperThrows_swallowedByScan() {
        when(mapper.listRecent(anyInt())).thenThrow(new RuntimeException("DB down"));

        // scanRiskyIps (public) 兜底 — scan 内部 try/catch 不应抛
        assertDoesNotThrow(() -> scheduler.scanRiskyIps());
        verifyNoInteractions(logNotifier);
    }

    @Test
    void doScan_oldRecordsOutsideWindow_ignored() {
        // createdAt 在 window 外, 即使 listRecent 返了也不计
        LocalDateTime oldTime = LocalDateTime.now().minusDays(30);
        ScrapeAudit a = new ScrapeAudit();
        a.setIp("1.2.3.4");
        a.setCreatedAt(oldTime);
        when(mapper.listRecent(anyInt())).thenReturn(List.of(a));

        scheduler.doScan();

        verifyNoInteractions(logNotifier);
    }

    // ============ 冷却机制新测试 (Round 6) ============

    @Test
    void doScan_aboveThreshold_firstTime_callsNotifiers_andTracksIp() {
        // 51 次同一 IP, 第一次扫描 → 触发告警 + 入 lastAlertByIp
        LocalDateTime now = LocalDateTime.now();
        List<ScrapeAudit> records = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            ScrapeAudit a = new ScrapeAudit();
            a.setIp("198.51.100.42");
            a.setCreatedAt(now.minusMinutes(i));
            records.add(a);
        }
        when(mapper.listRecent(anyInt())).thenReturn(records);

        scheduler.doScan();

        // notifier 被调用
        verify(logNotifier, times(1)).send(anyString(), anyString(), any());
        // in-memory 跟踪 1 个 IP
        assertEquals(1, scheduler.getTrackedIpCount());
    }

    @Test
    void doScan_sameIpTwiceWithinCooldown_secondCallSuppressed() {
        // 第一次扫描触发 + 入冷却, 第二次同一 IP 应跳过 notifier
        LocalDateTime now = LocalDateTime.now();
        List<ScrapeAudit> records = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            ScrapeAudit a = new ScrapeAudit();
            a.setIp("198.51.100.99");
            a.setCreatedAt(now.minusMinutes(i));
            records.add(a);
        }
        when(mapper.listRecent(anyInt())).thenReturn(records);

        scheduler.doScan();  // 第一次: 触发
        scheduler.doScan();  // 第二次: 冷却中, 跳过

        // 只调用 1 次 (第二次被冷却跳过)
        verify(logNotifier, times(1)).send(anyString(), anyString(), any());
        verify(webhookNotifier, times(1)).send(anyString(), anyString(), any());
    }

    @Test
    void doScan_differentIpsBothAboveThreshold_bothAlertOnce() {
        // 2 个 IP 都超阈值, 互不影响, 各发 1 次
        LocalDateTime now = LocalDateTime.now();
        List<ScrapeAudit> records = new ArrayList<>();
        for (int ip = 1; ip <= 2; ip++) {
            for (int i = 0; i < 51; i++) {
                ScrapeAudit a = new ScrapeAudit();
                a.setIp("198.51.100." + ip);
                a.setCreatedAt(now.minusMinutes(i));
                records.add(a);
            }
        }
        when(mapper.listRecent(anyInt())).thenReturn(records);

        scheduler.doScan();

        // 2 个不同 IP 各发 1 次, 总共 2 次
        verify(logNotifier, times(2)).send(anyString(), anyString(), any());
        verify(webhookNotifier, times(2)).send(anyString(), anyString(), any());
        assertEquals(2, scheduler.getTrackedIpCount());
    }
}
