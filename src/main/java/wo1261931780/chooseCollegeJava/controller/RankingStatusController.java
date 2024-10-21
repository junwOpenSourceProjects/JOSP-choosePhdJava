package wo1261931780.chooseCollegeJava.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import wo1261931780.chooseCollegeJava.config.ShowResult;
import wo1261931780.chooseCollegeJava.dto.RankingStatusDTO;
import wo1261931780.chooseCollegeJava.entity.UniversityConsider;
import wo1261931780.chooseCollegeJava.service.impl.QueryOrUpdateAllSchoolsServiceImpl;

import java.util.ArrayList;
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
	public List<RankingStatusDTO> queryOrUpdateAllSchools() {
		return schoolsService.queryRankingStatus();
	}

	@PostMapping("/insertOrUpdate")
	public ShowResult<String> updateRankingStatus(@RequestBody UniversityConsider universityConsiderList) {
		return schoolsService.insertOrUpdate(universityConsiderList) ? ShowResult.sendSuccess("成功") : ShowResult.sendError("失败");
	}
	@PostMapping("/insertBatch")
	public ShowResult<String> insertBatch(@RequestBody List<String> nameList) {
		return schoolsService.insertBatch(nameList) ? ShowResult.sendSuccess("鎴愬姛") : ShowResult.sendError("澶辫触");
	}
	@GetMapping("/tasks")
	public List<String> getTasks() {
		ArrayList<String> arrayList = new ArrayList<>();
		arrayList.add("task1");
		arrayList.add("task2");
		return arrayList;
	}
}
