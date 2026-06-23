package josp.choosphd.controller;

import josp.choosphd.common.ApiResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/overview")
public class OverviewController {

    private final JdbcTemplate jdbc;

    public OverviewController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/summary")
    public ApiResult<Map<String, Object>> summary() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("universityCount", jdbc.queryForObject(
                "SELECT COUNT(*) FROM university WHERE deleted = 0", Long.class));
        out.put("entryCount", jdbc.queryForObject(
                "SELECT COUNT(*) FROM ranking_entry WHERE deleted = 0", Long.class));
        out.put("trendCount", jdbc.queryForObject(
                "SELECT COUNT(*) FROM ranking_trend WHERE deleted = 0", Long.class));
        out.put("sourceCount", jdbc.queryForObject(
                "SELECT COUNT(*) FROM ranking_source WHERE deleted = 0", Long.class));
        out.put("regionCount", jdbc.queryForObject(
                "SELECT COUNT(*) FROM region WHERE deleted = 0", Long.class));
        out.put("latestYear", jdbc.queryForObject(
                "SELECT MAX(year) FROM ranking_entry WHERE deleted = 0", Integer.class));

        out.put("perSource", jdbc.queryForList("""
            SELECT s.code AS sourceCode,
                   COUNT(DISTINCT e.university_id) AS universities,
                   MAX(e.year) AS latestYear
            FROM ranking_entry e
            JOIN ranking_source s ON s.id = e.source_id
            WHERE e.deleted = 0
            GROUP BY s.code
            ORDER BY s.code
        """));

        out.put("topMovers", jdbc.queryForList("""
            SELECT u.url_id, u.name_en, u.name_zh,
                   s.code AS source_code, t.target_year AS year,
                   t.rank_to, t.rank_change, t.trend_type
            FROM ranking_trend t
            JOIN university u ON u.id = t.university_id
            JOIN ranking_source s ON s.id = t.source_id
            WHERE t.trend_type = 'GROWING' AND t.rank_change > 0
            ORDER BY t.rank_change DESC
            LIMIT 5
        """));

        return ApiResult.ok(out);
    }
}
