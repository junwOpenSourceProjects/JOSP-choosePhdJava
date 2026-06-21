package wo1261931780.chooseCollegeJava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsArwuSubject;

/**
 * UniversityRankingsArwuSubject数据访问层
 */
@Mapper
public interface UniversityRankingsArwuSubjectMapper extends BaseMapper<UniversityRankingsArwuSubject> {
    /**
     * 批量更新
     */
    int updateBatch(@Param("list") List<UniversityRankingsArwuSubject> list);
    int updateBatchUseMultiQuery(@Param("list") List<UniversityRankingsArwuSubject> list);
    int updateBatchSelective(@Param("list") List<UniversityRankingsArwuSubject> list);
    int batchInsert(@Param("list") List<UniversityRankingsArwuSubject> list);
    int batchInsertSelectiveUseDefaultForNull(@Param("list") List<UniversityRankingsArwuSubject> list);
    int deleteByPrimaryKeyIn(List<Integer> list);
    boolean insertOrUpdate(UniversityRankingsArwuSubject record);
    int insertOrUpdateSelective(UniversityRankingsArwuSubject record);

    /**
     * 分页 + 多条件查询 (跟 university_rankings_qs 一致)
     */
    java.util.List<UniversityRankingsArwuSubject> queryByCondition(
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
