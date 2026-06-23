package josp.choosphd.controller;

import josp.choosphd.common.ApiResult;
import josp.choosphd.domain.ranking.RankingViewPO;
import josp.choosphd.service.RankingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rankings")
public class RankingController {

    private final RankingService service;

    public RankingController(RankingService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResult<List<RankingViewPO>> list(
            @RequestParam String source,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "100") Integer limit) {
        if (year == null) {
            return ApiResult.ok(service.listBySource(source, limit));
        }
        return ApiResult.ok(service.listBySourceYear(source, year, limit));
    }

    @GetMapping("/latest")
    public ApiResult<List<RankingViewPO>> latest(
            @RequestParam String source,
            @RequestParam(defaultValue = "100") Integer limit) {
        return ApiResult.ok(service.listBySource(source, limit));
    }
}
