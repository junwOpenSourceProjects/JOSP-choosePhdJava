package wo1261931780.chooseCollegeJava.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import wo1261931780.chooseCollegeJava.config.RequireRole;
import wo1261931780.chooseCollegeJava.config.RoleConstants;
import wo1261931780.chooseCollegeJava.config.ShowResult;
import wo1261931780.chooseCollegeJava.dto.EchartsDTO;
import wo1261931780.chooseCollegeJava.dto.RankingStatusDTO;
import wo1261931780.chooseCollegeJava.entity.UniversityConsider;
import wo1261931780.chooseCollegeJava.service.impl.QueryOrUpdateAllSchoolsServiceImpl;

import java.util.List;

/**
 * 选校状态控制器
 */
@Slf4j
@Validated
@RequestMapping("/status")
@RestController
public class RankingStatusController {

	@Autowired
	private QueryOrUpdateAllSchoolsServiceImpl schoolsService;

	/**
	 * 查询意向学校状态排名
	 *
	 * @return 状态排名列表
	 */
	@GetMapping("/queryRankingStatus")
	public List<RankingStatusDTO> queryOrUpdateAllSchools() {
		return schoolsService.queryRankingStatus();
	}

	/**
	 * 新增或更新意向学校信息
	 *
	 * @param universityConsiderList 意向学校
	 * @return 操作结果
	 */
	@PostMapping("/insertOrUpdate")
	@RequireRole(RoleConstants.ROLE_ADMIN)
	public ShowResult<String> updateRankingStatus(@Valid @RequestBody UniversityConsider universityConsiderList) {
		return schoolsService.insertOrUpdate(universityConsiderList) ? ShowResult.sendSuccess("成功") : ShowResult.sendError("失败");
	}

	/**
	 * 批量新增意向学校
	 *
	 * @param nameList 学校名称列表
	 * @return 操作结果
	 */
	@PostMapping("/insertBatch")
	@RequireRole(RoleConstants.ROLE_ADMIN)
	public ShowResult<String> insertBatch(@RequestBody @NotEmpty(message = "学校名称列表不能为空") List<String> nameList) {
		return schoolsService.insertBatch(nameList) ? ShowResult.sendSuccess("成功") : ShowResult.sendError("失败");
	}

	/**
	 * 查询单个学校的抽屉图表数据
	 *
	 * @param name 学校名称
	 * @return ECharts 数据
	 * @throws JsonProcessingException JSON 处理异常
	 */
	@PostMapping("/drawerData")
	public EchartsDTO drawerData(@RequestBody String name) throws JsonProcessingException {
		return schoolsService.drawerData(name);
	}
}