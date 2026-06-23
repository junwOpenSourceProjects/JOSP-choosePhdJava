package josp.choosphd.service;

import josp.choosphd.domain.ranking.RankingViewPO;
import josp.choosphd.mapper.DictMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RankingService {

    private final JdbcTemplate jdbc;
    private final DictMapper dictMapper;

    public RankingService(JdbcTemplate jdbc, DictMapper dictMapper) {
        this.jdbc = jdbc;
        this.dictMapper = dictMapper;
    }

    /**
     * 某 source + year 的完整榜单
     */
    public List<RankingViewPO> listBySourceYear(String sourceCode, Integer year, Integer limit) {
        Integer sourceId = dictMapper.findSourceIdByCode(sourceCode);
        if (sourceId == null) return List.of();

        String sql = """
            SELECT e.id, e.rank_display AS rnk, e.score, e.year,
                   u.url_id AS urlId, u.name_en AS nameEn, u.name_zh AS nameZh,
                   u.logo_url AS logoUrl,
                   r.code AS regionCode, u.country_code AS countryCode,
                   s.code AS sourceCode, sb.code AS subjectCode,
                   t.trend_type AS trend, t.rank_change AS rankDelta
            FROM ranking_entry e
            JOIN university u ON u.id = e.university_id
            JOIN ranking_source s ON s.id = e.source_id
            LEFT JOIN region r ON r.id = u.region_id
            LEFT JOIN subject sb ON sb.id = e.subject_id
            LEFT JOIN ranking_trend t
                ON t.university_id = e.university_id
               AND t.source_id = e.source_id
               AND t.target_year = e.year
            WHERE e.source_id = ? AND e.deleted = 0
              AND (? IS NULL OR e.year = ?)
            ORDER BY e.rank_display ASC
            LIMIT ?
        """;
        return jdbc.query(sql, (rs, i) -> toView(rs), sourceId, year, year, limit);
    }

    public List<RankingViewPO> listBySource(String sourceCode, Integer limit) {
        Integer sourceId = dictMapper.findSourceIdByCode(sourceCode);
        if (sourceId == null) return List.of();

        String sql = """
            SELECT * FROM (
                SELECT e.id, e.rank_display AS rnk, e.score, e.year,
                       u.url_id AS urlId, u.name_en AS nameEn, u.name_zh AS nameZh,
                       u.logo_url AS logoUrl,
                       r.code AS regionCode, u.country_code AS countryCode,
                       s.code AS sourceCode, sb.code AS subjectCode,
                       t.trend_type AS trend, t.rank_change AS rankDelta,
                       ROW_NUMBER() OVER (PARTITION BY e.university_id ORDER BY e.year DESC) AS rn
                FROM ranking_entry e
                JOIN university u ON u.id = e.university_id
                JOIN ranking_source s ON s.id = e.source_id
                LEFT JOIN region r ON r.id = u.region_id
                LEFT JOIN subject sb ON sb.id = e.subject_id
                LEFT JOIN ranking_trend t
                    ON t.university_id = e.university_id
                   AND t.source_id = e.source_id
                   AND t.target_year = e.year
                WHERE e.source_id = ? AND e.deleted = 0
            ) latest
            WHERE latest.rn = 1
            ORDER BY latest.rnk ASC
            LIMIT ?
        """;
        return jdbc.query(sql, (rs, i) -> toView(rs), sourceId, limit);
    }

    private RankingViewPO toView(java.sql.ResultSet rs) throws java.sql.SQLException {
        RankingViewPO v = new RankingViewPO();
        v.setId(rs.getLong("id"));
        int rank = rs.getInt("rnk");
        v.setRank(rs.wasNull() ? null : rank);
        v.setScore(rs.getBigDecimal("score"));
        short year = rs.getShort("year");
        v.setYear(rs.wasNull() ? null : year);
        v.setUrlId(rs.getString("urlId"));
        v.setNameEn(rs.getString("nameEn"));
        v.setNameZh(rs.getString("nameZh"));
        v.setLogoUrl(rs.getString("logoUrl"));
        v.setRegionCode(rs.getString("regionCode"));
        v.setCountryCode(rs.getString("countryCode"));
        v.setSourceCode(rs.getString("sourceCode"));
        v.setSubjectCode(rs.getString("subjectCode"));
        v.setTrend(rs.getString("trend"));
        int delta = rs.getInt("rankDelta");
        v.setRankDelta(rs.wasNull() ? null : delta);
        return v;
    }
}
