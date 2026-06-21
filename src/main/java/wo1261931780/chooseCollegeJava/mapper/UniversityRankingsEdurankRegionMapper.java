package wo1261931780.chooseCollegeJava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsEdurankRegion;

/**
 * UniversityRankingsEdurankRegion数据访问层
 */
@Mapper
public interface UniversityRankingsEdurankRegionMapper extends BaseMapper<UniversityRankingsEdurankRegion> {
    /**
     * 批量更新
     */
    int updateBatch(@Param("list") List<UniversityRankingsEdurankRegion> list);
    int updateBatchUseMultiQuery(@Param("list") List<UniversityRankingsEdurankRegion> list);
    int updateBatchSelective(@Param("list") List<UniversityRankingsEdurankRegion> list);
    int batchInsert(@Param("list") List<UniversityRankingsEdurankRegion> list);
    int batchInsertSelectiveUseDefaultForNull(@Param("list") List<UniversityRankingsEdurankRegion> list);
    int deleteByPrimaryKeyIn(List<Integer> list);
    boolean insertOrUpdate(UniversityRankingsEdurankRegion record);
    int insertOrUpdateSelective(UniversityRankingsEdurankRegion record);

    /**
     * 分页 + 多条件查询 (跟 university_rankings_qs 一致)
     */
    java.util.List<UniversityRankingsEdurankRegion> queryByCondition(
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
