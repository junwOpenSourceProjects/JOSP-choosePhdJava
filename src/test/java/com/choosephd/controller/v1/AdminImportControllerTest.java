package com.choosephd.controller.v1;

import com.choosephd.importer.DataImportService;
import com.choosephd.importer.ImportProgress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminImportController 单元测试 — standaloneSetup() 模式.
 *
 * <p>2 端点:
 * <ol>
 *   <li>POST /run - 触发后台异步导入 (新线程跑 service.runImport)</li>
 *   <li>GET /status - 查当前进度</li>
 * </ol>
 *
 * <p>注: 异步线程无法在单测里直接 await, 用 verify(timeout) 验证 service.runImport 被调.
 * (memory 立 "mock void 方法用 doNothing() / 异步用 verify(...timeout)" 模式)
 */
class AdminImportControllerTest {

    private DataImportService dataImportService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        dataImportService = mock(DataImportService.class);
        AdminImportController controller = new AdminImportController(dataImportService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ===== POST /run =====
    @Test
    void runImport_returnsStarted() throws Exception {
        doNothing().when(dataImportService).runImport();

        mockMvc.perform(post("/api/v1/admin/import/run"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.status").value("started"));

        // 异步线程: 给 1s 让它跑完
        verify(dataImportService, timeout(1000)).runImport();
    }

    // ===== GET /status =====
    @Test
    void status_default_returnsProgress() throws Exception {
        ImportProgress progress = new ImportProgress();
        // 默认 status=IDLE, 不需要额外 set
        when(dataImportService.getProgress()).thenReturn(progress);

        mockMvc.perform(get("/api/v1/admin/import/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        verify(dataImportService).getProgress();
    }

    @Test
    void status_running_returnsCurrentState() throws Exception {
        ImportProgress running = new ImportProgress();
        running.setStatus("RUNNING");
        running.setCurrentFile("ranking_data/qs/2024.csv");
        running.setMessage("importing qs_world");
        when(dataImportService.getProgress()).thenReturn(running);

        mockMvc.perform(get("/api/v1/admin/import/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        verify(dataImportService).getProgress();
    }
}
