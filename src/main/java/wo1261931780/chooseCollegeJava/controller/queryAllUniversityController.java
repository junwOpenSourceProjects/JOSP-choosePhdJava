package wo1261931780.chooseCollegeJava.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wo1261931780.chooseCollegeJava.config.ShowResult;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQs;
import wo1261931780.chooseCollegeJava.service.AllQueryService;

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
@RequestMapping("/queryAll")
@RestController
public class queryAllUniversityController {
	@Autowired
	private AllQueryService allQueryService;

	@GetMapping("/qs")
	public ShowResult<Page<UniversityRankingsQs>> showQsResult(
			@RequestParam Integer page
			, @RequestParam Integer limit
	) {
		Page<UniversityRankingsQs> qsPage = new Page<>();
		qsPage.setCurrent(page);
		qsPage.setSize(limit);
		LambdaQueryWrapper<UniversityRankingsQs> queryWrapper = new LambdaQueryWrapper<>();
		// List<UniversityRankingsQs> list = allQueryService.list(qsPage, queryWrapper);
		Page<UniversityRankingsQs> rankingsQsPage = allQueryService.page(qsPage, queryWrapper);
		log.info("{}", rankingsQsPage.getSize());
		return ShowResult.sendSuccess(rankingsQsPage);
	}

}
