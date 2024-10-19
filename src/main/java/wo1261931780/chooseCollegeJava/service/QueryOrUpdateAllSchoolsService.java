package wo1261931780.chooseCollegeJava.service;

import wo1261931780.chooseCollegeJava.dto.RankingStatusDTO;
import wo1261931780.chooseCollegeJava.entity.UniversityConsider;

import java.util.List;

/**
 * Created by Intellij IDEA.
 * Project:chooseCollegeJava
 * Package:wo1261931780.chooseCollegeJava.service
 *
 * @author liujiajun_junw
 * @Date 2024-10-13-44  星期六
 * @Description
 */
public interface QueryOrUpdateAllSchoolsService {
	List<RankingStatusDTO> queryRankingStatus();

	Boolean insertOrUpdate(UniversityConsider universityConsiderList);
}
