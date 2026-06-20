package wo1261931780.chooseCollegeJava.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wo1261931780.chooseCollegeJava.config.RequireRole;
import wo1261931780.chooseCollegeJava.config.RoleConstants;
import wo1261931780.chooseCollegeJava.config.ShowResult;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsAll;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQs;
import wo1261931780.chooseCollegeJava.service.impl.AllQueryServiceImpl;
import wo1261931780.chooseCollegeJava.service.impl.UniversityRankingsAllService;
import wo1261931780.chooseCollegeJava.service.impl.UniversityRankingsQsService;

import java.util.List;

/**
 * 数据初始化控制器
 * <p>
 * 用于从已有的 QS 排名数据生成汇总表和 ECharts 表，方便首次部署或数据重置。
 */
@Slf4j
@RestController
@RequestMapping("/init")
public class DataInitController {

	@Autowired
	private UniversityRankingsQsService qsService;

	@Autowired
	private UniversityRankingsAllService allService;

	@Autowired
	private AllQueryServiceImpl allQueryService;

	/**
	 * 从 QS 数据初始化 university_rankings_all 和 university_rankings_echarts
	 *
	 * @return 初始化结果
	 */
	@PostMapping("/fromQs")
	@RequireRole(RoleConstants.ROLE_ADMIN)
	public ShowResult<String> initFromQs() {
		log.info("开始从 QS 数据初始化汇总表...");
		List<UniversityRankingsQs> qsList = qsService.list();
		if (qsList.isEmpty()) {
			return ShowResult.sendError("QS 表无数据，请先导入 QS 排名数据");
		}

		// 清空汇总表
		allService.remove(null);

		// 将 QS 数据转换为汇总表数据（其他排名维度暂为空）
		List<UniversityRankingsAll> allList = qsList.stream().map(qs -> {
			UniversityRankingsAll all = new UniversityRankingsAll();
			all.setUniversityNameChinese(qs.getUniversityNameChinese());
			all.setUniversityNameEnglish(qs.getUniversityNameEnglish());
			all.setUniversityTags(qs.getUniversityTags());
			all.setUniversityTagsState(qs.getUniversityTagsState());
			all.setRankingYear(qs.getRankingYear());
			all.setCurrentRankIntegerQs(qs.getCurrentRankInteger());
			all.setCurrentRankIntegerQsCs(null);
			all.setCurrentRankIntegerUsnews(null);
			all.setCurrentRankIntegerUsnewsCs(null);
			return all;
		}).toList();

		boolean saved = allService.saveBatch(allList);
		if (!saved) {
			return ShowResult.sendError("汇总表初始化失败");
		}
		log.info("汇总表初始化完成，共 {} 条", allList.size());

		// 生成 ECharts 数据表
		allQueryService.updateEchartsData();
		log.info("ECharts 数据表初始化完成");

		return ShowResult.sendSuccess("初始化完成：汇总表 " + allList.size() + " 条，ECharts 表已生成");
	}
}
