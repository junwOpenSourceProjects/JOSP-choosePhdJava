package josp.choosphd.controller;

import josp.choosphd.api.university.dto.UniversityDetailDTO;
import josp.choosphd.api.university.dto.UniversityListDTO;
import josp.choosphd.api.university.dto.UniversityListQuery;
import josp.choosphd.common.ApiResult;
import josp.choosphd.common.PageRequest;
import josp.choosphd.service.UniversityService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/universities")
public class UniversityController {

    private final UniversityService service;

    public UniversityController(UniversityService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResult<Map<String, Object>> list(
            UniversityListQuery query,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {

        PageRequest pr = new PageRequest(
                Math.max(1, pageNo),
                Math.min(100, Math.max(1, pageSize)),
                query.getSortBy() != null ? query.getSortBy() : "score",
                query.getSortDir() != null ? query.getSortDir() : "desc");
        return ApiResult.ok(service.list(query, pr));
    }

    @GetMapping("/{urlId}")
    public ApiResult<UniversityDetailDTO> detail(@PathVariable String urlId) {
        UniversityDetailDTO d = service.detail(urlId);
        if (d == null) return ApiResult.error(404, "university not found: " + urlId);
        return ApiResult.ok(d);
    }
}
