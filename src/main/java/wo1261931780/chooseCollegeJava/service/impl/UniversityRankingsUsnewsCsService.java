package wo1261931780.chooseCollegeJava.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsUsnewsCsMapper;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsUsnewsCs;
/**
*Created by Intellij IDEA.
*Project:chooseCollegeJava
*Package:wo1261931780.chooseCollegeJava.service
*@author liujiajun_junw
*@Date 2024-10-00-56  星期四
*@Description 
*/

@Service
public class UniversityRankingsUsnewsCsService extends ServiceImpl<UniversityRankingsUsnewsCsMapper, UniversityRankingsUsnewsCs> {

    
    public int updateBatch(List<UniversityRankingsUsnewsCs> list) {
        return baseMapper.updateBatch(list);
    }
    
    public int updateBatchUseMultiQuery(List<UniversityRankingsUsnewsCs> list) {
        return baseMapper.updateBatchUseMultiQuery(list);
    }
    
    public int updateBatchSelective(List<UniversityRankingsUsnewsCs> list) {
        return baseMapper.updateBatchSelective(list);
    }
    
    public int batchInsert(List<UniversityRankingsUsnewsCs> list) {
        return baseMapper.batchInsert(list);
    }
    
    public int batchInsertSelectiveUseDefaultForNull(List<UniversityRankingsUsnewsCs> list) {
        return baseMapper.batchInsertSelectiveUseDefaultForNull(list);
    }
    
    public int deleteByPrimaryKeyIn(List<Integer> list) {
        return baseMapper.deleteByPrimaryKeyIn(list);
    }
    
    public boolean insertOrUpdate(UniversityRankingsUsnewsCs record) {
        return baseMapper.insertOrUpdate(record);
    }
    
    public int insertOrUpdateSelective(UniversityRankingsUsnewsCs record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}
