package wo1261931780.chooseCollegeJava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsUsnewsCs;

/**
*Created by Intellij IDEA.
*Project:chooseCollegeJava
*Package:wo1261931780.chooseCollegeJava.mapper
*@author liujiajun_junw
*@Date 2024-10-00-56  星期四
*@Description 
*/

@Mapper
public interface UniversityRankingsUsnewsCsMapper extends BaseMapper<UniversityRankingsUsnewsCs> {
    int updateBatch(@Param("list") List<UniversityRankingsUsnewsCs> list);

    int updateBatchUseMultiQuery(@Param("list") List<UniversityRankingsUsnewsCs> list);

    int updateBatchSelective(@Param("list") List<UniversityRankingsUsnewsCs> list);

    int batchInsert(@Param("list") List<UniversityRankingsUsnewsCs> list);

    int batchInsertSelectiveUseDefaultForNull(@Param("list") List<UniversityRankingsUsnewsCs> list);

    int deleteByPrimaryKeyIn(List<Integer> list);

    boolean insertOrUpdate(UniversityRankingsUsnewsCs record);

    int insertOrUpdateSelective(UniversityRankingsUsnewsCs record);
}
