package wo1261931780.chooseCollegeJava.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wo1261931780.chooseCollegeJava.config.RequireRole;
import wo1261931780.chooseCollegeJava.config.RoleConstants;
import wo1261931780.chooseCollegeJava.config.ShowResult;
import wo1261931780.chooseCollegeJava.dto.EchartsDTO;
import wo1261931780.chooseCollegeJava.dto.UniversityAllDTO;
import wo1261931780.chooseCollegeJava.entity.ChartData;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsAll;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQs;
import wo1261931780.chooseCollegeJava.service.impl.AllQueryServiceImpl;

import java.util.List;

/**
 * 大学排名查询控制器
 */
@Slf4j
@Validated
@RequestMapping("/query")
@RestController
public class QueryAllUniversityController {
	@Autowired
	private AllQueryServiceImpl allQueryService;

	/**
	 * 查询 QS 排名列表
	 *
	 * @param page  页码
	 * @param limit 每页条数
	 * @return QS 排名分页结果
	 */
	@GetMapping("/qs")
	public ShowResult<Page<UniversityRankingsQs>> showQsResult(
			@RequestParam @Min(1) Integer page,
			@RequestParam @Min(1) @Max(500) Integer limit
	) {
		Page<UniversityRankingsQs> qsPage = new Page<>();
		qsPage.setCurrent(page);
		qsPage.setSize(limit);
		LambdaQueryWrapper<UniversityRankingsQs> queryWrapper = new LambdaQueryWrapper<>();
		Page<UniversityRankingsQs> rankingsQsPage = allQueryService.page(qsPage, queryWrapper);
		log.info("{}", rankingsQsPage.getSize());
		return ShowResult.sendSuccess(rankingsQsPage);
	}

	/**
	 * 分页查询大学排名
	 *
	 * @param page               页码
	 * @param limit              每页条数
	 * @param rankVariant        排名类型（qs/usnews/all）
	 * @param universityTagsState 洲
	 * @param universityTags     国家
	 * @param currentRank        当前排名上限
	 * @return 大学排名分页结果
	 */
	@GetMapping("/queryQs")
	@Operation(summary = "查询大学排名数据")
	public Page<UniversityAllDTO> queryUniversityRank(
			@RequestParam @Min(1) Integer page,
			@RequestParam @Min(1) @Max(500) Integer limit,
			@RequestParam(defaultValue = "qs") String rankVariant,
			@RequestParam(required = false) String universityTagsState,
			@RequestParam(required = false) String universityTags,
			@RequestParam(required = false) @Min(1) Integer currentRank,
			@RequestParam(required = false) String universityNameChinese
	) {
		return allQueryService.queryUniversityRank(page, limit, rankVariant,
				universityTagsState, universityTags, currentRank, universityNameChinese);
	}

	/**
	 * 分页查询大学汇总排名
	 *
	 * @param page                页码
	 * @param limit               每页条数
	 * @param universityNameChinese 学校中文名
	 * @param universityTagsState  洲
	 * @param universityTags      国家
	 * @param currentRank         当前排名上限
	 * @return 大学汇总排名分页结果
	 */
	@GetMapping("/queryAll")
	@Operation(summary = "查询大学汇总排名")
	public Page<UniversityRankingsAll> queryAllUniversityRank(
			@RequestParam @Min(1) Integer page,
			@RequestParam @Min(1) @Max(500) Integer limit,
			@RequestParam(required = false) String universityNameChinese,
			@RequestParam(required = false) String universityTagsState,
			@RequestParam(required = false) String universityTags,
			@RequestParam(required = false, defaultValue = "100") @Min(1) Integer currentRank
	) {
		log.info("universityNameChinese:{}, universityTagsState:{}, universityTags:{}, currentRank:{}",
				universityNameChinese, universityTagsState, universityTags, currentRank);
		return allQueryService.queryAllData(page, limit,
				universityNameChinese, universityTagsState, universityTags, currentRank);
	}

	/**
	 * 查询 ECharts 排名数据
	 *
	 * @param universityNameChinese 学校中文名
	 * @param universityTagsState   洲
	 * @param universityTags        国家
	 * @param currentRank           当前排名上限
	 * @param rankVariant           排名类型
	 * @return ECharts 图表数据（包装为 EchartsDTO，与 queryPartEcharts 保持一致）
	 */
	@GetMapping("/queryAllEcharts")
	@Operation(summary = "查询echarts大学汇总排名")
	public EchartsDTO queryAllUniversityRank(
			@RequestParam(required = false) String universityNameChinese,
			@RequestParam(required = false) String universityTagsState,
			@RequestParam(required = false) String universityTags,
			@RequestParam(required = false, defaultValue = "10") Integer currentRank,
			@RequestParam(required = false) String rankVariant
	) {
		ChartData chartData = allQueryService.queryAllEchartsData(
				universityNameChinese, universityTagsState, universityTags, currentRank, rankVariant);
		EchartsDTO dto = new EchartsDTO();
		dto.setChatData(chartData);
		dto.setLegendData(extractYears(universityNameChinese, universityTagsState, universityTags));
		return dto;
	}

	/**
	 * 从汇总表中提取去重排序后的年份作为 X 轴
	 */
	private List<String> extractYears(String universityNameChinese, String universityTagsState, String universityTags) {
		return allQueryService.queryAllData(1, 1000, universityNameChinese, universityTagsState, universityTags, null)
				.getRecords().stream()
				.map(UniversityRankingsAll::getRankingYear)
				.map(String::valueOf)
				.distinct()
				.sorted()
				.toList();
	}

	/**
	 * 更新 ECharts 数据表并返回图表数据
	 *
	 * @return ECharts 图表数据
	 */
	@GetMapping("/updateEchartsData")
	@Operation(summary = "更新echarts对象表")
	@RequireRole(RoleConstants.ROLE_ADMIN)
	public ChartData queryAllUniversityRank2() {
		return allQueryService.updateEchartsData();
	}

	/**
	 * 按条件查询部分 ECharts 数据
	 *
	 * @param universityNameChinese 学校中文名
	 * @param universityTagsState   洲
	 * @param universityTags        国家
	 * @param rankVariant           排名类型
	 * @return ECharts 数据
	 * @throws JsonProcessingException JSON 处理异常
	 */
	@GetMapping("/queryPartEcharts")
	@Operation(summary = "条件查询echarts大学汇总排名")
	public EchartsDTO queryPartEcharts(
			@RequestParam(required = false) String universityNameChinese,
			@RequestParam(required = false) String universityTagsState,
			@RequestParam(required = false) String universityTags,
			@RequestParam(defaultValue = "qs") String rankVariant
	) throws JsonProcessingException {
		return allQueryService.queryPartEcharts2(
				universityNameChinese, universityTagsState, universityTags, rankVariant);
	}
}