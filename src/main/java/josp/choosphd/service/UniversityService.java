package josp.choosphd.service;

import josp.choosphd.api.university.dto.UniversityDetailDTO;
import josp.choosphd.api.university.dto.UniversityListDTO;
import josp.choosphd.api.university.dto.UniversityListQuery;
import josp.choosphd.common.PageRequest;
import josp.choosphd.domain.university.UniversityPO;
import josp.choosphd.mapper.UniversityMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UniversityService {

    private final UniversityMapper universityMapper;
    private final JdbcTemplate jdbc;

    public UniversityService(UniversityMapper universityMapper, JdbcTemplate jdbc) {
        this.universityMapper = universityMapper;
        this.jdbc = jdbc;
    }

    public Map<String, Object> list(UniversityListQuery q, PageRequest pr) {
        StringBuilder where = new StringBuilder(" WHERE u.deleted = 0 ");
        List<Object> args = new ArrayList<>();
        if (q.getQ() != null && !q.getQ().isBlank()) {
            where.append(" AND (u.name_en LIKE ? OR u.name_zh LIKE ? OR u.url_id LIKE ?) ");
            String like = "%" + q.getQ() + "%";
            args.add(like); args.add(like); args.add(like);
        }
        if (q.getRegion() != null && !q.getRegion().isBlank()) {
            where.append(" AND r.code = ? ");
            args.add(q.getRegion());
        }
        if (q.getSource() != null && !q.getSource().isBlank()) {
            where.append(" AND EXISTS (SELECT 1 FROM ranking_entry re JOIN ranking_source rs ON rs.id = re.source_id WHERE re.university_id = u.id AND rs.code = ?) ");
            args.add(q.getSource());
        }
        if (q.getRankMin() != null) {
            where.append(" AND u.id IN (SELECT re2.university_id FROM ranking_entry re2 WHERE re2.rank_display >= ?) ");
            args.add(q.getRankMin());
        }
        if (q.getRankMax() != null) {
            where.append(" AND u.id IN (SELECT re2.university_id FROM ranking_entry re2 WHERE re2.rank_display <= ?) ");
            args.add(q.getRankMax());
        }
        if (q.getTrend() != null && !q.getTrend().isBlank()) {
            String trendType = "UP".equalsIgnoreCase(q.getTrend()) ? "GROWING" :
                    "DOWN".equalsIgnoreCase(q.getTrend()) ? "DECLINING" : q.getTrend().toUpperCase(Locale.ROOT);
            where.append(" AND u.id IN (SELECT rt.university_id FROM ranking_trend rt WHERE rt.trend_type = ?) ");
            args.add(trendType);
        }

        // count
        String countSql = "SELECT COUNT(*) FROM university u " +
                "LEFT JOIN region r ON r.id = u.region_id " + where;
        Long total = jdbc.queryForObject(countSql, Long.class, args.toArray());

        String orderBy = " ORDER BY u.id ASC ";
        if ("name".equalsIgnoreCase(q.getSortBy())) orderBy = " ORDER BY u.name_en ASC ";

        int offset = (pr.pageNo() - 1) * pr.pageSize();

        // 用相关子查询拿每个大学的"最新一条 entry"和 trend(避免 GROUP BY)
        String sourceFilterSql = "";
        if (q.getSource() != null && !q.getSource().isBlank()) {
            sourceFilterSql = " AND rs.code = '" + q.getSource().replace("'", "''") + "' ";
        }

        String listSql = "SELECT u.id, u.url_id AS urlId, u.name_en AS nameEn, u.name_zh AS nameZh, " +
                "u.logo_url AS logoUrl, r.code AS regionCode, u.country_code AS countryCode, " +
                "(SELECT re.rank_display FROM ranking_entry re " +
                "   JOIN ranking_source rs ON rs.id = re.source_id " +
                "   WHERE re.university_id = u.id " + sourceFilterSql +
                "   ORDER BY re.year DESC, re.score DESC LIMIT 1) AS rnk, " +
                "(SELECT re.score FROM ranking_entry re " +
                "   JOIN ranking_source rs ON rs.id = re.source_id " +
                "   WHERE re.university_id = u.id " + sourceFilterSql +
                "   ORDER BY re.year DESC, re.score DESC LIMIT 1) AS score, " +
                "(SELECT re.year FROM ranking_entry re " +
                "   JOIN ranking_source rs ON rs.id = re.source_id " +
                "   WHERE re.university_id = u.id " + sourceFilterSql +
                "   ORDER BY re.year DESC, re.score DESC LIMIT 1) AS year, " +
                "(SELECT rs.code FROM ranking_entry re " +
                "   JOIN ranking_source rs ON rs.id = re.source_id " +
                "   WHERE re.university_id = u.id " + sourceFilterSql +
                "   ORDER BY re.year DESC, re.score DESC LIMIT 1) AS sourceCode, " +
                "(SELECT rt.trend_type FROM ranking_trend rt WHERE rt.university_id = u.id ORDER BY rt.target_year DESC LIMIT 1) AS trend, " +
                "(SELECT rt.rank_change FROM ranking_trend rt WHERE rt.university_id = u.id ORDER BY rt.target_year DESC LIMIT 1) AS rankDelta " +
                "FROM university u " +
                "LEFT JOIN region r ON r.id = u.region_id " +
                where +
                orderBy +
                " LIMIT " + pr.pageSize() + " OFFSET " + offset;

        List<UniversityListDTO> rows = jdbc.query(listSql, args.toArray(), (rs, i) -> {
            UniversityListDTO d = new UniversityListDTO();
            d.setUrlId(rs.getString("urlId"));
            d.setName(rs.getString("nameEn"));
            d.setCnName(rs.getString("nameZh"));
            d.setRegion(rs.getString("regionCode"));
            d.setCountry(rs.getString("countryCode"));
            d.setLogo(rs.getString("logoUrl"));
            int rank = rs.getInt("rnk");
            d.setLatestRank(rs.wasNull() ? null : rank);
            java.math.BigDecimal score = rs.getBigDecimal("score");
            d.setLatestScore(score == null ? null : score.doubleValue());
            short year = rs.getShort("year");
            d.setLatestYear(rs.wasNull() ? null : (int) year);
            d.setSourceCode(rs.getString("sourceCode"));
            d.setTrend(rs.getString("trend"));
            int delta = rs.getInt("rankDelta");
            d.setRankDelta(rs.wasNull() ? null : delta);
            return d;
        });

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("list", rows);
        out.put("total", total == null ? 0L : total);
        out.put("pageNo", pr.pageNo());
        out.put("pageSize", pr.pageSize());
        return out;
    }

    public UniversityDetailDTO detail(String urlId) {
        UniversityPO u = universityMapper.findByUrlId(urlId);
        if (u == null) return null;

        List<Map<String, Object>> entryRows = jdbc.queryForList("""
            SELECT e.year, e.rank_display AS rnk, e.score, s.code AS sourceCode
            FROM ranking_entry e
            JOIN ranking_source s ON s.id = e.source_id
            WHERE e.university_id = ? AND e.deleted = 0
            ORDER BY s.code, e.year DESC
        """, u.getId());
        Map<String, List<UniversityDetailDTO.RankingEntryPoint>> rkMap = new LinkedHashMap<>();
        for (Map<String, Object> r : entryRows) {
            String src = (String) r.get("sourceCode");
            UniversityDetailDTO.RankingEntryPoint p = new UniversityDetailDTO.RankingEntryPoint();
            p.setYear(((Number) r.get("year")).intValue());
            p.setRank(r.get("rnk") == null ? null : ((Number) r.get("rnk")).intValue());
            p.setScore(r.get("score") == null ? null : ((Number) r.get("score")).doubleValue());
            rkMap.computeIfAbsent(src, k -> new ArrayList<>()).add(p);
        }

        List<Map<String, Object>> trendRows = jdbc.queryForList("""
            SELECT t.target_year AS year, t.rank_to AS rnk, t.trend_type AS direction, s.code AS sourceCode
            FROM ranking_trend t
            JOIN ranking_source s ON s.id = t.source_id
            WHERE t.university_id = ? AND t.deleted = 0
            ORDER BY s.code, t.target_year DESC
        """, u.getId());
        Map<String, List<UniversityDetailDTO.TrendPoint>> trMap = new LinkedHashMap<>();
        for (Map<String, Object> r : trendRows) {
            String src = (String) r.get("sourceCode");
            UniversityDetailDTO.TrendPoint p = new UniversityDetailDTO.TrendPoint();
            p.setYear(((Number) r.get("year")).intValue());
            p.setRank(r.get("rnk") == null ? null : ((Number) r.get("rnk")).intValue());
            p.setDirection((String) r.get("direction"));
            trMap.computeIfAbsent(src, k -> new ArrayList<>()).add(p);
        }

        List<String> subjects = jdbc.queryForList("""
            SELECT DISTINCT sb.code
            FROM ranking_entry e
            JOIN subject sb ON sb.id = e.subject_id
            WHERE e.university_id = ? AND e.deleted = 0
        """, String.class, u.getId());

        UniversityDetailDTO d = new UniversityDetailDTO();
        d.setUrlId(u.getUrlId());
        d.setName(u.getNameEn());
        d.setCnName(u.getNameZh());
        d.setRegion(u.getRegionId() == null ? null : lookupRegionCode(u.getRegionId()));
        d.setCountry(u.getCountryCode());
        d.setLogo(u.getLogoUrl());
        d.setRankings(rkMap);
        d.setTrends(trMap);
        d.setSubjects(subjects);
        return d;
    }

    private String lookupRegionCode(Integer regionId) {
        try {
            return jdbc.queryForObject("SELECT code FROM region WHERE id = ?", String.class, regionId);
        } catch (Exception e) {
            return null;
        }
    }
}
