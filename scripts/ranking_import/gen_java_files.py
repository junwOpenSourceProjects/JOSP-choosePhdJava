#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
一次性生成 7 张新表的 Entity + Mapper(java) + Mapper(xml) + Service
跟现有 UniversityRankingsQs/QsMapper/QSCsMapper 模板严格一致
"""
import os

ENTITY_DIR = '/Users/junw/Documents/GitHub/JOSP-choosePhdJava/src/main/java/wo1261931780/chooseCollegeJava/entity'
MAPPER_DIR = '/Users/junw/Documents/GitHub/JOSP-choosePhdJava/src/main/java/wo1261931780/chooseCollegeJava/mapper'
SERVICE_DIR = '/Users/junw/Documents/GitHub/JOSP-choosePhdJava/src/main/java/wo1261931780/chooseCollegeJava/service/impl'
XML_DIR = '/Users/junw/Documents/GitHub/JOSP-choosePhdJava/src/main/resources/wo1261931780/chooseCollegeJava/mapper'

TABLES = [
    ('ArwuSubject', 'arwu_subject', '大学ARWU学科排名'),
    ('EdurankRegion', 'edurank_region', '大学EduRank地区排名'),
    ('DecliningTrend', 'declining_trend', '大学排名下降趋势'),
    ('MosiurWorld', 'mosiur_world', '大学MOSIUR全球排名'),
    ('RurWorld', 'rur_world', '大学RUR全球学术排名'),
    ('UsnewsSubject', 'usnews_subject', '大学USNews学科排名'),
    ('QsSustainability', 'qs_sustainability', '大学QS可持续排名'),
]


def gen_entity(class_name: str, table_suffix: str, comment: str) -> str:
    return f'''package wo1261931780.chooseCollegeJava.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {comment}
 * @author junw
 */
@Schema(description="{comment}")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "computer_rank.university_rankings_{table_suffix}")
public class UniversityRankings{class_name} implements Serializable {{
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Integer id;

    /**
     * 大学名称（中文）
     */
    @TableField(value = "university_name_chinese")
    @Schema(description = "大学名称（中文）")
    private String universityNameChinese;

    /**
     * 大学名称（英文）
     */
    @TableField(value = "university_name_english")
    @Schema(description = "大学名称（英文）")
    private String universityNameEnglish;

    /**
     * 大学标签（例如：国家）
     */
    @TableField(value = "university_tags")
    @Schema(description = "大学标签（例如：国家）")
    private String universityTags;

    /**
     * 大学标签（例如：洲）
     */
    @TableField(value = "university_tags_state")
    @Schema(description = "大学标签（例如：洲）")
    private String universityTagsState;

    /**
     * 排名类别
     */
    @TableField(value = "ranking_category")
    @Schema(description = "排名类别")
    private String rankingCategory;

    /**
     * 排名年份
     */
    @TableField(value = "ranking_year")
    @Schema(description = "排名年份")
    private String rankingYear;

    /**
     * 当前排名（整数）
     */
    @TableField(value = "current_rank_integer")
    @Schema(description = "当前排名（整数）")
    private Integer currentRankInteger;

    /**
     * 当前排名（原始数据，例如"#1"）
     */
    @TableField(value = "current_rank_raw")
    @Schema(description = "当前排名（原始数据，例如\"#1\"）")
    private String currentRankRaw;

    /**
     * rank_variant slug
     */
    @TableField(value = "rank_variant")
    @Schema(description = "rank_variant slug")
    private String rankVariant;

    private static final long serialVersionUID = 1L;
}}
'''


def gen_mapper_java(class_name: str) -> str:
    return f'''package wo1261931780.chooseCollegeJava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import wo1261931780.chooseCollegeJava.entity.UniversityRankings{class_name};

/**
 * UniversityRankings{class_name}数据访问层
 */
@Mapper
public interface UniversityRankings{class_name}Mapper extends BaseMapper<UniversityRankings{class_name}> {{
    /**
     * 批量更新
     */
    int updateBatch(@Param("list") List<UniversityRankings{class_name}> list);
    int updateBatchUseMultiQuery(@Param("list") List<UniversityRankings{class_name}> list);
    int updateBatchSelective(@Param("list") List<UniversityRankings{class_name}> list);
    int batchInsert(@Param("list") List<UniversityRankings{class_name}> list);
    int batchInsertSelectiveUseDefaultForNull(@Param("list") List<UniversityRankings{class_name}> list);
    int deleteByPrimaryKeyIn(List<Integer> list);
    boolean insertOrUpdate(UniversityRankings{class_name} record);
    int insertOrUpdateSelective(UniversityRankings{class_name} record);

    /**
     * 分页 + 多条件查询 (跟 university_rankings_qs 一致)
     */
    java.util.List<UniversityRankings{class_name}> queryByCondition(
        @Param("universityNameChinese") String universityNameChinese,
        @Param("universityTags") String universityTags,
        @Param("universityTagsState") String universityTagsState,
        @Param("rankingCategory") String rankingCategory,
        @Param("rankingYear") String rankingYear,
        @Param("currentRankLimit") Integer currentRankLimit,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );

    int countByCondition(
        @Param("universityNameChinese") String universityNameChinese,
        @Param("universityTags") String universityTags,
        @Param("universityTagsState") String universityTagsState,
        @Param("rankingCategory") String rankingCategory,
        @Param("rankingYear") String rankingYear,
        @Param("currentRankLimit") Integer currentRankLimit
    );

    /**
     * 列出所有 distinct ranking_year (按年倒序, 供前端 filter)
     */
    java.util.List<String> selectDistinctYears();
}}
'''


def gen_mapper_xml(class_name: str, table_suffix: str) -> str:
    return f'''<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="wo1261931780.chooseCollegeJava.mapper.UniversityRankings{class_name}Mapper">
  <resultMap id="BaseResultMap" type="wo1261931780.chooseCollegeJava.entity.UniversityRankings{class_name}">
    <id column="id" jdbcType="INTEGER" property="id" />
    <result column="university_name_chinese" jdbcType="VARCHAR" property="universityNameChinese" />
    <result column="university_name_english" jdbcType="VARCHAR" property="universityNameEnglish" />
    <result column="university_tags" jdbcType="VARCHAR" property="universityTags" />
    <result column="university_tags_state" jdbcType="VARCHAR" property="universityTagsState" />
    <result column="ranking_category" jdbcType="VARCHAR" property="rankingCategory" />
    <result column="ranking_year" jdbcType="OTHER" property="rankingYear" />
    <result column="current_rank_integer" jdbcType="INTEGER" property="currentRankInteger" />
    <result column="current_rank_raw" jdbcType="VARCHAR" property="currentRankRaw" />
    <result column="rank_variant" jdbcType="VARCHAR" property="rankVariant" />
  </resultMap>
  <sql id="Base_Column_List">
    id, university_name_chinese, university_name_english, university_tags, university_tags_state,
    ranking_category, ranking_year, current_rank_integer, current_rank_raw, rank_variant
  </sql>
  <insert id="batchInsert" keyColumn="id" keyProperty="id" parameterType="map" useGeneratedKeys="true">
    insert into computer_rank.university_rankings_{table_suffix}
    (university_name_chinese, university_name_english, university_tags, university_tags_state,
      ranking_category, ranking_year, current_rank_integer, current_rank_raw, rank_variant)
    values
    <foreach collection="list" item="item" separator=",">
      (#{{item.universityNameChinese,jdbcType=VARCHAR}}, #{{item.universityNameEnglish,jdbcType=VARCHAR}},
        #{{item.universityTags,jdbcType=VARCHAR}}, #{{item.universityTagsState,jdbcType=VARCHAR}},
        #{{item.rankingCategory,jdbcType=VARCHAR}}, #{{item.rankingYear,jdbcType=OTHER}}, #{{item.currentRankInteger,jdbcType=INTEGER}},
        #{{item.currentRankRaw,jdbcType=VARCHAR}}, #{{item.rankVariant,jdbcType=VARCHAR}})
    </foreach>
  </insert>
  <delete id="deleteByPrimaryKeyIn">
    delete from computer_rank.university_rankings_{table_suffix} where id in
    <foreach close=")" collection="list" item="id" open="(" separator=",">
      #{{id,jdbcType=INTEGER}}
    </foreach>
  </delete>
  <select id="selectDistinctYears" resultType="java.lang.String">
    SELECT DISTINCT ranking_year FROM computer_rank.university_rankings_{table_suffix}
    WHERE ranking_year IS NOT NULL AND ranking_year != ''
    ORDER BY ranking_year DESC
  </select>
  <select id="queryByCondition" resultMap="BaseResultMap">
    SELECT * FROM computer_rank.university_rankings_{table_suffix}
    <where>
      <if test="universityNameChinese != null and universityNameChinese != ''">
        AND university_name_chinese LIKE CONCAT('%', #{{universityNameChinese}}, '%')
      </if>
      <if test="universityTags != null and universityTags != ''">
        AND university_tags = #{{universityTags}}
      </if>
      <if test="universityTagsState != null and universityTagsState != ''">
        AND university_tags_state = #{{universityTagsState}}
      </if>
      <if test="rankingCategory != null and rankingCategory != ''">
        AND ranking_category = #{{rankingCategory}}
      </if>
      <if test="rankingYear != null and rankingYear != ''">
        AND ranking_year = #{{rankingYear}}
      </if>
      <if test="currentRankLimit != null and currentRankLimit > 0">
        AND current_rank_integer IS NOT NULL AND current_rank_integer &lt;= #{{currentRankLimit}}
      </if>
    </where>
    ORDER BY ranking_year DESC, current_rank_integer ASC, id ASC
    LIMIT #{{offset}}, #{{limit}}
  </select>
  <select id="countByCondition" resultType="int">
    SELECT COUNT(*) FROM computer_rank.university_rankings_{table_suffix}
    <where>
      <if test="universityNameChinese != null and universityNameChinese != ''">
        AND university_name_chinese LIKE CONCAT('%', #{{universityNameChinese}}, '%')
      </if>
      <if test="universityTags != null and universityTags != ''">
        AND university_tags = #{{universityTags}}
      </if>
      <if test="universityTagsState != null and universityTagsState != ''">
        AND university_tags_state = #{{universityTagsState}}
      </if>
      <if test="rankingCategory != null and rankingCategory != ''">
        AND ranking_category = #{{rankingCategory}}
      </if>
      <if test="rankingYear != null and rankingYear != ''">
        AND ranking_year = #{{rankingYear}}
      </if>
      <if test="currentRankLimit != null and currentRankLimit > 0">
        AND current_rank_integer IS NOT NULL AND current_rank_integer &lt;= #{{currentRankLimit}}
      </if>
    </where>
  </select>
</mapper>
'''


def gen_service(class_name: str) -> str:
    return f'''package wo1261931780.chooseCollegeJava.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import wo1261931780.chooseCollegeJava.entity.UniversityRankings{class_name};
import wo1261931780.chooseCollegeJava.mapper.UniversityRankings{class_name}Mapper;

/**
 * UniversityRankings{class_name}业务接口
 */
@Service
public class UniversityRankings{class_name}Service
        extends ServiceImpl<UniversityRankings{class_name}Mapper, UniversityRankings{class_name}> {{
}}
'''


def main():
    for class_name, table_suffix, comment in TABLES:
        # 1. Entity
        entity_path = os.path.join(ENTITY_DIR, f'UniversityRankings{class_name}.java')
        with open(entity_path, 'w', encoding='utf-8') as f:
            f.write(gen_entity(class_name, table_suffix, comment))
        print(f'  [entity] {entity_path}')

        # 2. Mapper java
        mapper_path = os.path.join(MAPPER_DIR, f'UniversityRankings{class_name}Mapper.java')
        with open(mapper_path, 'w', encoding='utf-8') as f:
            f.write(gen_mapper_java(class_name))
        print(f'  [mapper] {mapper_path}')

        # 3. Mapper xml
        xml_path = os.path.join(XML_DIR, f'UniversityRankings{class_name}Mapper.xml')
        with open(xml_path, 'w', encoding='utf-8') as f:
            f.write(gen_mapper_xml(class_name, table_suffix))
        print(f'  [xml]    {xml_path}')

        # 4. Service
        service_path = os.path.join(SERVICE_DIR, f'UniversityRankings{class_name}Service.java')
        with open(service_path, 'w', encoding='utf-8') as f:
            f.write(gen_service(class_name))
        print(f'  [svc]    {service_path}')

    print(f'\nGenerated {len(TABLES)} entities + {len(TABLES)} mapper-java + {len(TABLES)} xml + {len(TABLES)} service = {len(TABLES)*4} files')


if __name__ == '__main__':
    main()