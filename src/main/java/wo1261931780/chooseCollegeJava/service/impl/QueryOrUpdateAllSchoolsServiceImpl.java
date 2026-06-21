package wo1261931780.chooseCollegeJava.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wo1261931780.chooseCollegeJava.config.ShowResult;
import wo1261931780.chooseCollegeJava.dto.EchartsDTO;
import wo1261931780.chooseCollegeJava.dto.RankingStatusDTO;
import wo1261931780.chooseCollegeJava.entity.*;
import wo1261931780.chooseCollegeJava.service.QueryOrUpdateAllSchoolsService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * QueryOrUpdateAllSchools业务实现类
 */
@Service
@Slf4j
public class QueryOrUpdateAllSchoolsServiceImpl implements QueryOrUpdateAllSchoolsService {

	@Autowired
	private UniversityRankingsEchartsService echartsService;
	// 需要进行一次组装，那么我就需要echartsService和considerService
	@Autowired
	private UniversityConsiderService considerService;

	private final ObjectMapper objectMapper;

	public QueryOrUpdateAllSchoolsServiceImpl(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}
	/**
	 * 查询意向学校状态排名
	 */

	@Override
	public List<RankingStatusDTO> queryRankingStatus() {
		ArrayList<RankingStatusDTO> rankingStatusDTOS = new ArrayList<>();
		// 因为这里是查询出所有的意向院校，然后根据情况进行意愿的更新或者新增，所以查询所有的以后，直接组装即可。
		// 接下来我需要去consider中查所有存在的记录
		List<UniversityConsider> considerList = considerService.list();
		if (considerList == null && considerList.isEmpty()) {
			log.info("暂无意向院校：{}", considerList);
			return rankingStatusDTOS;
		}
		// 然后再根据consider的记录去echarts表中看所有排名信息
		considerList.forEach(consider -> {
			RankingStatusDTO rankingStatusDTO = new RankingStatusDTO();
			BeanUtils.copyProperties(consider, rankingStatusDTO);
			// 这里需要根据university的id去echarts表中查询排名信息
			LambdaQueryWrapper<UniversityRankingsEcharts> queryWrapper = new LambdaQueryWrapper<>();
			queryWrapper.eq(UniversityRankingsEcharts::getUniversityNameChinese, consider.getUniversityNameChinese()).last("limit 1");
			UniversityRankingsEcharts serviceOne = echartsService.getOne(queryWrapper);
			rankingStatusDTO.setUniversityTags(serviceOne.getUniversityTags());
			rankingStatusDTO.setUniversityTagsState(serviceOne.getUniversityTagsState());
			rankingStatusDTO.setRankingQs(serviceOne.getRankingQs());
			rankingStatusDTO.setRankingQsCs(serviceOne.getRankingQsCs());
			rankingStatusDTO.setRankingUsnews(serviceOne.getRankingUsnews());
			rankingStatusDTO.setRankingUsnewsCs(serviceOne.getRankingUsnewsCs());
			rankingStatusDTOS.add(rankingStatusDTO);
		});
		return rankingStatusDTOS;
	}
	/**
	 * 新增或更新意向学校信息
	 */

	@Override
	public Boolean insertOrUpdate(UniversityConsider universityConsiderList) {
		// if (updated > 0) {
		// 	log.info("更新{}条记录", updated);
		// return String.valueOf("更新成功{}, 共{}条记录".toCharArray(), updated, universityConsiderList.size());
		// } else {
		// log.info("新增{}条记录", universityConsiderList.size());
		// return String.valueOf("新增成功{}, 共{}条记录".toCharArray(), universityConsiderList.size(), universityConsiderList.size());
		// }
		// 用雪花算法生成id
		// universityConsiderList.setId(IdUtil.getSnowflake(1, 1).nextId());
		return considerService.insertOrUpdate(universityConsiderList);
	}
	/**
	 * 批量新增意向学校
	 */

