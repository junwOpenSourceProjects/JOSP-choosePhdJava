package wo1261931780.chooseCollegeJava.service;

import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQs;

import java.util.List;

/**
 * Order业务接口
 */
public interface OrderService {
	/**
	 * 按排名排序
	 */
	List<UniversityRankingsQs> orderByRank(List<UniversityRankingsQs> universityRankingsQsList);

}