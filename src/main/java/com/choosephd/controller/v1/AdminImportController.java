package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import com.choosephd.importer.DataImportService;
import com.choosephd.importer.ImportProgress;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin 数据导入 controller — 触发 CSV 批量导入 + 查询进度。
 *
 * <p>权限：仅 ROLE_ADMIN（{@link com.choosephd.security.AdminInterceptor} 拦截，
 * request attribute "userRole" 必须 = "ROLE_ADMIN"）。
 *
 * <p>导入流程：POST /run (CSV 文件 + 数据集名) → 后端异步执行
 * {@link com.choosephd.importer.DataImportService#runImport} → 通过
 * {@link com.choosephd.importer.ImportProgress} 返回当前进度 → 前端轮询进度条。
 *
 * <p>Service：{@link com.choosephd.importer.DataImportService}。
 */
@RestController
@RequestMapping("/api/v1/admin/import")
public class AdminImportController {

    private final DataImportService dataImportService;

    public AdminImportController(DataImportService dataImportService) {
        this.dataImportService = dataImportService;
    }

    @PostMapping("/run")
    public ApiResult<Map<String, String>> runImport() {
        new Thread(() -> dataImportService.runImport()).start();
        return ApiResult.ok(Map.of("status", "started"));
    }

    @GetMapping("/status")
    public ApiResult<ImportProgress> status() {
        return ApiResult.ok(dataImportService.getProgress());
    }
}
