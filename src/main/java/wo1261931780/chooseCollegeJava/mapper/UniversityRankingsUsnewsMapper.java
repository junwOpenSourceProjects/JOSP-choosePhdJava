package wo1261931780.chooseCollegeJava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsUsnews;

/**
*Created by Intellij IDEA.
*Project:chooseCollegeJava
*Package:wo1261931780.chooseCollegeJava.mapper
*@author liujiajun_junw
*@Date 2024-10-16-37  星期三
*@Description 
*/

@Mapper
public interface UniversityRankingsUsnewsMapper extends BaseMapper<UniversityRankingsUsnews> {
    int updateBatch(@Param("list") List<UniversityRankingsUsnews> list);

    int updateBatchUseMultiQuery(@Param("list") List<UniversityRankingsUsnews> list);

    int updateBatchSelective(@Param("list") List<UniversityRankingsUsnews> list);

    int batchInsert(@Param("list") List<UniversityRankingsUsnews> list);

    int batchInsertSelectiveUseDefaultForNull(@Param("list") List<UniversityRankingsUsnews> list);

    int deleteByPrimaryKeyIn(List<Integer> list);

    boolean insertOrUpdate(UniversityRankingsUsnews record);

    int insertOrUpdateSelective(UniversityRankingsUsnews record);
}