	@Override
	public boolean insertBatch(List<String> nameList) {
		if (nameList == null || nameList.isEmpty()) {
			log.info("nameList is null or empty");
			return false;
		}
		List<UniversityConsider> considerList = new ArrayList<>();
		nameList.forEach(name -> {
			UniversityConsider universityConsider = new UniversityConsider();
			universityConsider.setUniversityNameChinese(name);
			universityConsider.setConsider((byte) 1);
			universityConsider.setStatusQs((byte) 1);
			universityConsider.setStatusQsCs((byte) 1);
			universityConsider.setStatusUsnews((byte) 1);
			universityConsider.setStatusUsnewsCs((byte) 1);
			considerList.add(universityConsider);
		});
		return considerService.saveBatch(considerList);
	}
	/**
	 * 查询单个学校的抽屉图表数据
	 *
	 * <p>返 4 条 series (qs/qs_cs/usnews/usnews_cs 4 维) + 4 个 legendData,
	 * 不再硬编码附加 "亚利桑那州立大学" 对比基线 (旧版调试用, 已 100% 污染前端详情页)</p>
	 */

	@Override
	public EchartsDTO drawerData(String name1) throws JsonProcessingException {

		List<Series> series = new ArrayList<>();
		List<String> strings = new ArrayList<>();

		log.info("drawerData: query single school 4-dim trend, name={}", name1);
		// 先精确匹配, 失败再用 like (兼容前端传简称的场景)
		UniversityRankingsEcharts serviceOne = echartsService.getOne(
				new LambdaQueryWrapper<UniversityRankingsEcharts>().eq(UniversityRankingsEcharts::getUniversityNameChinese, name1));
		if (serviceOne == null) {
			LambdaQueryWrapper<UniversityRankingsEcharts> likeWrapper = new LambdaQueryWrapper<>();
			likeWrapper.like(UniversityRankingsEcharts::getUniversityNameChinese, name1);
			serviceOne = echartsService.getOne(likeWrapper);
		}
		if (serviceOne == null) {
			log.warn("drawerData: 找不到 echarts 大学, name={}", name1);
			return new EchartsDTO();
		}
		// 单校 4 维 series (qs / qs_cs / usnews / usnews_cs)
		series.add(buildSeries(name1, parseJsonList(serviceOne.getRankingQs())));
		strings.add(name1 + "qs");
		series.add(buildSeries(name1, parseJsonList(serviceOne.getRankingQsCs())));
		strings.add(name1 + "QsCs");
		series.add(buildSeries(name1, parseJsonList(serviceOne.getRankingUsnews())));
		strings.add(name1 + "Usnews");
		series.add(buildSeries(name1, parseJsonList(serviceOne.getRankingUsnewsCs())));
		strings.add(name1 + "UsnewsCs");

		EchartsDTO echartsDTO = new EchartsDTO();
		echartsDTO.setChatData(new ChartData(series));
		echartsDTO.setLegendData(strings);
		return echartsDTO;
	}

	private Series buildSeries(String name, List<Double> data) {
		Series s = new Series();
		s.setName(name);
		s.setType("line");
		s.setSmooth(Boolean.TRUE);
		s.setEmphasis(new Emphasis("series"));
		s.setData(data);
		return s;
	}

	/** 解析 echarts 表里的 JSON 数组字段, 失败返空 list */
	private List<Double> parseJsonList(String json) {
		if (json == null || json.isEmpty()) return new ArrayList<>();
		try {
			return objectMapper.readValue(json, new TypeReference<>() {});
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	/**
	 * 列出 echarts 表所有大学
	 */
	@Override
	public List<String> listEchartsUniversities() {
		LambdaQueryWrapper<UniversityRankingsEcharts> wrapper = new LambdaQueryWrapper<>();
		wrapper.select(UniversityRankingsEcharts::getUniversityNameChinese);
		return echartsService.list(wrapper).stream()
				.map(UniversityRankingsEcharts::getUniversityNameChinese)
				.distinct()
				.sorted()
				.toList();
	}
}