package wo1261931780.chooseCollegeJava.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsAllMapper;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsAll;
/**
*Created by Intellij IDEA.
*Project:chooseCollegeJava
*Package:wo1261931780.chooseCollegeJava.service
*@author liujiajun_junw
*@Date 2024-10-15-06  星期四
*@Description 
*/

@Service
public class UniversityRankingsAllService extends ServiceImpl<UniversityRankingsAllMapper, UniversityRankingsAll> {

    
    public int updateBatch(List<UniversityRankingsAll> list) {
        return baseMapper.updateBatch(list);
    }
    
    public int updateBatchUseMultiQuery(List<UniversityRankingsAll> list) {
        return baseMapper.updateBatchUseMultiQuery(list);
    }
    
    public int updateBatchSelective(List<UniversityRankingsAll> list) {
        return baseMapper.updateBatchSelective(list);
    }
    
    public int batchInsert(List<UniversityRankingsAll> list) {
        return baseMapper.batchInsert(list);
    }
    
    public int batchInsertSelectiveUseDefaultForNull(List<UniversityRankingsAll> list) {
        return baseMapper.batchInsertSelectiveUseDefaultForNull(list);
    }
    
    public int deleteByPrimaryKeyIn(List<Integer> list) {
        return baseMapper.deleteByPrimaryKeyIn(list);
    }
    
    public boolean insertOrUpdate(UniversityRankingsAll record) {
        return baseMapper.insertOrUpdate(record);
    }
    
    public int insertOrUpdateSelective(UniversityRankingsAll record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}
