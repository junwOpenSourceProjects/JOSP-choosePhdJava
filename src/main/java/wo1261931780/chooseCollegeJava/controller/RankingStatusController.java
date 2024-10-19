package wo1261931780.chooseCollegeJava.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import wo1261931780.chooseCollegeJava.config.ShowResult;
import wo1261931780.chooseCollegeJava.dto.RankingStatusDTO;
import wo1261931780.chooseCollegeJava.entity.UniversityConsider;
import wo1261931780.chooseCollegeJava.service.impl.QueryOrUpdateAllSchoolsServiceImpl;

import java.util.List;

/**
 * Created by Intellij IDEA.
 * Project:chooseCollegeJava
 * Package:wo1261931780.chooseCollegeJava.controller
 *
 * @author liujiajun_junw
 * @Date 2024-10-13-35  星期六
 * @Description
 */
@Slf4j
@RequestMapping("/status")
@RestController
public class RankingStatusController {

	@Autowired
	private QueryOrUpdateAllSchoolsServiceImpl schoolsService;


	@GetMapping("/queryRankingStatus")
	public List<RankingStatusDTO> queryOrUpdateAllSchools(	) {
		return schoolsService.queryRankingStatus();
	}
	@PutMapping("/insertOrUpdate")
	public ShowResult<String> updateRankingStatus(@RequestBody List<UniversityConsider> universityConsiderList) {
		return ShowResult.sendSuccess(schoolsService.insertOrUpdate(universityConsiderList));
	}
}
