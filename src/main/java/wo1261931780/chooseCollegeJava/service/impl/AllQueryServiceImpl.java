package wo1261931780.chooseCollegeJava.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wo1261931780.chooseCollegeJava.dto.EchartsDTO;
import wo1261931780.chooseCollegeJava.dto.UniversityAllDTO;
import wo1261931780.chooseCollegeJava.entity.*;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsAllMapper;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsEchartsMapper;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsQsMapper;
import wo1261931780.chooseCollegeJava.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

	@Autowired
	public AllQueryServiceImpl(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

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
	@Autowired
	private UniversityRankingsAllService allService;

	@Autowired
	private UniversityRankingsAllMapper allMapper;
	@Autowired
	private UniversityRankingsEchartsService echartsService;
	@Autowired
	private UniversityRankingsEchartsMapper echartsMapper;
	private final ObjectMapper objectMapper;


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
			UniversityAllDTO universityAllDTO = queryDiffObject(dto.getUniversityNameChinese(), dto.getRankingYear());

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

	@Override
	public Page<UniversityRankingsAll> queryAllData(
			Integer page, Integer limit,
			String universityNameChinese, String universityTagsState, String universityTags, Integer currentRank) {
		Page<UniversityRankingsAll> dtoPage = new Page<>(page, limit);
		// 首先判断是否为空，
		LambdaQueryWrapper<UniversityRankingsAll> lambdaQueryWrapper = new LambdaQueryWrapper<>();
		if (universityNameChinese != null && !universityNameChinese.isEmpty()) {
			lambdaQueryWrapper.eq(UniversityRankingsAll::getUniversityNameChinese, universityNameChinese);
		}
		if (universityTagsState != null && !universityTagsState.isEmpty()) {
			lambdaQueryWrapper.eq(UniversityRankingsAll::getUniversityTagsState, universityTagsState);
		}
		if (universityTags != null && !universityTags.isEmpty()) {
			lambdaQueryWrapper.eq(UniversityRankingsAll::getUniversityTags, universityTags);
		}
		// 默认查询前100名的，默认使用qs排名
		if (currentRank != null) {
			lambdaQueryWrapper.le(UniversityRankingsAll::getCurrentRankIntegerQs, currentRank);
		}
		lambdaQueryWrapper.orderByAsc(UniversityRankingsAll::getUniversityNameChinese)
				.orderByAsc(UniversityRankingsAll::getRankingYear);
		// 执行查询
		// List<UniversityRankingsAll> rankingsAllList = allService.list(lambdaQueryWrapper);
		// dtoPage.setRecords(rankingsAllList);
		// dtoPage.setTotal(rankingsAllList.size());
		return allService.page(dtoPage, lambdaQueryWrapper);
	}

	@Override
	public ChartData queryAllEchartsData(
			String universityNameChinese, String universityTagsState, String universityTags, Integer currentRank, String rankVariant) {
		ChartData chartData = getOneChartData();
		List<Series> seriesList = chartData.getSeries();
		seriesList.forEach(series -> {
			LambdaQueryWrapper<UniversityRankingsAll> lambdaQueryWrapper = new LambdaQueryWrapper<>();
			List<Double> seriesData = series.getData();
			// 遍历所有系列，根据名称去查询qs排名，把数据一个一个对照set进去
			lambdaQueryWrapper.eq(UniversityRankingsAll::getUniversityNameChinese, series.getName());
			// 按照排名年份asc排序
			lambdaQueryWrapper.orderByAsc(UniversityRankingsAll::getRankingYear);
			List<UniversityRankingsAll> oneList = allService.list(lambdaQueryWrapper);// 获取该学校的所有年份数据
			switch (rankVariant.toLowerCase()) {
				// case "qs":
				// 	break;
				case "usnews":
					oneList.forEach(one -> seriesData.add(Double.valueOf(one.getCurrentRankIntegerUsnews())));// 把usnews排名添加进去
					break;
				case "qs_cs":
					oneList.forEach(one -> seriesData.add(Double.valueOf(one.getCurrentRankIntegerQsCs())));// 把qs+cs排名添加进去
					break;
				case "usnews_cs":
					oneList.forEach(one -> seriesData.add(Double.valueOf(one.getCurrentRankIntegerUsnewsCs())));// 把usnews+cs排名添加进去
					break;
				default:
					oneList.forEach(one -> seriesData.add(Double.valueOf(one.getCurrentRankIntegerQs())));// 把qs排名添加进去
			}
		});
		return chartData;
	}

	@Override
	public ChartData updateEchartsData() {
		ChartData chartData = queryAllEchartsData(null, null, null, null, "qs");
		// ChartData chartData = queryAllEchartsData(null, null, null, null, "qs_cs");
		// ChartData chartData = queryAllEchartsData(null, null, null, null, "usnews");
		// ChartData chartData = queryAllEchartsData(null, null, null, null, "usnews_cs");
		chartData.getSeries().forEach(series -> {
			LambdaQueryWrapper<UniversityRankingsEcharts> lambdaQueryWrapper = new LambdaQueryWrapper<>();
			lambdaQueryWrapper.eq(UniversityRankingsEcharts::getUniversityNameChinese, series.getName());
			UniversityRankingsEcharts serviceOne = echartsService.getOne(lambdaQueryWrapper);
			if (serviceOne == null) {
				UniversityRankingsEcharts rankingsEcharts = new UniversityRankingsEcharts();
				rankingsEcharts.setUniversityNameChinese(series.getName());
				rankingsEcharts.setRankingQs(series.getData().toString());
				// rankingsEcharts.setRankingQsCs(series.getData().toString());
				// rankingsEcharts.setRankingUsnews(series.getData().toString());
				// rankingsEcharts.setRankingUsnewsCs(series.getData().toString());
				echartsService.save(rankingsEcharts);
			} else {
				serviceOne.setRankingQs(series.getData().toString());
				// serviceOne.setRankingQsCs(series.getData().toString());
				// serviceOne.setRankingUsnews(series.getData().toString());
				// serviceOne.setRankingUsnewsCs(series.getData().toString());
				echartsService.insertOrUpdate(serviceOne);
			}
		});
		return chartData;
	}

	@Override
	public ChartData queryPartEcharts(String universityNameChinese, String universityTagsState, String universityTags, String rankVariant) throws JsonProcessingException {
		if (rankVariant == null) {
			log.error("rankVariant is null");
			return null;
		}
		ChartData chartData = new ChartData();
		ArrayList<Series> seriesArrayList = new ArrayList<>();
		LambdaQueryWrapper<UniversityRankingsEcharts> lambdaQueryWrapper = new LambdaQueryWrapper<>();

		if (universityNameChinese != null && !universityNameChinese.isEmpty()) {
			lambdaQueryWrapper.eq(UniversityRankingsEcharts::getUniversityNameChinese, universityNameChinese);
			UniversityRankingsEcharts serviceOne = echartsService.getOne(lambdaQueryWrapper);
			Series series = new Series();
			series.setName(universityNameChinese);
			series.setType("line");
			series.setSmooth(Boolean.TRUE);
			series.setEmphasis(new Emphasis("series"));
			List<Double> doubles;
			doubles = getDoubles(rankVariant, serviceOne);
			series.setData(doubles);
			seriesArrayList.add(series);
			chartData.setSeries(seriesArrayList);
			return chartData;
		}
		if (universityTagsState != null && !universityTagsState.isEmpty()) {
			lambdaQueryWrapper.eq(UniversityRankingsEcharts::getUniversityTagsState, universityTagsState);
		}
		if (universityTags != null && !universityTags.isEmpty()) {
			lambdaQueryWrapper.eq(UniversityRankingsEcharts::getUniversityTags, universityTags);
		}

		List<UniversityRankingsEcharts> serviceList = echartsService.list(lambdaQueryWrapper);
		serviceList.forEach(all -> {
			Series series = new Series();
			series.setName(all.getUniversityNameChinese());
			series.setType("line");
			series.setSmooth(Boolean.TRUE);
			series.setEmphasis(new Emphasis("series"));
			List<Double> doubles;
			doubles = getDoubles(rankVariant, all);
			series.setData(doubles);
			seriesArrayList.add(series);
		});
		chartData.setSeries(seriesArrayList);
		return chartData;
	}

	@Override
	public EchartsDTO queryPartEcharts2(String universityNameChinese, String universityTagsState, String universityTags, String rankVariant) throws JsonProcessingException {
		EchartsDTO echartsDTO = new EchartsDTO();
		if (universityTags == null || universityTags.isEmpty()) {
			log.error("universityTagsState is null");
			universityTags = "德国";
		}
		ChartData chartData = queryPartEcharts(universityNameChinese, universityTagsState, universityTags, rankVariant);
		echartsDTO.setChatData(chartData);
		ArrayList<String> strings = new ArrayList<>();
		chartData.getSeries().forEach(series -> strings.add(series.getName()));
		echartsDTO.setLegendData(strings);
		return echartsDTO;
	}

	private List<Double> getDoubles(String rankVariant, UniversityRankingsEcharts all) {
		List<Double> doubles;
		try {
			switch (rankVariant.toLowerCase()) {
				case "usnews":
					doubles = objectMapper.readValue(all.getRankingUsnews(), new TypeReference<>() {
					});
					break;
				case "qs_cs":
					// series.setDataString(all.getRankingQsCs());
					doubles = objectMapper.readValue(all.getRankingQsCs(), new TypeReference<>() {
					});
					break;
				case "usnews_cs":
					// series.setDataString(all.getRankingUsnewsCs());
					doubles = objectMapper.readValue(all.getRankingUsnewsCs(), new TypeReference<>() {
					});
					break;
				default:
					// series.setDataString(all.getRankingQs());
					doubles = objectMapper.readValue(all.getRankingQs(), new TypeReference<>() {
							}
					);
					break;
			}
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		return doubles;
	}

	ChartData getOneChartData() {
		// 设置一个新的方法用来组合数据为echarts格式
		ChartData chartData = new ChartData();
		ArrayList<Series> seriesArrayList = new ArrayList<>();
		// 首先要去重获取所有的UniversityNameChinese，把名称全部set进去，
		// 创建 LambdaQueryWrapper 实例并配置查询条件
		LambdaQueryWrapper<UniversityRankingsAll> wrapper = new LambdaQueryWrapper<>();
		wrapper.select(UniversityRankingsAll::getUniversityNameChinese)
				.groupBy(UniversityRankingsAll::getUniversityNameChinese);

		// 执行查询，获取去重后的 UniversityRankingsAll 对象列表
		List<UniversityRankingsAll> universityRecords = allMapper.selectList(wrapper);

		// 将 UniversityRankingsAll 对象列表转换为仅包含 UniversityNameChinese 的 List<String>
		List<String> universityNameList = universityRecords.stream()
				.map(UniversityRankingsAll::getUniversityNameChinese)
				.toList();
		log.debug(String.valueOf(universityNameList.size()));
		// 然后遍历，根据名称去查询qs排名，把数据一个一个对照set进去
		universityNameList.forEach(all -> {
			Series series = new Series();
			series.setName(all);
			series.setType("line");
			series.setSmooth(Boolean.TRUE);
			series.setEmphasis(new Emphasis("series"));
			ArrayList<Double> doubles = new ArrayList<>();
			series.setData(doubles);
			seriesArrayList.add(series);
		});
		chartData.setSeries(seriesArrayList);
		return chartData;
	}

	/**
	 * 执行三次查询，笨方法
	 *
	 * @param universityNameChinese 中文名
	 * @param rankingYear           年份
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
