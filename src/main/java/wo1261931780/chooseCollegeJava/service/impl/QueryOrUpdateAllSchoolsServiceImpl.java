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
 * Created by Intellij IDEA.
 * Project:chooseCollegeJava
 * Package:wo1261931780.chooseCollegeJava.service.impl
 *
 * @author liujiajun_junw
 * @Date 2024-10-13-44  星期六
 * @Description
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

	@Override
	public EchartsDTO drawerData(String name1) throws JsonProcessingException {

		List<Series> series = new ArrayList<>();
		List<String> strings = new ArrayList<>();

		while (series.size() < 8) {
			Series series1 = new Series();
			series1.setName(name1);
			series1.setType("line");
			series1.setSmooth(Boolean.TRUE);
			series1.setEmphasis(new Emphasis("series"));
			series.add(series1);
			strings.add(name1);
		}
		log.info("series size:{}", series.size());
		LambdaQueryWrapper<UniversityRankingsEcharts> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.like(UniversityRankingsEcharts::getUniversityNameChinese, name1);
		UniversityRankingsEcharts serviceOne = echartsService.getOne(queryWrapper);
		UniversityRankingsEcharts serviceTwo = echartsService.getOne(new LambdaQueryWrapper<UniversityRankingsEcharts>().eq(UniversityRankingsEcharts::getUniversityNameChinese, "亚利桑那州立大学"));
		for (int i = 0; i < series.size(); i++) {
			if (i == 0) {
				series.get(i).setData(objectMapper.readValue(serviceOne.getRankingQs(), new TypeReference<>() {
				}));
				strings.set(i, name1 + "qs");
			} else if (i == 1) {
				series.get(i).setData(objectMapper.readValue(serviceOne.getRankingQsCs(), new TypeReference<>() {
				}));
				strings.set(i, name1 + "QsCs");
			} else if (i == 2) {
				series.get(i).setData(objectMapper.readValue(serviceOne.getRankingUsnews(), new TypeReference<>() {
				}));
				strings.set(i, name1 + "Usnews");
			} else if (i == 3) {
				series.get(i).setData(objectMapper.readValue(serviceOne.getRankingUsnewsCs(), new TypeReference<>() {
				}));
				strings.set(i, name1 + "UsnewsCs");
			} else if (i == 4) {
				series.get(i).setName("亚利桑那州立大学");
				series.get(i).setData(objectMapper.readValue(serviceTwo.getRankingQs(), new TypeReference<>() {
				}));
				strings.set(i, "亚利桑那州立大学qs");
			} else if (i == 5) {
				series.get(i).setName("亚利桑那州立大学");
				series.get(i).setData(objectMapper.readValue(serviceTwo.getRankingQsCs(), new TypeReference<>() {
				}));
				strings.set(i, "亚利桑那州立大学QsCs");
			} else if (i == 6) {
				series.get(i).setName("亚利桑那州立大学");
				series.get(i).setData(objectMapper.readValue(serviceTwo.getRankingUsnews(), new TypeReference<>() {
				}));
				strings.set(i, "亚利桑那州立大学Usnews");
			} else if (i == 7) {
				series.get(i).setName("亚利桑那州立大学");
				series.get(i).setData(objectMapper.readValue(serviceTwo.getRankingUsnewsCs(), new TypeReference<>() {
				}));
				strings.set(i, "亚利桑那州立大学UsnewsCs");
			}
		}
		EchartsDTO echartsDTO = new EchartsDTO();
		echartsDTO.setChatData(new ChartData(series));
		echartsDTO.setLegendData(strings);
		return echartsDTO;
	}
}
