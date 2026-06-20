package wo1261931780.chooseCollegeJava.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import wo1261931780.chooseCollegeJava.dto.EchartsDTO;
import wo1261931780.chooseCollegeJava.dto.RankingStatusDTO;
import wo1261931780.chooseCollegeJava.entity.ChartData;
import wo1261931780.chooseCollegeJava.entity.UniversityConsider;

import java.util.List;

/**
 * QueryOrUpdateAllSchools业务接口
 */
public interface QueryOrUpdateAllSchoolsService {
	/**
	 * 查询意向学校状态排名
	 */
	List<RankingStatusDTO> queryRankingStatus();
	/**
	 * 新增或更新意向学校信息
	 */

	Boolean insertOrUpdate(UniversityConsider universityConsiderList);
	/**
	 * 批量新增意向学校
	 */

	boolean insertBatch(List<String> nameList);
	/**
	 * 查询单个学校的抽屉图表数据
	 */

	EchartsDTO drawerData(String name1) throws JsonProcessingException;
}