package wo1261931780.chooseCollegeJava.service;

import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQs;

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
public interface orderService {
	List<UniversityRankingsQs> orderByRank(List<UniversityRankingsQs> universityRankingsQsList);

}
