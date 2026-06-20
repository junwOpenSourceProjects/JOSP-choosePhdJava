package wo1261931780.chooseCollegeJava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsAll;

/**
 * UniversityRankingsAll数据访问层
 */
@Mapper
public interface UniversityRankingsAllMapper extends BaseMapper<UniversityRankingsAll> {
    /**
     * 批量更新
     */
    int updateBatch(@Param("list") List<UniversityRankingsAll> list);
    /**
     * 批量更新（多查询方式）
     */

    int updateBatchUseMultiQuery(@Param("list") List<UniversityRankingsAll> list);
    /**
     * 批量选择性更新
     */

    int updateBatchSelective(@Param("list") List<UniversityRankingsAll> list);
    /**
     * 批量插入
     */

    int batchInsert(@Param("list") List<UniversityRankingsAll> list);
    /**
     * 批量选择性插入（NULL 使用默认值）
     */

    int batchInsertSelectiveUseDefaultForNull(@Param("list") List<UniversityRankingsAll> list);
    /**
     * 按主键集合批量删除
     */

    int deleteByPrimaryKeyIn(List<Integer> list);
    /**
     * 新增或更新意向学校信息
     */

    boolean insertOrUpdate(UniversityRankingsAll record);
    /**
     * 选择性插入或更新
     */

    int insertOrUpdateSelective(UniversityRankingsAll record);
    /**
     * 从 qs (qs_world variant) + usnews (the_world variant) 两张主表按 (name, year) GROUP BY 聚合
     * INSERT INTO university_rankings_all, 返回影响行数
     */
    int aggregateFromRawTables();
    /**
     * 取出所有去重 (按 name) 的大学 + tags + tags_state (兼容 only_full_group_by)
     * 用在 echarts / chart series 取数, 替代 LambdaQueryWrapper.groupBy() 的 only_full_group_by 错误
     */
    java.util.List<UniversityRankingsAll> selectDistinctUniversities();
}