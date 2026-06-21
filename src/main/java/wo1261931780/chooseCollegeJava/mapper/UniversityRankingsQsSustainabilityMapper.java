package wo1261931780.chooseCollegeJava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQsSustainability;

/**
 * UniversityRankingsQsSustainability数据访问层
 */
@Mapper
public interface UniversityRankingsQsSustainabilityMapper extends BaseMapper<UniversityRankingsQsSustainability> {
    /**
     * 批量更新
     */
    int updateBatch(@Param("list") List<UniversityRankingsQsSustainability> list);
    int updateBatchUseMultiQuery(@Param("list") List<UniversityRankingsQsSustainability> list);
    int updateBatchSelective(@Param("list") List<UniversityRankingsQsSustainability> list);
    int batchInsert(@Param("list") List<UniversityRankingsQsSustainability> list);
    int batchInsertSelectiveUseDefaultForNull(@Param("list") List<UniversityRankingsQsSustainability> list);
    int deleteByPrimaryKeyIn(List<Integer> list);
    boolean insertOrUpdate(UniversityRankingsQsSustainability record);
    int insertOrUpdateSelective(UniversityRankingsQsSustainability record);

    /**
     * 分页 + 多条件查询 (跟 university_rankings_qs 一致)
     */
    java.util.List<UniversityRankingsQsSustainability> queryByCondition(
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
