package wo1261931780.chooseCollegeJava.service.impl;

import org.springframework.stereotype.Service;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQs;
import wo1261931780.chooseCollegeJava.service.OrderService;

import java.util.List;

/**
 * Order业务实现类
 */
@Service
public class OrderServiceImpl implements OrderService {
	/**
	 * 按排名排序
	 */
	@Override
	public List<UniversityRankingsQs> orderByRank(List<UniversityRankingsQs> universityRankingsQsList) {
		return List.of();
	}
}