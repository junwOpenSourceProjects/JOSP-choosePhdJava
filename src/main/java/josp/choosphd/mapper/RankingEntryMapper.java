package josp.choosphd.mapper;

import josp.choosphd.domain.ranking.RankingEntryPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RankingEntryMapper {

    @Select("""
        SELECT id, university_id AS universityId, source_id AS sourceId,
               subject_id AS subjectId, year, rank_display AS rankDisplay,
               rank_exact AS rankExact, score, indicators,
               source_raw_id AS sourceRawId, created_at AS createdAt, updated_at AS updatedAt, deleted
        FROM ranking_entry
        WHERE university_id = #{universityId}
        ORDER BY source_id, year DESC, score DESC
    """)
    List<RankingEntryPO> listByUniversityId(Long universityId);

    @Select("""
        SELECT id, university_id AS universityId, source_id AS sourceId,
               subject_id AS subjectId, year, rank_display AS rankDisplay,
               rank_exact AS rankExact, score, indicators,
               source_raw_id AS sourceRawId, created_at AS createdAt, updated_at AS updatedAt, deleted
        FROM ranking_entry
        WHERE source_id = #{sourceId}
        ORDER BY year DESC, rank_display ASC
    """)
    List<RankingEntryPO> listBySourceId(@Param("sourceId") Integer sourceId);

    @Select("SELECT COUNT(*) FROM ranking_entry WHERE deleted = 0")
    long countAll();

    @Insert("""
        INSERT INTO ranking_entry
            (university_id, source_id, subject_id, year, rank_display, rank_exact, score,
             indicators, source_raw_id, created_at, updated_at)
        VALUES
            (#{universityId}, #{sourceId}, #{subjectId}, #{year}, #{rankDisplay}, #{rankExact}, #{score},
             #{indicators}, #{sourceRawId}, #{createdAt}, #{updatedAt})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RankingEntryPO e);

    @Delete("""
        DELETE FROM ranking_entry
        WHERE university_id = #{universityId} AND source_id = #{sourceId} AND year = #{year}
        AND (subject_id = #{subjectId} OR (subject_id IS NULL AND #{subjectId} IS NULL))
    """)
    int deleteByNaturalKey(@Param("universityId") Long universityId,
                           @Param("sourceId") Integer sourceId,
                           @Param("year") Short year,
                           @Param("subjectId") Integer subjectId);
}
