package wo1261931780.chooseCollegeJava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsAll;

/**
*Created by Intellij IDEA.
*Project:chooseCollegeJava
*Package:wo1261931780.chooseCollegeJava.mapper
*@author liujiajun_junw
*@Date 2024-10-15-06  星期四
*@Description 
*/

@Mapper
public interface UniversityRankingsAllMapper extends BaseMapper<UniversityRankingsAll> {
    int updateBatch(@Param("list") List<UniversityRankingsAll> list);

    int updateBatchUseMultiQuery(@Param("list") List<UniversityRankingsAll> list);

    int updateBatchSelective(@Param("list") List<UniversityRankingsAll> list);

    int batchInsert(@Param("list") List<UniversityRankingsAll> list);

    int batchInsertSelectiveUseDefaultForNull(@Param("list") List<UniversityRankingsAll> list);

    int deleteByPrimaryKeyIn(List<Integer> list);

    boolean insertOrUpdate(UniversityRankingsAll record);

    int insertOrUpdateSelective(UniversityRankingsAll record);
}
