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
	 * 从 qs + usnews 两张主表按 (name, year) GROUP BY 聚合, 1 大学 1 年 1 行
	 * - current_rank_integer_qs      ← qs_world 该年 rank
	 * - current_rank_integer_usnews  ← the_world 该年 rank (THE 世界排名)
	 * - qs_cs / usnews_cs            ← 无数据源, NULL
	 *
	 * @return 初始化结果
	 */
	@PostMapping("/fromQs")
	@RequireRole(RoleConstants.ROLE_ADMIN)
	public ShowResult<String> initFromQs() {
		log.info("开始从 QS + US News 数据按 (name, year) 聚合初始化汇总表...");
		List<UniversityRankingsQs> qsList = qsService.list();
		if (qsList.isEmpty()) {
			return ShowResult.sendError("QS 表无数据，请先导入 QS 排名数据");
		}

		// 清空汇总表
		allService.remove(null);

		// 用 SQL 直接 GROUP BY 聚合, 1 大学 1 年 1 行, 4 维度字段 (qs_world + the_world) 子查询填值
		int count = allMapper.aggregateFromRawTables();
		log.info("汇总表初始化完成，共 {} 条 (name, year) 组合", count);

		// 生成 ECharts 数据表 (跨 4 维度聚合, 走 queryAllEchartsData 缓存)
		try {
			allQueryService.updateEchartsData();
			log.info("ECharts 数据表初始化完成");
		} catch (Exception e) {
			log.warn("ECharts 数据表初始化失败 (qs_cs/usnews_cs 维度数据源为空, 可忽略): {}", e.getMessage());
		}

		return ShowResult.sendSuccess("初始化完成：汇总表 " + count + " 条");
	}

	@Autowired
	private wo1261931780.chooseCollegeJava.mapper.UniversityRankingsAllMapper allMapper;
}
