package wo1261931780.chooseCollegeJava.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import wo1261931780.chooseCollegeJava.mapper.UniversityConsiderMapper;
import wo1261931780.chooseCollegeJava.entity.UniversityConsider;
/**
*Created by Intellij IDEA.
*Project:chooseCollegeJava
*Package:wo1261931780.chooseCollegeJava.service
*@author liujiajun_junw
*@Date 2024-10-13-39  星期六
*@Description 
*/

@Service
public class UniversityConsiderService extends ServiceImpl<UniversityConsiderMapper, UniversityConsider> {

    
    public int updateBatch(List<UniversityConsider> list) {
        return baseMapper.updateBatch(list);
    }
    
    public int updateBatchUseMultiQuery(List<UniversityConsider> list) {
        return baseMapper.updateBatchUseMultiQuery(list);
    }
    
    public int updateBatchSelective(List<UniversityConsider> list) {
        return baseMapper.updateBatchSelective(list);
    }
    
    public int batchInsert(List<UniversityConsider> list) {
        return baseMapper.batchInsert(list);
    }
    
    public int batchInsertSelectiveUseDefaultForNull(List<UniversityConsider> list) {
        return baseMapper.batchInsertSelectiveUseDefaultForNull(list);
    }
    
    public int deleteByPrimaryKeyIn(List<Integer> list) {
        return baseMapper.deleteByPrimaryKeyIn(list);
    }
    
    public boolean insertOrUpdate(UniversityConsider record) {
        return baseMapper.insertOrUpdate(record);
    }
    
    public int insertOrUpdateSelective(UniversityConsider record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}
