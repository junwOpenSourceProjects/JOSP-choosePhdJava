package wo1261931780.chooseCollegeJava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsEcharts;

/**
*Created by Intellij IDEA.
*Project:chooseCollegeJava
*Package:wo1261931780.chooseCollegeJava.mapper
*@author liujiajun_junw
*@Date 2024-10-04-18  星期五
*@Description 
*/

@Mapper
public interface UniversityRankingsEchartsMapper extends BaseMapper<UniversityRankingsEcharts> {
    int updateBatch(@Param("list") List<UniversityRankingsEcharts> list);

    int updateBatchUseMultiQuery(@Param("list") List<UniversityRankingsEcharts> list);

    int updateBatchSelective(@Param("list") List<UniversityRankingsEcharts> list);

    int batchInsert(@Param("list") List<UniversityRankingsEcharts> list);

    int batchInsertSelectiveUseDefaultForNull(@Param("list") List<UniversityRankingsEcharts> list);

    int deleteByPrimaryKeyIn(List<Integer> list);

    boolean insertOrUpdate(UniversityRankingsEcharts record);

    int insertOrUpdateSelective(UniversityRankingsEcharts record);
}
