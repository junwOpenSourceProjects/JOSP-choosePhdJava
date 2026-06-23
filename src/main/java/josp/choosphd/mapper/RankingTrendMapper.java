package josp.choosphd.mapper;

import josp.choosphd.domain.trend.RankingTrendPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RankingTrendMapper {

    @Select("""
        SELECT id, university_id AS universityId, source_id AS sourceId,
               subject_id AS subjectId, trend_type AS trendType,
               base_year AS baseYear, target_year AS targetYear,
               rank_change AS rankChange, rank_from AS rankFrom, rank_to AS rankTo,
               note, created_at AS createdAt, deleted
        FROM ranking_trend
        WHERE university_id = #{universityId}
        ORDER BY source_id, target_year DESC
    """)
    List<RankingTrendPO> listByUniversityId(Long universityId);

    @Select("""
        SELECT u.url_id AS urlId,
               u.name_en AS nameEn,
               s.code AS sourceCode,
               rt.trend_type AS trendType,
               rt.base_year AS baseYear,
               rt.target_year AS targetYear,
               rt.rank_from AS rankFrom,
               rt.rank_to AS rankTo,
               rt.rank_change AS rankChange
        FROM ranking_trend rt
        JOIN university u ON u.id = rt.university_id AND u.deleted = 0
        JOIN ranking_source s ON s.id = rt.source_id AND s.deleted = 0
        WHERE rt.source_id = #{sourceId} AND rt.trend_type = #{trendType} AND rt.deleted = 0
        ORDER BY rt.rank_change DESC
        LIMIT #{limit}
    """)
    List<josp.choosphd.api.trend.dto.TrendRowDTO> listBySourceAndType(@Param("sourceId") Integer sourceId,
                                                                      @Param("trendType") String trendType,
                                                                      @Param("limit") Integer limit);

    @Select("SELECT COUNT(*) FROM ranking_trend WHERE deleted = 0")
    long countAll();

    @Insert("""
        INSERT INTO ranking_trend
            (university_id, source_id, subject_id, trend_type, base_year, target_year,
             rank_change, rank_from, rank_to, note, created_at)
        VALUES
            (#{universityId}, #{sourceId}, #{subjectId}, #{trendType}, #{baseYear}, #{targetYear},
             #{rankChange}, #{rankFrom}, #{rankTo}, #{note}, #{createdAt})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RankingTrendPO t);

    @Delete("""
        DELETE FROM ranking_trend
        WHERE university_id = #{universityId} AND source_id = #{sourceId}
          AND base_year = #{baseYear} AND target_year = #{targetYear}
          AND trend_type = #{trendType}
    """)
    int deleteByNaturalKey(@Param("universityId") Long universityId,
                           @Param("sourceId") Integer sourceId,
                           @Param("baseYear") Short baseYear,
                           @Param("targetYear") Short targetYear,
                           @Param("trendType") String trendType);
}
