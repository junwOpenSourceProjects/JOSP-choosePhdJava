package wo1261931780.chooseCollegeJava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQsCs;

/**
*Created by Intellij IDEA.
*Project:chooseCollegeJava
*Package:wo1261931780.chooseCollegeJava.mapper
*@author liujiajun_junw
*@Date 2024-10-00-56  星期四
*@Description 
*/

@Mapper
public interface UniversityRankingsQsCsMapper extends BaseMapper<UniversityRankingsQsCs> {
    int updateBatch(@Param("list") List<UniversityRankingsQsCs> list);

    int updateBatchUseMultiQuery(@Param("list") List<UniversityRankingsQsCs> list);

    int updateBatchSelective(@Param("list") List<UniversityRankingsQsCs> list);

    int batchInsert(@Param("list") List<UniversityRankingsQsCs> list);

    int batchInsertSelectiveUseDefaultForNull(@Param("list") List<UniversityRankingsQsCs> list);

    int deleteByPrimaryKeyIn(List<Integer> list);

    boolean insertOrUpdate(UniversityRankingsQsCs record);

    int insertOrUpdateSelective(UniversityRankingsQsCs record);
}
