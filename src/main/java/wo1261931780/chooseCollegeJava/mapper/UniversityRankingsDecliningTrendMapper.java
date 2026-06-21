package wo1261931780.chooseCollegeJava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsDecliningTrend;

/**
 * UniversityRankingsDecliningTrend数据访问层
 */
@Mapper
public interface UniversityRankingsDecliningTrendMapper extends BaseMapper<UniversityRankingsDecliningTrend> {
    /**
     * 批量更新
     */
    int updateBatch(@Param("list") List<UniversityRankingsDecliningTrend> list);
    int updateBatchUseMultiQuery(@Param("list") List<UniversityRankingsDecliningTrend> list);
    int updateBatchSelective(@Param("list") List<UniversityRankingsDecliningTrend> list);
    int batchInsert(@Param("list") List<UniversityRankingsDecliningTrend> list);
    int batchInsertSelectiveUseDefaultForNull(@Param("list") List<UniversityRankingsDecliningTrend> list);
    int deleteByPrimaryKeyIn(List<Integer> list);
    boolean insertOrUpdate(UniversityRankingsDecliningTrend record);
    int insertOrUpdateSelective(UniversityRankingsDecliningTrend record);

    /**
     * 分页 + 多条件查询 (跟 university_rankings_qs 一致)
     */
    java.util.List<UniversityRankingsDecliningTrend> queryByCondition(
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
}
