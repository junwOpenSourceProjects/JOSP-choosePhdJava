package josp.choosphd.mapper;

import josp.choosphd.domain.university.UniversityPO;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UniversityMapper {

    @Select("""
        SELECT id, url_id AS urlId, name_en AS nameEn, name_zh AS nameZh,
               logo_url AS logoUrl, region_id AS regionId, country_code AS countryCode,
               city, website, founded_year AS foundedYear, type, aliases,
               created_at AS createdAt, updated_at AS updatedAt, deleted
        FROM university
        WHERE url_id = #{urlId} LIMIT 1
    """)
    UniversityPO findByUrlId(String urlId);

    @Select("SELECT id FROM university WHERE url_id = #{urlId} LIMIT 1")
    Long findIdByUrlId(String urlId);

    @Select("SELECT url_id FROM university WHERE name_en = #{nameEn} LIMIT 1")
    String findUrlIdByName(String nameEn);

    @Select("SELECT COUNT(*) FROM university WHERE deleted = 0")
    long countAll();

    @Insert("""
        INSERT INTO university (url_id, name_en, name_zh, logo_url, region_id, country_code,
                                city, website, founded_year, type, aliases, created_at, updated_at)
        VALUES (#{urlId}, #{nameEn}, #{nameZh}, #{logoUrl}, #{regionId}, #{countryCode},
                #{city}, #{website}, #{foundedYear}, #{type}, #{aliases}, #{createdAt}, #{updatedAt})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UniversityPO u);

    @Update("""
        UPDATE university SET
            name_en = #{nameEn}, name_zh = #{nameZh}, logo_url = #{logoUrl},
            region_id = #{regionId}, country_code = #{countryCode},
            city = #{city}, website = #{website}, type = #{type},
            updated_at = #{updatedAt}
        WHERE id = #{id}
    """)
    int update(UniversityPO u);
}
