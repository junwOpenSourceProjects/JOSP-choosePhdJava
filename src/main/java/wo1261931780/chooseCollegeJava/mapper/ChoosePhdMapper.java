package wo1261931780.chooseCollegeJava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import wo1261931780.chooseCollegeJava.entity.ChoosePhd;

/**
*Created by Intellij IDEA.
*Project:JOSP-choosePhdJava
*Package:wo1261931780.chooseCollegeJava.mapper
*@author liujiajun_junw
*@Date 2024-11-16-53  星期日
*@Description 
*/

@Mapper
public interface ChoosePhdMapper extends BaseMapper<ChoosePhd> {
    int updateBatch(@Param("list") List<ChoosePhd> list);

    int updateBatchUseMultiQuery(@Param("list") List<ChoosePhd> list);

    int updateBatchSelective(@Param("list") List<ChoosePhd> list);

    int batchInsert(@Param("list") List<ChoosePhd> list);

    int batchInsertSelectiveUseDefaultForNull(@Param("list") List<ChoosePhd> list);

    int deleteByPrimaryKeyIn(List<Long> list);

    boolean insertOrUpdate(ChoosePhd record);

    int insertOrUpdateSelective(ChoosePhd record);
}
