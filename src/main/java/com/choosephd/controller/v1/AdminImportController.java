package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import com.choosephd.importer.DataImportService;
import com.choosephd.importer.ImportProgress;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
