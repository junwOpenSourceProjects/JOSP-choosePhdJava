package wo1261931780.chooseCollegeJava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import wo1261931780.chooseCollegeJava.entity.UniversityConsider;

/**
*Created by Intellij IDEA.
*Project:chooseCollegeJava
*Package:wo1261931780.chooseCollegeJava.mapper
*@author liujiajun_junw
*@Date 2024-10-13-39  星期六
*@Description 
*/

@Mapper
public interface UniversityConsiderMapper extends BaseMapper<UniversityConsider> {
    int updateBatch(@Param("list") List<UniversityConsider> list);

    int updateBatchUseMultiQuery(@Param("list") List<UniversityConsider> list);

    int updateBatchSelective(@Param("list") List<UniversityConsider> list);

    int batchInsert(@Param("list") List<UniversityConsider> list);

    int batchInsertSelectiveUseDefaultForNull(@Param("list") List<UniversityConsider> list);

    int deleteByPrimaryKeyIn(List<Integer> list);

    boolean insertOrUpdate(UniversityConsider record);

    int insertOrUpdateSelective(UniversityConsider record);
}
