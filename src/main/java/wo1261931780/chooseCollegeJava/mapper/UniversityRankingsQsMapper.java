package wo1261931780.chooseCollegeJava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQs;

/**
 * UniversityRankingsQs数据访问层
 */
@Mapper
public interface UniversityRankingsQsMapper extends BaseMapper<UniversityRankingsQs> {
    /**
     * 批量更新
     */
    int updateBatch(@Param("list") List<UniversityRankingsQs> list);
    /**
     * 批量更新（多查询方式）
     */

    int updateBatchUseMultiQuery(@Param("list") List<UniversityRankingsQs> list);
    /**
     * 批量选择性更新
     */

    int updateBatchSelective(@Param("list") List<UniversityRankingsQs> list);
    /**
     * 批量插入
     */

    int batchInsert(@Param("list") List<UniversityRankingsQs> list);
    /**
     * 批量选择性插入（NULL 使用默认值）
     */

    int batchInsertSelectiveUseDefaultForNull(@Param("list") List<UniversityRankingsQs> list);
    /**
     * 按主键集合批量删除
     */

    int deleteByPrimaryKeyIn(List<Integer> list);
    /**
     * 新增或更新意向学校信息
     */

    boolean insertOrUpdate(UniversityRankingsQs record);
    /**
     * 选择性插入或更新
     */

    int insertOrUpdateSelective(UniversityRankingsQs record);
}