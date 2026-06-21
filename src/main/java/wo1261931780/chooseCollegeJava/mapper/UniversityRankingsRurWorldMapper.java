package wo1261931780.chooseCollegeJava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsRurWorld;

/**
 * UniversityRankingsRurWorld数据访问层
 */
@Mapper
public interface UniversityRankingsRurWorldMapper extends BaseMapper<UniversityRankingsRurWorld> {
    /**
     * 批量更新
     */
    int updateBatch(@Param("list") List<UniversityRankingsRurWorld> list);
    int updateBatchUseMultiQuery(@Param("list") List<UniversityRankingsRurWorld> list);
    int updateBatchSelective(@Param("list") List<UniversityRankingsRurWorld> list);
    int batchInsert(@Param("list") List<UniversityRankingsRurWorld> list);
    int batchInsertSelectiveUseDefaultForNull(@Param("list") List<UniversityRankingsRurWorld> list);
    int deleteByPrimaryKeyIn(List<Integer> list);
    boolean insertOrUpdate(UniversityRankingsRurWorld record);
    int insertOrUpdateSelective(UniversityRankingsRurWorld record);

    /**
     * 分页 + 多条件查询 (跟 university_rankings_qs 一致)
     */
    java.util.List<UniversityRankingsRurWorld> queryByCondition(
        @Param("universityNameChinese") String universityNameChinese,
        @Param("universityTags") String universityTags,
        @Param("universityTagsState") String universityTagsState,
        @Param("rankingCategory") String rankingCategory,
        @Param("rankingYear") String rankingYear,
        @Param("currentRankLimit") Integer currentRankLimit,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );

    int countByCondition(
        @Param("universityNameChinese") String universityNameChinese,
        @Param("universityTags") String universityTags,
        @Param("universityTagsState") String universityTagsState,
        @Param("rankingCategory") String rankingCategory,
        @Param("rankingYear") String rankingYear,
        @Param("currentRankLimit") Integer currentRankLimit
    );

    /**
     * 列出所有 distinct ranking_year (按年倒序, 供前端 filter)
     */
    java.util.List<String> selectDistinctYears();
}
