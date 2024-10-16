package wo1261931780.chooseCollegeJava.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wo1261931780.chooseCollegeJava.config.ShowResult;
import wo1261931780.chooseCollegeJava.dto.UniversityAllDTO;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQs;
import wo1261931780.chooseCollegeJava.service.impl.AllQueryServiceImpl;

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
@RequestMapping("/queryAll")
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

	@GetMapping("/query")
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

}