package wo1261931780.chooseCollegeJava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQs;

/**
*Created by Intellij IDEA.
*Project:chooseCollegeJava
*Package:wo1261931780.chooseCollegeJava.mapper
*@author liujiajun_junw
*@Date 2024-10-16-37  星期三
*@Description 
*/

@Mapper
public interface UniversityRankingsQsMapper extends BaseMapper<UniversityRankingsQs> {
    int updateBatch(@Param("list") List<UniversityRankingsQs> list);

    int updateBatchUseMultiQuery(@Param("list") List<UniversityRankingsQs> list);

    int updateBatchSelective(@Param("list") List<UniversityRankingsQs> list);

    int batchInsert(@Param("list") List<UniversityRankingsQs> list);

    int batchInsertSelectiveUseDefaultForNull(@Param("list") List<UniversityRankingsQs> list);

    int deleteByPrimaryKeyIn(List<Integer> list);

    boolean insertOrUpdate(UniversityRankingsQs record);

    int insertOrUpdateSelective(UniversityRankingsQs record);
}
