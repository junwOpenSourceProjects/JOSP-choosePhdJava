package wo1261931780.chooseCollegeJava.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.List;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsEchartsMapper;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsEcharts;
/**
*Created by Intellij IDEA.
*Project:chooseCollegeJava
*Package:wo1261931780.chooseCollegeJava.service
*@author liujiajun_junw
*@Date 2024-10-04-18  星期五
*@Description 
*/

@Service
public class UniversityRankingsEchartsService extends ServiceImpl<UniversityRankingsEchartsMapper, UniversityRankingsEcharts> {

    
    public int updateBatch(List<UniversityRankingsEcharts> list) {
        return baseMapper.updateBatch(list);
    }
    
    public int updateBatchUseMultiQuery(List<UniversityRankingsEcharts> list) {
        return baseMapper.updateBatchUseMultiQuery(list);
    }
    
    public int updateBatchSelective(List<UniversityRankingsEcharts> list) {
        return baseMapper.updateBatchSelective(list);
    }
    
    public int batchInsert(List<UniversityRankingsEcharts> list) {
        return baseMapper.batchInsert(list);
    }
    
    public int batchInsertSelectiveUseDefaultForNull(List<UniversityRankingsEcharts> list) {
        return baseMapper.batchInsertSelectiveUseDefaultForNull(list);
    }
    
    public int deleteByPrimaryKeyIn(List<Integer> list) {
        return baseMapper.deleteByPrimaryKeyIn(list);
    }
    
    public boolean insertOrUpdate(UniversityRankingsEcharts record) {
        return baseMapper.insertOrUpdate(record);
    }
    
    public int insertOrUpdateSelective(UniversityRankingsEcharts record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}
