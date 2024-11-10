package wo1261931780.chooseCollegeJava.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wo1261931780.chooseCollegeJava.entity.ChoosePhd;
import wo1261931780.chooseCollegeJava.entity.UniversityConsider;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsEcharts;
import wo1261931780.chooseCollegeJava.mapper.ChoosePhdMapper;
import wo1261931780.chooseCollegeJava.service.ChoosePhdService;
import wo1261931780.chooseCollegeJava.service.impl.UniversityConsiderService;
import wo1261931780.chooseCollegeJava.service.impl.UniversityRankingsEchartsService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Intellij IDEA.
 * Project:JOSP-choosePhdJava
 * Package:wo1261931780.chooseCollegeJava.controller
 *
 * @author liujiajun_junw
 * @Date 2024-11-16-55  星期日
 * @Description
 */
@Slf4j
@RequestMapping("/insertChoosePhd")
@RestController
public class InsertChoosePhdController {
	@Autowired
	private ChoosePhdService choosePhdService;
	@Autowired
	private UniversityConsiderService universityConsiderService;
	@Autowired
	private UniversityRankingsEchartsService universityRankingsEchartsService;


	@RequestMapping("/insert")
	public Integer insertChoosePhd() {
		LambdaQueryWrapper<UniversityConsider> lambdaQueryWrapper = new LambdaQueryWrapper<>();
		lambdaQueryWrapper.orderByDesc(UniversityConsider::getUniversityNameChinese);
		List<UniversityConsider> considerList = universityConsiderService.list(lambdaQueryWrapper);
		log.info("considerList: {}", considerList);
		List<ChoosePhd> choosePhdArrayList = new ArrayList<>();
		considerList.forEach(choosePhd -> {
			ChoosePhd phd = new ChoosePhd();
			String universityName = choosePhd.getUniversityNameChinese();
			phd.setUniversityName(universityName);
			LambdaQueryWrapper<UniversityRankingsEcharts> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
			lambdaQueryWrapper1.eq(UniversityRankingsEcharts::getUniversityNameChinese, universityName);
			UniversityRankingsEcharts serviceOne = universityRankingsEchartsService.getOne(lambdaQueryWrapper1);
			phd.setCountryRegion(serviceOne.getUniversityTags());
			choosePhdArrayList.add(phd);
		});
		return choosePhdService.batchInsert(choosePhdArrayList);
	}
}
