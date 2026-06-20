package wo1261931780.chooseCollegeJava.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.List;
import wo1261931780.chooseCollegeJava.entity.ChoosePhd;
import wo1261931780.chooseCollegeJava.mapper.ChoosePhdMapper;
/**
 * ChoosePhd业务接口
 */
@Service
public class ChoosePhdService extends ServiceImpl<ChoosePhdMapper, ChoosePhd> {
    /**
     * 批量更新
     */

    
    public int updateBatch(List<ChoosePhd> list) {
        return baseMapper.updateBatch(list);
    }
    /**
     * 批量更新（多查询方式）
     */
    
    public int updateBatchUseMultiQuery(List<ChoosePhd> list) {
        return baseMapper.updateBatchUseMultiQuery(list);
    }
    /**
     * 批量选择性更新
     */
    
    public int updateBatchSelective(List<ChoosePhd> list) {
        return baseMapper.updateBatchSelective(list);
    }
    /**
     * 批量插入
     */
    
    public int batchInsert(List<ChoosePhd> list) {
        return baseMapper.batchInsert(list);
    }
    /**
     * 批量选择性插入（NULL 使用默认值）
     */
    
    public int batchInsertSelectiveUseDefaultForNull(List<ChoosePhd> list) {
        return baseMapper.batchInsertSelectiveUseDefaultForNull(list);
    }
    /**
     * 按主键集合批量删除
     */
    
    public int deleteByPrimaryKeyIn(List<Long> list) {
        return baseMapper.deleteByPrimaryKeyIn(list);
    }
    /**
     * 新增或更新意向学校信息
     */
    
    public boolean insertOrUpdate(ChoosePhd record) {
        return baseMapper.insertOrUpdate(record);
    }
    /**
     * 选择性插入或更新
     */
    
    public int insertOrUpdateSelective(ChoosePhd record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}