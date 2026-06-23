package josp.choosphd.controller;

import josp.choosphd.api.trend.dto.TrendRowDTO;
import josp.choosphd.common.ApiResult;
import josp.choosphd.mapper.DictMapper;
import josp.choosphd.mapper.RankingTrendMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trend")
public class TrendController {

    private final RankingTrendMapper trendMapper;
    private final DictMapper dictMapper;

    public TrendController(RankingTrendMapper trendMapper, DictMapper dictMapper) {
        this.trendMapper = trendMapper;
        this.dictMapper = dictMapper;
    }

    @GetMapping
    public ApiResult<List<TrendRowDTO>> list(
            @RequestParam String source,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String direction,
            @RequestParam(defaultValue = "100") Integer limit) {

        Integer sourceId = dictMapper.findSourceIdByCode(source.toUpperCase(java.util.Locale.ROOT));
        if (sourceId == null) return ApiResult.ok(List.of());

        String trendType = null;
        if (direction != null) {
            String d = direction.toUpperCase(java.util.Locale.ROOT);
            if (d.equals("UP") || d.equals("GROWING")) trendType = "GROWING";
            else if (d.equals("DOWN") || d.equals("DECLINING")) trendType = "DECLINING";
            else if (d.equals("STABLE")) trendType = "STABLE";
        }

        return ApiResult.ok(trendMapper.listBySourceAndType(sourceId, trendType, limit));
    }
}
