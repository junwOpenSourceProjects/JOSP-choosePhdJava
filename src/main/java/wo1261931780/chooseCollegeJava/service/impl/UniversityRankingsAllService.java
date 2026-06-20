package wo1261931780.chooseCollegeJava.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsAllMapper;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsAll;
/**
 * UniversityRankingsAll业务接口
 */
@Service
public class UniversityRankingsAllService extends ServiceImpl<UniversityRankingsAllMapper, UniversityRankingsAll> {
    /**
     * 批量更新
     */

    
    public int updateBatch(List<UniversityRankingsAll> list) {
        return baseMapper.updateBatch(list);
    }
    /**
     * 批量更新（多查询方式）
     */
    
    public int updateBatchUseMultiQuery(List<UniversityRankingsAll> list) {
        return baseMapper.updateBatchUseMultiQuery(list);
    }
    /**
     * 批量选择性更新
     */
    
    public int updateBatchSelective(List<UniversityRankingsAll> list) {
        return baseMapper.updateBatchSelective(list);
    }
    /**
     * 批量插入
     */
    
    public int batchInsert(List<UniversityRankingsAll> list) {
        return baseMapper.batchInsert(list);
    }
    /**
     * 批量选择性插入（NULL 使用默认值）
     */
    
    public int batchInsertSelectiveUseDefaultForNull(List<UniversityRankingsAll> list) {
        return baseMapper.batchInsertSelectiveUseDefaultForNull(list);
    }
    /**
     * 按主键集合批量删除
     */
    
    public int deleteByPrimaryKeyIn(List<Integer> list) {
        return baseMapper.deleteByPrimaryKeyIn(list);
    }
    /**
     * 新增或更新意向学校信息
     */
    
    public boolean insertOrUpdate(UniversityRankingsAll record) {
        return baseMapper.insertOrUpdate(record);
    }
    /**
     * 选择性插入或更新
     */
    
    public int insertOrUpdateSelective(UniversityRankingsAll record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}