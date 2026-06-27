package com.choosephd.controller;

import com.choosephd.entity.ScrapeAudit;
import com.choosephd.repository.ScrapeAuditMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScrapeAuditAdminController 单元测试 — 用 standaloneSetup() 跳过 Spring context 启动。
 *
 * <p>原因：{@code @WebMvcTest} 默认 load ChoosePhdApplication,触发完整 context,
 * WebConfig → AuthInterceptor → JwtService 链路上 JwtService 无 Bean 时整 context 挂。
 * standaloneSetup 仅 mock controller 依赖的 mapper,毫秒级启动。
 *
 * <p>注：401/403 由 WebConfig 注册的 AdminInterceptor 处理（已在 AdminInterceptorTest 单独覆盖）,
 * 此处只验证 controller 逻辑 + 参数 clamp + 数据返回。
 */
class ScrapeAuditAdminControllerTest {

    private ScrapeAuditMapper scrapeAuditMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        scrapeAuditMapper = mock(ScrapeAuditMapper.class);
        ScrapeAuditAdminController controller = new ScrapeAuditAdminController(scrapeAuditMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void recent_defaultLimit_returnsList() throws Exception {
        ScrapeAudit a = newScrapeAudit("1.2.3.4", LocalDateTime.now());
        when(scrapeAuditMapper.listRecent(anyInt())).thenReturn(List.of(a));

        mockMvc.perform(get("/api/v1/admin/scrape-audit/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].ip").value("1.2.3.4"));
    }

    @Test
    void recent_limitClampedToMax500() throws Exception {
        when(scrapeAuditMapper.listRecent(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/scrape-audit/recent").param("limit", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(scrapeAuditMapper).listRecent(500);
    }

    @Test
    void recent_limitClampedToMin1() throws Exception {
        when(scrapeAuditMapper.listRecent(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/scrape-audit/recent").param("limit", "0"))
                .andExpect(status().isOk());

        verify(scrapeAuditMapper).listRecent(1);
    }

    @Test
    void ipCount_validParams_returnsCount() throws Exception {
        when(scrapeAuditMapper.countByIpSince(anyString(), any())).thenReturn(42L);

        mockMvc.perform(get("/api/v1/admin/scrape-audit/ip-count")
                        .param("ip", "203.0.113.7")
                        .param("hours", "24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.ip").value("203.0.113.7"))
                .andExpect(jsonPath("$.data.hours").value(24))
                .andExpect(jsonPath("$.data.count").value(42))
                .andExpect(jsonPath("$.data.since").exists());
    }

    @Test
    void ipCount_hoursClampedToMax168() throws Exception {
        when(scrapeAuditMapper.countByIpSince(anyString(), any())).thenReturn(0L);

        mockMvc.perform(get("/api/v1/admin/scrape-audit/ip-count")
                        .param("ip", "1.2.3.4")
                        .param("hours", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hours").value(168));
    }

    @Test
    void riskyIps_emptyRecent_returnsZeroRisk() throws Exception {
        when(scrapeAuditMapper.listRecent(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/scrape-audit/risky-ips")
                        .param("hours", "24")
                        .param("threshold", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.hours").value(24))
                .andExpect(jsonPath("$.data.threshold").value(50))
                .andExpect(jsonPath("$.data.total_ips").value(0))
                .andExpect(jsonPath("$.data.risky_ip_count").value(0))
                .andExpect(jsonPath("$.data.top.length()").value(0));
    }

    @Test
    void riskyIps_aboveThreshold_returnsSortedTop() throws Exception {
        // 3.3.3.3 出现 60 次 (最高), 1.1.1.1 出现 55 次, 2.2.2.2 出现 51 次
        // threshold=50 全部触发, 排序应 3.3.3.3 > 1.1.1.1 > 2.2.2.2
        LocalDateTime now = LocalDateTime.now();
        List<ScrapeAudit> records = new java.util.ArrayList<>();
        for (int i = 0; i < 60; i++) records.add(newScrapeAudit("3.3.3.3", now.minusMinutes(i)));
        for (int i = 0; i < 55; i++) records.add(newScrapeAudit("1.1.1.1", now.minusMinutes(i + 60)));
        for (int i = 0; i < 51; i++) records.add(newScrapeAudit("2.2.2.2", now.minusMinutes(i + 120)));
        when(scrapeAuditMapper.listRecent(anyInt())).thenReturn(records);

        mockMvc.perform(get("/api/v1/admin/scrape-audit/risky-ips")
                        .param("hours", "24")
                        .param("threshold", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.risky_ip_count").value(3))
                .andExpect(jsonPath("$.data.total_ips").value(3))
                .andExpect(jsonPath("$.data.top[0].ip").value("3.3.3.3"))
                .andExpect(jsonPath("$.data.top[0].count").value(60))
                .andExpect(jsonPath("$.data.top[1].ip").value("1.1.1.1"))
                .andExpect(jsonPath("$.data.top[1].count").value(55))
                .andExpect(jsonPath("$.data.top[2].ip").value("2.2.2.2"))
                .andExpect(jsonPath("$.data.top[2].count").value(51));
    }

    @Test
    void riskyIps_belowThreshold_emptyTop() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        List<ScrapeAudit> records = List.of(
                newScrapeAudit("1.1.1.1", now.minusMinutes(1)),
                newScrapeAudit("1.1.1.1", now.minusMinutes(2))
        );
        when(scrapeAuditMapper.listRecent(anyInt())).thenReturn(records);

        mockMvc.perform(get("/api/v1/admin/scrape-audit/risky-ips")
                        .param("hours", "24")
                        .param("threshold", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.risky_ip_count").value(0))
                .andExpect(jsonPath("$.data.total_ips").value(1))
                .andExpect(jsonPath("$.data.top.length()").value(0));
    }

    @Test
    void riskyIps_oldRecordsOutsideWindow_excluded() throws Exception {
        LocalDateTime old = LocalDateTime.now().minusDays(7);
        ScrapeAudit a = newScrapeAudit("1.1.1.1", old);
        when(scrapeAuditMapper.listRecent(anyInt())).thenReturn(List.of(a));

        mockMvc.perform(get("/api/v1/admin/scrape-audit/risky-ips")
                        .param("hours", "24")
                        .param("threshold", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_ips").value(0))
                .andExpect(jsonPath("$.data.top.length()").value(0));
    }

    private static ScrapeAudit newScrapeAudit(String ip, LocalDateTime createdAt) {
        ScrapeAudit a = new ScrapeAudit();
        a.setIp(ip);
        a.setCreatedAt(createdAt);
        a.setStatusCode(403);
        return a;
    }
}
