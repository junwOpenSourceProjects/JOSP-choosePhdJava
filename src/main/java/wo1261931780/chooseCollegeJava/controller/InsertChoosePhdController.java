package wo1261931780.chooseCollegeJava.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wo1261931780.chooseCollegeJava.config.RequireRole;
import wo1261931780.chooseCollegeJava.config.RoleConstants;
import wo1261931780.chooseCollegeJava.entity.ChoosePhd;
import wo1261931780.chooseCollegeJava.entity.UniversityConsider;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsEcharts;
import wo1261931780.chooseCollegeJava.service.ChoosePhdService;
import wo1261931780.chooseCollegeJava.service.impl.UniversityConsiderService;
import wo1261931780.chooseCollegeJava.service.impl.UniversityRankingsEchartsService;

import java.util.ArrayList;
import java.util.List;

/**
 * 选校数据导入控制器
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

	/**
	 * 根据意向学校批量生成选校数据
	 *
	 * @return 生成记录数
	 */
	@RequestMapping("/insert")
	@RequireRole(RoleConstants.ROLE_ADMIN)
	@Transactional(rollbackFor = Exception.class)
	public Integer insertChoosePhd() {
		LambdaQueryWrapper<UniversityConsider> lambdaQueryWrapper = new LambdaQueryWrapper<>();
		lambdaQueryWrapper.orderByDesc(UniversityConsider::getUniversityNameChinese);
		List<UniversityConsider> considerList = universityConsiderService.list(lambdaQueryWrapper);
		if (considerList == null || considerList.isEmpty()) {
			log.info("暂无意向院校");
			return 0;
		}
		log.info("considerList size: {}", considerList.size());
		List<ChoosePhd> choosePhdArrayList = new ArrayList<>();
		considerList.forEach(choosePhd -> {
			ChoosePhd phd = new ChoosePhd();
			String universityName = choosePhd.getUniversityNameChinese();
			phd.setUniversityName(universityName);
			LambdaQueryWrapper<UniversityRankingsEcharts> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
			lambdaQueryWrapper1.eq(UniversityRankingsEcharts::getUniversityNameChinese, universityName);
			UniversityRankingsEcharts serviceOne = universityRankingsEchartsService.getOne(lambdaQueryWrapper1);
			if (serviceOne != null) {
				phd.setCountryRegion(serviceOne.getUniversityTags());
			}
			choosePhdArrayList.add(phd);
		});
		return choosePhdService.batchInsert(choosePhdArrayList);
	}
}