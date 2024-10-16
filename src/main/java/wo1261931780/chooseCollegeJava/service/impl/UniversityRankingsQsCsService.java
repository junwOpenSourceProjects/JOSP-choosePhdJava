package wo1261931780.chooseCollegeJava.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsQsCsMapper;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQsCs;
/**
*Created by Intellij IDEA.
*Project:chooseCollegeJava
*Package:wo1261931780.chooseCollegeJava.service
*@author liujiajun_junw
*@Date 2024-10-00-56  星期四
*@Description 
*/

@Service
public class UniversityRankingsQsCsService extends ServiceImpl<UniversityRankingsQsCsMapper, UniversityRankingsQsCs> {

    
    public int updateBatch(List<UniversityRankingsQsCs> list) {
        return baseMapper.updateBatch(list);
    }
    
    public int updateBatchUseMultiQuery(List<UniversityRankingsQsCs> list) {
        return baseMapper.updateBatchUseMultiQuery(list);
    }
    
    public int updateBatchSelective(List<UniversityRankingsQsCs> list) {
        return baseMapper.updateBatchSelective(list);
    }
    
    public int batchInsert(List<UniversityRankingsQsCs> list) {
        return baseMapper.batchInsert(list);
    }
    
    public int batchInsertSelectiveUseDefaultForNull(List<UniversityRankingsQsCs> list) {
        return baseMapper.batchInsertSelectiveUseDefaultForNull(list);
    }
    
    public int deleteByPrimaryKeyIn(List<Integer> list) {
        return baseMapper.deleteByPrimaryKeyIn(list);
    }
    
    public boolean insertOrUpdate(UniversityRankingsQsCs record) {
        return baseMapper.insertOrUpdate(record);
    }
    
    public int insertOrUpdateSelective(UniversityRankingsQsCs record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}
