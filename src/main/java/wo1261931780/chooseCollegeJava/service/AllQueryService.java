package wo1261931780.chooseCollegeJava.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.bind.annotation.RequestParam;
import wo1261931780.chooseCollegeJava.dto.UniversityAllDTO;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQs;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsUsnews;

import java.util.List;

/**
 * Created by Intellij IDEA.
 * Project:chooseCollegeJava
 * Package:wo1261931780.chooseCollegeJava.service
 *
 * @author liujiajun_junw
 * @Date 2024-10-18-07  星期三
 * @Description
 */
public interface AllQueryService extends IService<UniversityRankingsQs> {
	/**
	 * 默认的查询模块，主要是对数据进行组装操作
	 * 业务逻辑是这样的，前端执行查询，传入：
	 * 当前页Integer page，页面数据量Integer limit，排名类型rankVariant，大洲universityTagsState，
	 * 国家universityTags，前xx名currentRankInteger等参数，
	 * 我需要根据参数去执行查询逻辑，并将其组装为一个dto数据，然后返回给前端。
	 *
	 * @param universityRankingsQsList     qs数据
	 * @param universityRankingsUsnewsList usnews数据
	 * @return 返回组装好的dto数据
	 */
	List<UniversityAllDTO> incorporationDto(List<UniversityRankingsQs> universityRankingsQsList,
	                                        List<UniversityRankingsUsnews> universityRankingsUsnewsList);

	Page<UniversityAllDTO> queryUniversityRank(
			Integer page,
			Integer limit,
			String rankVariant,
			String universityTagsState,
			String universityTags,
			Integer currentRank
	);
}
