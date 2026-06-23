package josp.choosphd.controller;

import josp.choosphd.api.university.dto.UniversityDetailDTO;
import josp.choosphd.common.ApiResult;
import josp.choosphd.domain.university.UniversityPO;
import josp.choosphd.mapper.UniversityMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/compare")
public class CompareController {

    private final UniversityMapper universityMapper;
    private final JdbcTemplate jdbc;

    public CompareController(UniversityMapper universityMapper, JdbcTemplate jdbc) {
        this.universityMapper = universityMapper;
        this.jdbc = jdbc;
    }

    @GetMapping
    public ApiResult<List<UniversityDetailDTO>> compare(@RequestParam List<String> ids) {
        if (ids == null || ids.isEmpty()) return ApiResult.ok(List.of());
        if (ids.size() > 8) return ApiResult.error(400, "一次最多对比 8 所院校");

        List<UniversityDetailDTO> out = new ArrayList<>();
        for (String id : ids) {
            UniversityPO u = universityMapper.findByUrlId(id);
            if (u == null) continue;

            List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT e.year, e.rank_display AS rnk, e.score, s.code AS sourceCode
                FROM ranking_entry e
                JOIN ranking_source s ON s.id = e.source_id
                WHERE e.university_id = ? AND e.deleted = 0
                ORDER BY s.code, e.year DESC
            """, u.getId());

            Map<String, List<UniversityDetailDTO.RankingEntryPoint>> latest = new LinkedHashMap<>();
            for (Map<String, Object> r : rows) {
                String src = (String) r.get("sourceCode");
                UniversityDetailDTO.RankingEntryPoint p = new UniversityDetailDTO.RankingEntryPoint();
                p.setYear(((Number) r.get("year")).intValue());
                p.setRank(r.get("rnk") == null ? null : ((Number) r.get("rnk")).intValue());
                p.setScore(r.get("score") == null ? null : ((Number) r.get("score")).doubleValue());
                latest.computeIfAbsent(src, k -> new ArrayList<>()).add(p);
            }

            UniversityDetailDTO d = new UniversityDetailDTO();
            d.setUrlId(u.getUrlId());
            d.setName(u.getNameEn());
            d.setCnName(u.getNameZh());
            d.setCountry(u.getCountryCode());
            d.setLogo(u.getLogoUrl());
            d.setRankings(latest);
            out.add(d);
        }
        return ApiResult.ok(out);
    }
}
