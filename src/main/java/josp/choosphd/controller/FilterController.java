package josp.choosphd.controller;

import josp.choosphd.common.ApiResult;
import josp.choosphd.mapper.DictMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/filter")
public class FilterController {

    private final DictMapper dictMapper;
    private final JdbcTemplate jdbc;

    public FilterController(DictMapper dictMapper, JdbcTemplate jdbc) {
        this.dictMapper = dictMapper;
        this.jdbc = jdbc;
    }

    @GetMapping("/regions")
    public ApiResult<List<Map<String, Object>>> regions() {
        return ApiResult.ok(dictMapper.regions());
    }

    @GetMapping("/countries")
    public ApiResult<List<Map<String, Object>>> countries(@RequestParam(required = false) String region) {
        if (region == null || region.isBlank()) {
            return ApiResult.ok(jdbc.queryForList(
                    "SELECT DISTINCT country_code AS code FROM university WHERE country_code IS NOT NULL AND deleted = 0 ORDER BY country_code"));
        }
        Integer regionId = dictMapper.findRegionIdByCode(region);
        if (regionId == null) return ApiResult.ok(List.of());
        return ApiResult.ok(jdbc.queryForList(
                "SELECT DISTINCT country_code AS code FROM university WHERE region_id = ? AND country_code IS NOT NULL AND deleted = 0 ORDER BY country_code",
                regionId));
    }

    @GetMapping("/subjects")
    public ApiResult<List<Map<String, Object>>> subjects() {
        return ApiResult.ok(dictMapper.subjects());
    }

    @GetMapping("/years")
    public ApiResult<List<Integer>> years() {
        return ApiResult.ok(jdbc.queryForList(
                "SELECT DISTINCT year FROM ranking_entry WHERE deleted = 0 ORDER BY year DESC", Integer.class));
    }

    @GetMapping("/sources")
    public ApiResult<List<Map<String, Object>>> sources() {
        return ApiResult.ok(dictMapper.sources());
    }
}
