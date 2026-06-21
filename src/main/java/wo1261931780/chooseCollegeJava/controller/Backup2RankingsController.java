package wo1261931780.chooseCollegeJava.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wo1261931780.chooseCollegeJava.config.ShowResult;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsArwuSubject;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsDecliningTrend;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsEdurankRegion;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsMosiurWorld;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQsSustainability;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsRurWorld;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsUsnewsSubject;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsArwuSubjectMapper;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsDecliningTrendMapper;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsEdurankRegionMapper;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsMosiurWorldMapper;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsQsSustainabilityMapper;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsRurWorldMapper;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsUsnewsSubjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "备份 2" 7 张新榜单表的统一查询 controller
 * 设计: rankTable 参数路由 (避免 7 个 endpoint 重复样板)
 * rankTable 取值:
 *   arwu_subject / edurank_region / declining_trend /
 *   mosiur_world / rur_world / usnews_subject / qs_sustainability
 *
 * 注: 此 controller 不引入新 Service, 直接调 mapper.queryByCondition (xml 自定义)
 */
@Slf4j
@Validated
@RequestMapping("/backup2")
@RestController
public class Backup2RankingsController {

    @Autowired private UniversityRankingsArwuSubjectMapper arwuSubjectMapper;
    @Autowired private UniversityRankingsEdurankRegionMapper edurankRegionMapper;
    @Autowired private UniversityRankingsDecliningTrendMapper decliningTrendMapper;
    @Autowired private UniversityRankingsMosiurWorldMapper mosiurWorldMapper;
    @Autowired private UniversityRankingsRurWorldMapper rurWorldMapper;
    @Autowired private UniversityRankingsUsnewsSubjectMapper usnewsSubjectMapper;
    @Autowired private UniversityRankingsQsSustainabilityMapper qsSustainabilityMapper;

    /** 所有支持的 rankTable 名 */
    public static final List<String> SUPPORTED_TABLES = List.of(
        "arwu_subject", "edurank_region", "declining_trend",
        "mosiur_world", "rur_world", "usnews_subject", "qs_sustainability"
    );

    /**
     * 列出支持的 rankTable
     */
    @GetMapping("/listTables")
    @Operation(summary = "列出支持的 rankTable (备份 2 新增的 7 张表)")
    public ShowResult<List<String>> listTables() {
        return ShowResult.sendSuccess(SUPPORTED_TABLES);
    }

    /**
     * 通用分页查询
     */
    @GetMapping("/list")
    @Operation(summary = "查询备份 2 新表数据 (按 rankTable 路由)")
    public ShowResult<Map<String, Object>> list(
            @RequestParam @Min(1) Integer page,
            @RequestParam @Min(1) @Max(500) Integer limit,
            @RequestParam String rankTable,
            @RequestParam(required = false) String universityNameChinese,
            @RequestParam(required = false) String universityTags,
            @RequestParam(required = false) String universityTagsState,
            @RequestParam(required = false) String rankingCategory,
            @RequestParam(required = false) String rankingYear,
            @RequestParam(required = false) Integer currentRankLimit
    ) {
        if (!SUPPORTED_TABLES.contains(rankTable)) {
            return ShowResult.sendSuccess(Map.of(
                "records", List.of(),
                "total", 0,
                "error", "unsupported rankTable: " + rankTable + ", supported: " + SUPPORTED_TABLES
            ));
        }
        int offset = (page - 1) * limit;
        Map<String, Object> result = new HashMap<>();
        result.put("records", queryRecords(rankTable, universityNameChinese, universityTags, universityTagsState,
                                            rankingCategory, rankingYear, currentRankLimit, offset, limit));
        result.put("total", countRecords(rankTable, universityNameChinese, universityTags, universityTagsState,
                                          rankingCategory, rankingYear, currentRankLimit));
        result.put("page", page);
        result.put("limit", limit);
        result.put("rankTable", rankTable);
        return ShowResult.sendSuccess(result);
    }

    /**
     * 按 rankTable 路由查询记录
     */
    private List<?> queryRecords(String rankTable, String name, String tags, String tagsState,
                                  String category, String year, Integer rankLimit, int offset, int limit) {
        return switch (rankTable) {
            case "arwu_subject" -> arwuSubjectMapper.queryByCondition(name, tags, tagsState, category, year, rankLimit, offset, limit);
            case "edurank_region" -> edurankRegionMapper.queryByCondition(name, tags, tagsState, category, year, rankLimit, offset, limit);
            case "declining_trend" -> decliningTrendMapper.queryByCondition(name, tags, tagsState, category, year, rankLimit, offset, limit);
            case "mosiur_world" -> mosiurWorldMapper.queryByCondition(name, tags, tagsState, category, year, rankLimit, offset, limit);
            case "rur_world" -> rurWorldMapper.queryByCondition(name, tags, tagsState, category, year, rankLimit, offset, limit);
            case "usnews_subject" -> usnewsSubjectMapper.queryByCondition(name, tags, tagsState, category, year, rankLimit, offset, limit);
            case "qs_sustainability" -> qsSustainabilityMapper.queryByCondition(name, tags, tagsState, category, year, rankLimit, offset, limit);
            default -> new ArrayList<>();
        };
    }

    private int countRecords(String rankTable, String name, String tags, String tagsState,
                              String category, String year, Integer rankLimit) {
        return switch (rankTable) {
            case "arwu_subject" -> arwuSubjectMapper.countByCondition(name, tags, tagsState, category, year, rankLimit);
            case "edurank_region" -> edurankRegionMapper.countByCondition(name, tags, tagsState, category, year, rankLimit);
            case "declining_trend" -> decliningTrendMapper.countByCondition(name, tags, tagsState, category, year, rankLimit);
            case "mosiur_world" -> mosiurWorldMapper.countByCondition(name, tags, tagsState, category, year, rankLimit);
            case "rur_world" -> rurWorldMapper.countByCondition(name, tags, tagsState, category, year, rankLimit);
            case "usnews_subject" -> usnewsSubjectMapper.countByCondition(name, tags, tagsState, category, year, rankLimit);
            case "qs_sustainability" -> qsSustainabilityMapper.countByCondition(name, tags, tagsState, category, year, rankLimit);
            default -> 0;
        };
    }
}