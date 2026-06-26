package com.choosephd.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.choosephd.common.BusinessException;
import com.choosephd.common.PageResult;
import com.choosephd.dto.PageQuery;
import com.choosephd.dto.PageUtil;
import com.choosephd.dto.RankingEntryVo;
import com.choosephd.dto.UniversityDetailResponse;
import com.choosephd.dto.UniversitySourceSummary;
import com.choosephd.dto.UniversityTagVo;
import com.choosephd.dto.UniversityVo;
import com.choosephd.entity.University;
import com.choosephd.repository.RankingEntryMapper;
import com.choosephd.repository.UniversityMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 院校数据核心 service — 列表/详情/标签/榜单汇总四大入口。
 *
 * <p>覆盖：
 * <ul>
 *   <li>{@code searchPage} — 院校列表分页（关键字/国家/地区/标签筛选 + 排序）</li>
 *   <li>{@code listCountries} — 不重复的国家列表（用于筛选下拉）</li>
 *   <li>{@code getDetail} — 单院校详情（基本信息 + 标签 + 历年榜单汇总）</li>
 *   <li>{@code attachTags} — 批量查询院校标签（用 {@link UniversityTagService#listTagsByUniversities}）</li>
 * </ul>
 *
 * <p>批量查询：从 N+1（每校一次 selectList）优化为单次 IN 查询，详情页 20 校 → 1 SQL。
 *
 * <p>CONTINENT_COUNTRIES 静态常量：continent (亚洲/欧洲等) → Set&lt;country&gt; 映射，
 * SQL OR u.country IN (...) 用。同时 SQL 还有 OR u.country = #{continent} 兼容脏数据
 * （参考 UniversityMapper.xml 注释）。
 */
@Service
public class UniversityService {

    private static final Map<String, Set<String>> CONTINENT_COUNTRIES = new HashMap<>();

    static {
        Set<String> asia = new HashSet<>(Set.of(
                "中国", "中国台湾", "中国香港", "中国澳门", "蒙古", "朝鲜", "韩国", "日本",
                "越南", "老挝", "柬埔寨", "泰国", "缅甸", "马来西亚", "新加坡", "印度尼西亚",
                "文莱", "菲律宾", "东帝汶", "印度", "巴基斯坦", "孟加拉国", "尼泊尔", "不丹",
                "斯里兰卡", "马尔代夫", "哈萨克斯坦", "吉尔吉斯斯坦", "塔吉克斯坦", "乌兹别克斯坦",
                "土库曼斯坦", "阿富汗", "伊拉克", "伊朗", "叙利亚", "黎巴嫩", "以色列", "巴勒斯坦",
                "约旦", "沙特阿拉伯", "也门", "阿曼", "阿联酋", "卡塔尔", "巴林", "科威特",
                "土耳其", "阿塞拜疆", "格鲁吉亚", "亚美尼亚", "塞浦路斯"
        ));
        Set<String> europe = new HashSet<>(Set.of(
                "英国", "爱尔兰", "法国", "荷兰", "比利时", "卢森堡", "德国", "奥地利", "瑞士",
                "波兰", "捷克", "斯洛伐克", "匈牙利", "罗马尼亚", "保加利亚", "塞尔维亚", "克罗地亚",
                "斯洛文尼亚", "黑山", "北马其顿", "波黑", "阿尔巴尼亚", "希腊", "意大利", "西班牙",
                "葡萄牙", "马耳他", "安道尔", "圣马力诺", "梵蒂冈", "摩纳哥", "列支敦士登",
                "丹麦", "挪威", "瑞典", "芬兰", "冰岛", "爱沙尼亚", "拉脱维亚", "立陶宛",
                "白俄罗斯", "俄罗斯", "乌克兰", "摩尔多瓦"
        ));
        Set<String> northAmerica = new HashSet<>(Set.of(
                "美国", "加拿大", "墨西哥", "危地马拉", "伯利兹", "萨尔瓦多", "洪都拉斯", "尼加拉瓜",
                "哥斯达黎加", "巴拿马", "古巴", "牙买加", "海地", "多米尼加", "巴哈马", "巴巴多斯",
                "特立尼达和多巴哥", "格林纳达", "圣卢西亚", "圣文森特和格林纳丁斯", "安提瓜和巴布达",
                "圣基茨和尼维斯", "多米尼克"
        ));
        Set<String> africa = new HashSet<>(Set.of(
                "埃及", "利比亚", "突尼斯", "阿尔及利亚", "摩洛哥", "苏丹", "南苏丹", "乍得", "尼日尔",
                "马里", "布基纳法索", "毛里塔尼亚", "塞内加尔", "冈比亚", "几内亚", "几内亚比绍",
                "塞拉利昂", "利比里亚", "科特迪瓦", "加纳", "多哥", "贝宁", "尼日利亚", "喀麦隆",
                "中非共和国", "赤道几内亚", "加蓬", "刚果（布）", "刚果（金）", "安哥拉", "赞比亚",
                "马拉维", "莫桑比克", "纳米比亚", "博茨瓦纳", "津巴布韦", "南非", "斯威士兰", "莱索托",
                "马达加斯加", "毛里求斯", "科摩罗", "塞舌尔", "佛得角", "圣多美和普林西比",
                "埃塞俄比亚", "厄立特里亚", "吉布提", "索马里", "肯尼亚", "乌干达", "坦桑尼亚",
                "卢旺达", "布隆迪", "刚果"
        ));
        Set<String> southAmerica = new HashSet<>(Set.of(
                "巴西", "阿根廷", "智利", "乌拉圭", "巴拉圭", "玻利维亚", "秘鲁", "哥伦比亚",
                "委内瑞拉", "厄瓜多尔", "圭亚那", "苏里南", "法属圭亚那"
        ));
        Set<String> oceania = new HashSet<>(Set.of(
                "澳大利亚", "新西兰", "巴布亚新几内亚", "斐济", "所罗门群岛", "萨摩亚", "汤加",
                "瓦努阿图", "基里巴斯", "瑙鲁", "图瓦卢", "帕劳", "密克罗尼西亚", "马绍尔群岛"
        ));

        CONTINENT_COUNTRIES.put("亚洲", asia);
        CONTINENT_COUNTRIES.put("欧洲", europe);
        CONTINENT_COUNTRIES.put("北美洲", northAmerica);
        CONTINENT_COUNTRIES.put("非洲", africa);
        CONTINENT_COUNTRIES.put("南美洲", southAmerica);
        CONTINENT_COUNTRIES.put("大洋洲", oceania);
    }

    private static final Set<Integer> CORE_RANKING_SOURCES = Set.of(4, 11, 16, 19, 46, 54);

    private final UniversityMapper universityMapper;
    private final RankingEntryMapper rankingEntryMapper;
    private final UniversityTagService universityTagService;

    public UniversityService(UniversityMapper universityMapper, RankingEntryMapper rankingEntryMapper,
                             UniversityTagService universityTagService) {
        this.universityMapper = universityMapper;
        this.rankingEntryMapper = rankingEntryMapper;
        this.universityTagService = universityTagService;
    }

    public PageResult<UniversityVo> searchUniversities(String keyword, String continent, String country,
                                                        String sortBy, List<Integer> tagIds, PageQuery query) {
        Set<String> continentCountries = continentCountriesOf(continent);
        IPage<UniversityVo> page = universityMapper.selectSearchPage(
                PageUtil.toPage(query),
                keyword,
                continent,
                continentCountries,
                country,
                sortBy,
                tagIds);
        attachTags(page.getRecords());
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    private void attachTags(List<UniversityVo> universities) {
        if (universities == null || universities.isEmpty()) {
            return;
        }
        List<String> ids = universities.stream()
                .map(University::getUrlId)
                .distinct()
                .toList();
        // 一次 SQL 拿所有大学的标签，Map 按 urlId 分组
        Map<String, List<UniversityTagVo>> tagsByUniversity = universityTagService.listTagsByUniversities(ids);
        for (UniversityVo vo : universities) {
            vo.setTags(tagsByUniversity.getOrDefault(vo.getUrlId(), Collections.emptyList()));
        }
    }

    public List<String> listCountries(String continent) {
        Set<String> continentCountries = continentCountriesOf(continent);
        return universityMapper.selectCountries(continent, continentCountries);
    }

    private Set<String> continentCountriesOf(String continent) {
        if (continent == null || continent.isBlank()) {
            return Collections.emptySet();
        }
        return CONTINENT_COUNTRIES.getOrDefault(continent.trim(), Collections.emptySet());
    }

    public UniversityDetailResponse getUniversityDetail(String urlId) {
        University university = universityMapper.selectById(urlId);
        if (university == null) {
            throw new BusinessException("大学不存在");
        }
        List<UniversitySourceSummary> sources = rankingEntryMapper.selectSourceSummariesByUniversity(urlId);
        UniversityDetailResponse response = new UniversityDetailResponse(university, sources);
        response.setTags(universityTagService.listTagsByUniversity(urlId));
        return response;
    }

    public PageResult<RankingEntryVo> listUniversityRankings(String urlId, Integer sourceId, Integer year,
                                                              Boolean overallOnly, PageQuery query) {
        University university = universityMapper.selectById(urlId);
        if (university == null) {
            throw new BusinessException("大学不存在");
        }
        IPage<RankingEntryVo> result = rankingEntryMapper.selectVoPageByUniversity(
                PageUtil.toPage(query.getPage(), query.getSize(), 5000L), urlId, sourceId, year, overallOnly);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }
}
