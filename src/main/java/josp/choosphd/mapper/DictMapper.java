package josp.choosphd.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 字典查询 mapper(region / subject / ranking_source)
 * 直接用 Map 接收,简单
 */
@Mapper
public interface DictMapper {

    @Select("SELECT id, code, name_en AS nameEn, name_zh AS nameZh FROM region WHERE deleted = 0 ORDER BY name_zh, name_en")
    List<Map<String, Object>> regions();

    @Select("SELECT id, code, name_en AS nameEn, name_zh AS nameZh FROM subject WHERE deleted = 0 ORDER BY name_zh, name_en")
    List<Map<String, Object>> subjects();

    @Select("""
        SELECT id, code, name_en AS nameEn, name_zh AS nameZh, organization,
               methodology_url AS methodologyUrl, scale, has_trend AS hasTrend,
               has_score AS hasScore, active
        FROM ranking_source
        WHERE deleted = 0
        ORDER BY name_zh, name_en
    """)
    List<Map<String, Object>> sources();

    @Select("SELECT id FROM region WHERE code = #{code} AND deleted = 0 LIMIT 1")
    Integer findRegionIdByCode(String code);

    @Select("SELECT id FROM subject WHERE code = #{code} AND deleted = 0 LIMIT 1")
    Integer findSubjectIdByCode(String code);

    @Select("SELECT id FROM ranking_source WHERE code = #{code} AND deleted = 0 LIMIT 1")
    Integer findSourceIdByCode(String code);

    @Select("SELECT id FROM ranking_source WHERE code = #{code} AND deleted = 0 LIMIT 1")
    Integer findSourceIdByCodeRaw(String code);
}
