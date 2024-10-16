package wo1261931780.chooseCollegeJava.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import wo1261931780.chooseCollegeJava.dto.UniversityAllDTO;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQs;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsUsnews;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsQsMapper;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsUsnewsMapper;
import wo1261931780.chooseCollegeJava.service.AllQueryService;
import wo1261931780.chooseCollegeJava.service.UniversityRankingsQsService;
import wo1261931780.chooseCollegeJava.service.UniversityRankingsUsnewsService;

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
	private UniversityRankingsUsnewsService UsnewsService;


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

		// 查询 QS 表
		Page<UniversityRankingsQs> qsPage = qsMapper.selectPage(new Page<>(page, limit),
				(QueryWrapper<UniversityRankingsQs>) queryWrapper);
		qsPage.getRecords().forEach(qs -> {
			UniversityAllDTO dto = new UniversityAllDTO();
			BeanUtils.copyProperties(qs, dto);
			dto.setRankingYear(qs.getRankingYear().toString().substring(0, 4));
			dto.setCurrentQsAllRank(qs.getCurrentRankInteger());
			UniversityRankingsUsnews usnews = new UniversityRankingsUsnews();
			BeanUtils.copyProperties(qs, usnews);
			usnews.setRankingYear(qs.getRankingYear().toString().substring(0, 4));
			UniversityAllDTO universityAllDTO = queryDiffObject(qs, usnews);
			dto.setCurrentQsComputerRank(universityAllDTO.getCurrentQsComputerRank());
			dto.setCurrentUsnewsAllRank(universityAllDTO.getCurrentUsnewsAllRank());
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
	UniversityAllDTO queryDiffObject(UniversityRankingsQs qs, UniversityRankingsUsnews usnews) {
		LambdaQueryWrapper<UniversityRankingsQs> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(UniversityRankingsQs::getUniversityNameChinese, qs.getUniversityNameChinese());
		queryWrapper.eq(UniversityRankingsQs::getRankingYear, qs.getRankingYear());
		queryWrapper.eq(UniversityRankingsQs::getRankingCategory, "计算机科学");
		queryWrapper.eq(UniversityRankingsQs::getRankVariant, "QS");
		List<UniversityRankingsQs> list = qsService.list(queryWrapper);
		UniversityRankingsQs qsResult = list.size() > 1 ? list.get(0) : qsService.getOne(queryWrapper);

		LambdaQueryWrapper<UniversityRankingsUsnews> usnewsQueryWrapper = new LambdaQueryWrapper<>();
		usnewsQueryWrapper.eq(UniversityRankingsUsnews::getUniversityNameChinese, usnews.getUniversityNameChinese());
		usnewsQueryWrapper.eq(UniversityRankingsUsnews::getRankingYear, usnews.getRankingYear());
		usnewsQueryWrapper.eq(UniversityRankingsUsnews::getRankingCategory, "计算机科学");
		usnewsQueryWrapper.eq(UniversityRankingsUsnews::getRankVariant, "USNews");
		List<UniversityRankingsUsnews> list1 = UsnewsService.list(usnewsQueryWrapper);
		log.warn("list1 size > 1" + list1);
		UniversityRankingsUsnews usnewsResult = list1.size() > 1 ? list1.get(0) : UsnewsService.getOne(usnewsQueryWrapper);

		LambdaQueryWrapper<UniversityRankingsUsnews> usnewsQueryWrapper2 = new LambdaQueryWrapper<>();
		usnewsQueryWrapper2.eq(UniversityRankingsUsnews::getUniversityNameChinese, usnews.getUniversityNameChinese());
		usnewsQueryWrapper2.eq(UniversityRankingsUsnews::getRankingYear, usnews.getRankingYear());
		usnewsQueryWrapper2.eq(UniversityRankingsUsnews::getRankingCategory, "USNEWS世界大学排名");
		usnewsQueryWrapper2.eq(UniversityRankingsUsnews::getRankVariant, "USNews");
		List<UniversityRankingsUsnews> list2 = UsnewsService.list(usnewsQueryWrapper2);
		log.warn("list2 size > 1" + list2);
		UniversityRankingsUsnews usnewsResult2 = list2.size() > 1 ? list2.get(1) : UsnewsService.getOne(usnewsQueryWrapper);

		UniversityAllDTO universityAllDTO = new UniversityAllDTO();
		universityAllDTO.setCurrentQsComputerRank(qsResult == null ? 0 : qsResult.getCurrentRankInteger());
		universityAllDTO.setCurrentUsnewsAllRank(usnewsResult == null ? 0 : usnewsResult.getCurrentRankInteger());
		universityAllDTO.setCurrentUsnewsComputerRank(usnewsResult2 == null ? 0 : usnewsResult2.getCurrentRankInteger());
		return universityAllDTO;
	}
}
