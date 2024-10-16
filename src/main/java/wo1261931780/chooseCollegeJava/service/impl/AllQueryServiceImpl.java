package wo1261931780.chooseCollegeJava.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wo1261931780.chooseCollegeJava.dto.UniversityAllDTO;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQs;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQsCs;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsUsnews;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsUsnewsCs;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsQsMapper;
import wo1261931780.chooseCollegeJava.service.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Intellij IDEA.
 * Project:chooseCollegeJava
 * Package:wo1261931780.chooseCollegeJava.service
 *
 * @author liujiajun_junw
 * @Date 2024-10-17-41  星期三
 * @Description
 */
@Service
public class AllQueryServiceImpl extends ServiceImpl<UniversityRankingsQsMapper, UniversityRankingsQs> implements AllQueryService {


	/**
	 * 默认的查询模块，主要是对数据进行组装操作
	 *
	 * @param universityRankingsQsList     qs数据
	 * @param universityRankingsUsnewsList usnews数据
	 * @return 返回组装好的dto数据
	 */
	@Override
	public List<UniversityAllDTO> incorporationDto(List<UniversityRankingsQs> universityRankingsQsList, List<UniversityRankingsUsnews> universityRankingsUsnewsList) {

		return List.of();
	}

	@Autowired
	private UniversityRankingsQsMapper qsMapper;
	@Autowired
	private UniversityRankingsQsService qsService;
	@Autowired
	private UniversityRankingsQsCsService qsCsService;
	@Autowired
	private UniversityRankingsUsnewsService UsnewsService;
	@Autowired
	private UniversityRankingsUsnewsCsService UsnewsCsService;


	@Override
	public Page<UniversityAllDTO> queryUniversityRank(
			Integer page, Integer limit,
			String rankVariant,
			String universityTagsState, String universityTags,
			Integer currentRank) {
		List<UniversityAllDTO> dtoList = new ArrayList<>();

		// 创建分页对象
		Page<UniversityAllDTO> dtoPage = new Page<>(page, limit);

		// 构建查询条件
		QueryWrapper<?> queryWrapper = new QueryWrapper<>();
		if (universityTagsState != null && !universityTagsState.isEmpty()) {
			queryWrapper.eq("university_tags_state", universityTagsState);
		}
		if (universityTags != null && !universityTags.isEmpty()) {
			queryWrapper.eq("university_tags", universityTags);
		}
		if (currentRank != null) {
			queryWrapper.le("current_rank_integer", currentRank);
		}

		Page<UniversityRankingsQs> qsPage = qsMapper.selectPage(new Page<>(page, limit),
				(QueryWrapper<UniversityRankingsQs>) queryWrapper);
		qsPage.getRecords().forEach(qs -> {
			UniversityAllDTO dto = new UniversityAllDTO();
			BeanUtils.copyProperties(qs, dto);
			// UniversityRankingsUsnews usnews = new UniversityRankingsUsnews();
			// BeanUtils.copyProperties(qs, usnews);
			// 丢进去批量处理
			UniversityAllDTO universityAllDTO = queryDiffObject(dto.getUniversityNameChinese(),dto.getRankingYear());

			// 设置qs全球排名
			dto.setCurrentQsAllRank(qs.getCurrentRankInteger());
			// 设置qs计算机排名
			dto.setCurrentQsComputerRank(universityAllDTO.getCurrentQsComputerRank());
			// 设置usnews全球排名
			dto.setCurrentUsnewsAllRank(universityAllDTO.getCurrentUsnewsAllRank());
			// 设置usnews计算机排名
			dto.setCurrentUsnewsComputerRank(universityAllDTO.getCurrentUsnewsComputerRank());
			// 根据业务需求设置其他字段
			dtoList.add(dto);
		});
		dtoPage.setRecords(dtoList);
		dtoPage.setTotal(qsPage.getTotal());


		return dtoPage;
	}

	/**
	 * 执行三次查询，笨方法
	 *
	 * @param qs     qs对象
	 * @param usnews usnews对象
	 * @return dto结果
	 */
	UniversityAllDTO queryDiffObject(String universityNameChinese, String rankingYear) {
		LambdaQueryWrapper<UniversityRankingsQsCs> queryWrapperqscs = new LambdaQueryWrapper<>();
		queryWrapperqscs.eq(UniversityRankingsQsCs::getUniversityNameChinese, universityNameChinese);
		queryWrapperqscs.eq(UniversityRankingsQsCs::getRankingYear, rankingYear);
		List<UniversityRankingsQsCs> list1 = qsCsService.list(queryWrapperqscs);
		UniversityRankingsQsCs qsCsResult = list1.size() > 1 ? list1.get(0) : qsCsService.getOne(queryWrapperqscs);

		LambdaQueryWrapper<UniversityRankingsUsnews> queryWrapperusnews = new LambdaQueryWrapper<>();
		queryWrapperusnews.eq(UniversityRankingsUsnews::getUniversityNameChinese, universityNameChinese);
		queryWrapperusnews.eq(UniversityRankingsUsnews::getRankingYear, rankingYear);
		List<UniversityRankingsUsnews> list2 = UsnewsService.list(queryWrapperusnews);
		UniversityRankingsUsnews usnewsResult = list2.size() > 1 ? list2.get(0) : UsnewsService.getOne(queryWrapperusnews);

		LambdaQueryWrapper<UniversityRankingsUsnewsCs> queryWrapperusnewscs = new LambdaQueryWrapper<>();
		queryWrapperusnewscs.eq(UniversityRankingsUsnewsCs::getUniversityNameChinese, universityNameChinese);
		queryWrapperusnewscs.eq(UniversityRankingsUsnewsCs::getRankingYear, rankingYear);
		List<UniversityRankingsUsnewsCs> list3 = UsnewsCsService.list(queryWrapperusnewscs);
		UniversityRankingsUsnewsCs usnewsResult2 = list3.size() > 1 ? list3.get(0) : UsnewsCsService.getOne(queryWrapperusnewscs);

		UniversityAllDTO universityAllDTO = new UniversityAllDTO();
		universityAllDTO.setCurrentQsComputerRank(qsCsResult == null ? 0 : qsCsResult.getCurrentRankInteger());
		universityAllDTO.setCurrentUsnewsAllRank(usnewsResult == null ? 0 : usnewsResult.getCurrentRankInteger());
		universityAllDTO.setCurrentUsnewsComputerRank(usnewsResult2 == null ? 0 : usnewsResult2.getCurrentRankInteger());
		return universityAllDTO;
	}
}
