package wo1261931780.chooseCollegeJava.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wo1261931780.chooseCollegeJava.config.ShowResult;
import wo1261931780.chooseCollegeJava.dto.UniversityAllDTO;
import wo1261931780.chooseCollegeJava.entity.ChartData;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsAll;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQs;
import wo1261931780.chooseCollegeJava.service.impl.AllQueryServiceImpl;

import java.util.List;

/**
 * Created by Intellij IDEA.
 * Project:chooseCollegeJava
 * Package:wo1261931780.chooseCollegeJava.controller
 *
 * @author liujiajun_junw
 * @Date 2024-10-17-52  星期三
 * @Description
 */
@Slf4j
@RequestMapping("/query")
@RestController
public class queryAllUniversityController {
	@Autowired
	private AllQueryServiceImpl allQueryService;

	@Autowired
	private AllQueryServiceImpl allQueryServiceImpl;

	@GetMapping("/qs")
	public ShowResult<Page<UniversityRankingsQs>> showQsResult(
			@RequestParam Integer page
			, @RequestParam Integer limit
	) {
		Page<UniversityRankingsQs> qsPage = new Page<>();
		qsPage.setCurrent(page);
		qsPage.setSize(limit);
		LambdaQueryWrapper<UniversityRankingsQs> queryWrapper = new LambdaQueryWrapper<>();
		Page<UniversityRankingsQs> rankingsQsPage = allQueryService.page(qsPage, queryWrapper);
		log.info("{}", rankingsQsPage.getSize());
		return ShowResult.sendSuccess(rankingsQsPage);
	}

	@GetMapping("/queryQs")
	@ApiOperation("查询大学排名数据")
	public Page<UniversityAllDTO> queryUniversityRank(
			@RequestParam Integer page,
			@RequestParam Integer limit,
			@RequestParam(defaultValue = "qs") String rankVariant, // 如 "qs", "usnews", "all"
			@RequestParam(required = false) String universityTagsState,
			@RequestParam(required = false) String universityTags,
			@RequestParam(required = false) Integer currentRank
	) {
		return allQueryServiceImpl.queryUniversityRank(page, limit, rankVariant,
				universityTagsState, universityTags, currentRank);
	}

	@GetMapping("/queryAll")
	@ApiOperation("查询大学汇总排名")
	public Page<UniversityRankingsAll> queryAllUniversityRank(
			@RequestParam Integer page,
			@RequestParam Integer limit,
			@RequestParam(required = false) String universityNameChinese,
			@RequestParam(required = false) String universityTagsState,
			@RequestParam(required = false) String universityTags,
			@RequestParam(required = false, defaultValue = "100") Integer currentRank
	) {
		return allQueryServiceImpl.queryAllData(page, limit,
				universityNameChinese, universityTagsState, universityTags, currentRank);
	}

	@GetMapping("/queryAllEcharts")
	@ApiOperation("查询echarts大学汇总排名")
	public ChartData queryAllUniversityRank(
			@RequestParam(required = false) String universityNameChinese,
			@RequestParam(required = false) String universityTagsState,
			@RequestParam(required = false) String universityTags,
			@RequestParam(required = false, defaultValue = "10") Integer currentRank,
			@RequestParam(required = false) String rankVariant
	) {
		return allQueryServiceImpl.queryAllEchartsData(
				universityNameChinese, universityTagsState, universityTags, currentRank, rankVariant);
	}

	@GetMapping("/updateEchartsData")
	@ApiOperation("更新echarts对象表")
	public ChartData queryAllUniversityRank2(
	) {
		return allQueryServiceImpl.updateEchartsData();
	}
	@GetMapping("/queryPartEcharts")
	@ApiOperation("条件查询echarts大学汇总排名")
	public ChartData queryPartEcharts(
			@RequestParam(required = false) String universityNameChinese,
			@RequestParam(required = false) String universityTagsState,
			@RequestParam(required = false) String universityTags,
			@RequestParam(defaultValue = "qs") String rankVariant
	) throws JsonProcessingException {
		return allQueryServiceImpl.queryPartEcharts(
				universityNameChinese, universityTagsState, universityTags,  rankVariant);
	}

}
