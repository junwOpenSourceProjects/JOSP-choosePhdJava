package wo1261931780.chooseCollegeJava.service.impl;

import org.springframework.stereotype.Service;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQs;
import wo1261931780.chooseCollegeJava.service.orderService;

import java.util.List;

/**
 * Created by Intellij IDEA.
 * Project:chooseCollegeJava
 * Package:wo1261931780.chooseCollegeJava.service
 *
 * @author liujiajun_junw
 * @Date 2024-10-18-07  星期三
 * @Description
 */
@Service
public class orderServiceImpl implements orderService {
	@Override
	public List<UniversityRankingsQs> orderByRank(List<UniversityRankingsQs> universityRankingsQsList) {
		return List.of();
	}
}
