package wo1261931780.chooseCollegeJava.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQs;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsQsMapper;

import java.util.List;

/**
 * Created by Intellij IDEA.
 * Project:chooseCollegeJava
 * Package:wo1261931780.chooseCollegeJava.service
 *
 * @author liujiajun_junw
 * @Date 2024-10-17-41  星期三
 * @Description
 */
@Service
public class AllQueryService extends ServiceImpl<UniversityRankingsQsMapper, UniversityRankingsQs> {
	public int updateBatch(List<UniversityRankingsQs> list) {
		return baseMapper.updateBatch(list);
	}

	public int updateBatchSelective(List<UniversityRankingsQs> list) {
		return baseMapper.updateBatchSelective(list);
	}

	public int batchInsert(List<UniversityRankingsQs> list) {
		return baseMapper.batchInsert(list);
	}

	public boolean insertOrUpdate(UniversityRankingsQs record) {
		return baseMapper.insertOrUpdate(record);
	}

	public int insertOrUpdateSelective(UniversityRankingsQs record) {
		return baseMapper.insertOrUpdateSelective(record);
	}
}
// todo 想办法删掉登录页面
