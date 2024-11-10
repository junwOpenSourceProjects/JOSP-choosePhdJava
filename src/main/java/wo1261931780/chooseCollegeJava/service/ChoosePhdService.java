package wo1261931780.chooseCollegeJava.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.List;
import wo1261931780.chooseCollegeJava.entity.ChoosePhd;
import wo1261931780.chooseCollegeJava.mapper.ChoosePhdMapper;
/**
*Created by Intellij IDEA.
*Project:JOSP-choosePhdJava
*Package:wo1261931780.chooseCollegeJava.service
*@author liujiajun_junw
*@Date 2024-11-16-53  星期日
*@Description 
*/

@Service
public class ChoosePhdService extends ServiceImpl<ChoosePhdMapper, ChoosePhd> {

    
    public int updateBatch(List<ChoosePhd> list) {
        return baseMapper.updateBatch(list);
    }
    
    public int updateBatchUseMultiQuery(List<ChoosePhd> list) {
        return baseMapper.updateBatchUseMultiQuery(list);
    }
    
    public int updateBatchSelective(List<ChoosePhd> list) {
        return baseMapper.updateBatchSelective(list);
    }
    
    public int batchInsert(List<ChoosePhd> list) {
        return baseMapper.batchInsert(list);
    }
    
    public int batchInsertSelectiveUseDefaultForNull(List<ChoosePhd> list) {
        return baseMapper.batchInsertSelectiveUseDefaultForNull(list);
    }
    
    public int deleteByPrimaryKeyIn(List<Long> list) {
        return baseMapper.deleteByPrimaryKeyIn(list);
    }
    
    public boolean insertOrUpdate(ChoosePhd record) {
        return baseMapper.insertOrUpdate(record);
    }
    
    public int insertOrUpdateSelective(ChoosePhd record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}
