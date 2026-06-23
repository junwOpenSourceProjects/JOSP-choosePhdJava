package josp.choosphd.controller;

import josp.choosphd.common.ApiResult;
import josp.choosphd.domain.auth.ImportJobPO;
import josp.choosphd.importer.DataImportService;
import josp.choosphd.importer.RawDataScanner;
import josp.choosphd.mapper.ImportJobMapper;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/import")
public class AdminImportController {

    private final RawDataScanner scanner;
    private final DataImportService importer;
    private final ImportJobMapper jobMapper;

    public AdminImportController(RawDataScanner scanner, DataImportService importer, ImportJobMapper jobMapper) {
        this.scanner = scanner;
        this.importer = importer;
        this.jobMapper = jobMapper;
    }

    @PostMapping("/run")
    public ApiResult<Map<String, Object>> run() {
        List<RawDataScanner.RawFile> files = scanner.scan();
        int ok = 0, fail = 0;
        for (RawDataScanner.RawFile rf : files) {
            ImportJobPO job = importer.importOne(rf);
            if (job == null) continue;
            if ("SUCCESS".equals(job.getStatus())) ok++; else fail++;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", files.size());
        out.put("success", ok);
        out.put("failed", fail);
        return ApiResult.ok(out);
    }

    @GetMapping("/jobs")
    public ApiResult<List<ImportJobPO>> jobs() {
        return ApiResult.ok(jobMapper.selectAll());
    }
}
