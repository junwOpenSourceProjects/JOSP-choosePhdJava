package wo1261931780.chooseCollegeJava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsMosiurWorld;

/**
 * UniversityRankingsMosiurWorld数据访问层
 */
@Mapper
public interface UniversityRankingsMosiurWorldMapper extends BaseMapper<UniversityRankingsMosiurWorld> {
    /**
     * 批量更新
     */
    int updateBatch(@Param("list") List<UniversityRankingsMosiurWorld> list);
    int updateBatchUseMultiQuery(@Param("list") List<UniversityRankingsMosiurWorld> list);
    int updateBatchSelective(@Param("list") List<UniversityRankingsMosiurWorld> list);
    int batchInsert(@Param("list") List<UniversityRankingsMosiurWorld> list);
    int batchInsertSelectiveUseDefaultForNull(@Param("list") List<UniversityRankingsMosiurWorld> list);
    int deleteByPrimaryKeyIn(List<Integer> list);
    boolean insertOrUpdate(UniversityRankingsMosiurWorld record);
    int insertOrUpdateSelective(UniversityRankingsMosiurWorld record);

    /**
     * 分页 + 多条件查询 (跟 university_rankings_qs 一致)
     */
    java.util.List<UniversityRankingsMosiurWorld> queryByCondition(
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
