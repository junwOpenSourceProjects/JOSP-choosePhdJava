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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
	 * <p>
	 * 幂等：重复调用不会插重复行。先查 university_consider 全部意向学校，
	 * 再用 LinkedHashSet 按 universityName 去重，最后用 choose_phd 已有 university_name 集合
	 * 二次过滤已存在的，剩下才是真要插的。
	 *
	 * @return 实际新增条数
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

		// 1) 同一批次内按 universityName 去重
		Set<String> seenInBatch = new HashSet<>();
		List<ChoosePhd> distinctList = new ArrayList<>();
		for (UniversityConsider consider : considerList) {
			String universityName = consider.getUniversityNameChinese();
			if (universityName == null || universityName.isEmpty()) continue;
			if (!seenInBatch.add(universityName)) continue; // 批次内重复跳过
			ChoosePhd phd = new ChoosePhd();
			phd.setUniversityName(universityName);

			// 同步国家/地区（如果 echarts 表能查到）
			LambdaQueryWrapper<UniversityRankingsEcharts> echartsQuery = new LambdaQueryWrapper<>();
			echartsQuery.eq(UniversityRankingsEcharts::getUniversityNameChinese, universityName);
			UniversityRankingsEcharts echartsOne = universityRankingsEchartsService.getOne(echartsQuery);
			if (echartsOne != null) {
				phd.setCountryRegion(echartsOne.getUniversityTags());
			}
			distinctList.add(phd);
		}

		if (distinctList.isEmpty()) return 0;

		// 2) 跨批次去重：过滤掉 choose_phd 已存在的大学名
		List<String> candidateNames = distinctList.stream()
				.map(ChoosePhd::getUniversityName)
				.toList();
		LambdaQueryWrapper<ChoosePhd> existingQuery = new LambdaQueryWrapper<>();
		existingQuery.in(ChoosePhd::getUniversityName, candidateNames)
				.select(ChoosePhd::getUniversityName);
		Set<String> existingNames = choosePhdService.list(existingQuery).stream()
				.map(ChoosePhd::getUniversityName)
				.collect(Collectors.toSet());

		List<ChoosePhd> toInsert = distinctList.stream()
				.filter(p -> !existingNames.contains(p.getUniversityName()))
				.toList();

		if (toInsert.isEmpty()) {
			log.info("全部 {} 所大学已在 choose_phd 表中,无需重复插入", candidateNames.size());
			return 0;
		}
		log.info("准备插入 {} 所新大学(共 {} 所候选,已存在 {})",
				toInsert.size(), candidateNames.size(), existingNames.size());
		return choosePhdService.batchInsert(toInsert);
	}
}