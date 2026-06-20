package wo1261931780.chooseCollegeJava.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsUsnewsCsMapper;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsUsnewsCs;
/**
 * UniversityRankingsUsnewsCs业务接口
 */
@Service
public class UniversityRankingsUsnewsCsService extends ServiceImpl<UniversityRankingsUsnewsCsMapper, UniversityRankingsUsnewsCs> {
    /**
     * 批量更新
     */

    
    public int updateBatch(List<UniversityRankingsUsnewsCs> list) {
        return baseMapper.updateBatch(list);
    }
    /**
     * 批量更新（多查询方式）
     */
    
    public int updateBatchUseMultiQuery(List<UniversityRankingsUsnewsCs> list) {
        return baseMapper.updateBatchUseMultiQuery(list);
    }
    /**
     * 批量选择性更新
     */
    
    public int updateBatchSelective(List<UniversityRankingsUsnewsCs> list) {
        return baseMapper.updateBatchSelective(list);
    }
    /**
     * 批量插入
     */
    
    public int batchInsert(List<UniversityRankingsUsnewsCs> list) {
        return baseMapper.batchInsert(list);
    }
    /**
     * 批量选择性插入（NULL 使用默认值）
     */
    
    public int batchInsertSelectiveUseDefaultForNull(List<UniversityRankingsUsnewsCs> list) {
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
    
    public boolean insertOrUpdate(UniversityRankingsUsnewsCs record) {
        return baseMapper.insertOrUpdate(record);
    }
    /**
     * 选择性插入或更新
     */
    
    public int insertOrUpdateSelective(UniversityRankingsUsnewsCs record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}