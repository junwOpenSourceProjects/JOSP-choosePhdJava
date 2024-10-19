package wo1261931780.chooseCollegeJava.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wo1261931780.chooseCollegeJava.config.ShowResult;
import wo1261931780.chooseCollegeJava.dto.RankingStatusDTO;
import wo1261931780.chooseCollegeJava.entity.UniversityConsider;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsEcharts;
import wo1261931780.chooseCollegeJava.service.QueryOrUpdateAllSchoolsService;

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


	@Override
	public List<RankingStatusDTO> queryRankingStatus() {
		ArrayList<RankingStatusDTO> rankingStatusDTOS = new ArrayList<>();
		// 因为这里是查询出所有的意向院校，然后根据情况进行意愿的更新或者新增，所以查询所有的以后，直接组装即可。
		// 接下来我需要去consider中查所有存在的记录
		List<UniversityConsider> considerList = considerService.list();
		if (considerList != null && !considerList.isEmpty()) {
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
}
