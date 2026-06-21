-- =============================================================================
-- JOSP-choosePhd · 7 张新榜单表 DDL
-- =============================================================================
-- 来源: /Users/junw/Desktop/ranking_data 备份 2/ (相对"备份 1" 新增的榜单类型)
-- 设计原则: 跟现有 university_rankings_qs/usnews 同 schema, 加 rank_variant 区分榜单类型
-- 安全: CREATE TABLE IF NOT EXISTS, 跑多次不报错
-- 关联: src/main/resources/schema.sql 同步追加这 7 段
-- =============================================================================

USE computer_rank;

-- ----------------------------
-- ARWU 学科排名 (11 学科 × 9 年 ≈ 9000 行)
-- ----------------------------
CREATE TABLE IF NOT EXISTS university_rankings_arwu_subject
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（中文）',
    university_name_english VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（英文）',
    university_tags         VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（例如：国家）',
    university_tags_state   VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（例如：洲）',
    ranking_category        VARCHAR(100)          DEFAULT NULL COMMENT '排名类别（学科名，例如：航空航天工程）',
    ranking_year            VARCHAR(20)           DEFAULT NULL COMMENT '排名年份',
    current_rank_integer    INT                   DEFAULT NULL COMMENT '当前排名（整数）',
    current_rank_raw        VARCHAR(50)           DEFAULT NULL COMMENT '当前排名（原始数据，例如"#1"）',
    rank_variant            VARCHAR(100)          DEFAULT NULL COMMENT 'rank_variant slug（arwu_subject 固定值）',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='ARWU 大学学科排名';

ALTER TABLE university_rankings_arwu_subject ADD INDEX idx_arwu_subject_name_year (university_name_chinese, ranking_year);
ALTER TABLE university_rankings_arwu_subject ADD INDEX idx_arwu_subject_category (ranking_category);
ALTER TABLE university_rankings_arwu_subject ADD INDEX idx_arwu_subject_variant (rank_variant);

-- ----------------------------
-- EduRank 6 个地区排名 (6 region × 多年 ≈ 3000 行)
-- ----------------------------
CREATE TABLE IF NOT EXISTS university_rankings_edurank_region
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（中文）',
    university_name_english VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（英文）',
    university_tags         VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（国家）',
    university_tags_state   VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（地区）',
    ranking_category        VARCHAR(100)          DEFAULT NULL COMMENT '排名类别（地区名，例如：亚洲）',
    ranking_year            VARCHAR(20)           DEFAULT NULL COMMENT '排名年份',
    current_rank_integer    INT                   DEFAULT NULL COMMENT '当前排名（整数）',
    current_rank_raw        VARCHAR(50)           DEFAULT NULL COMMENT '当前排名（原始数据，例如"#1"）',
    rank_variant            VARCHAR(100)          DEFAULT NULL COMMENT 'rank_variant slug（edurank_region 固定值）',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='EduRank 大学地区排名';

ALTER TABLE university_rankings_edurank_region ADD INDEX idx_edurank_region_name_year (university_name_chinese, ranking_year);
ALTER TABLE university_rankings_edurank_region ADD INDEX idx_edurank_region_category (ranking_category);

-- ----------------------------
-- Declining Trend 下降趋势 (6 source × 多年 ≈ 40000 行)
-- ----------------------------
CREATE TABLE IF NOT EXISTS university_rankings_declining_trend
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（中文）',
    university_name_english VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（英文）',
    university_tags         VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（国家）',
    university_tags_state   VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（地区）',
    ranking_category        VARCHAR(100)          DEFAULT NULL COMMENT '排名类别（原榜单 source，例如 qs）',
    ranking_year            VARCHAR(20)           DEFAULT NULL COMMENT '排名年份',
    current_rank_integer    INT                   DEFAULT NULL COMMENT '下降幅度（整数，正数表示下降名次）',
    current_rank_raw        VARCHAR(200)          DEFAULT NULL COMMENT '原始数据（HTML 标签 + 数字，例如"<i class=...> 300"）',
    rank_variant            VARCHAR(100)          DEFAULT NULL COMMENT 'rank_variant slug',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='大学排名下降趋势';

ALTER TABLE university_rankings_declining_trend ADD INDEX idx_declining_name_year (university_name_chinese, ranking_year);
ALTER TABLE university_rankings_declining_trend ADD INDEX idx_declining_category (ranking_category);

-- ----------------------------
-- MOSIUR World 全球排名 (≈ 2400 行)
-- ----------------------------
CREATE TABLE IF NOT EXISTS university_rankings_mosiur_world
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（中文）',
    university_name_english VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（英文）',
    university_tags         VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（国家）',
    university_tags_state   VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（地区）',
    ranking_category        VARCHAR(100)          DEFAULT NULL COMMENT '排名类别（mosiur_world 固定值）',
    ranking_year            VARCHAR(20)           DEFAULT NULL COMMENT '排名年份',
    current_rank_integer    INT                   DEFAULT NULL COMMENT '当前排名（整数）',
    current_rank_raw        VARCHAR(50)           DEFAULT NULL COMMENT '当前排名（原始数据，例如"#1"）',
    rank_variant            VARCHAR(100)          DEFAULT NULL COMMENT 'rank_variant slug（mosiur_world 固定值）',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='MOSIUR 大学全球排名';

ALTER TABLE university_rankings_mosiur_world ADD INDEX idx_mosiur_world_name_year (university_name_chinese, ranking_year);

-- ----------------------------
-- RUR World 全球学术排名 (≈ 28000 行)
-- ----------------------------
CREATE TABLE IF NOT EXISTS university_rankings_rur_world
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（中文）',
    university_name_english VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（英文）',
    university_tags         VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（国家）',
    university_tags_state   VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（地区）',
    ranking_category        VARCHAR(100)          DEFAULT NULL COMMENT '排名类别（rur_world 固定值）',
    ranking_year            VARCHAR(20)           DEFAULT NULL COMMENT '排名年份',
    current_rank_integer    INT                   DEFAULT NULL COMMENT '当前排名（整数）',
    current_rank_raw        VARCHAR(50)           DEFAULT NULL COMMENT '当前排名（原始数据，例如"#1"）',
    rank_variant            VARCHAR(100)          DEFAULT NULL COMMENT 'rank_variant slug（rur_world 固定值）',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='RUR 大学全球学术排名';

ALTER TABLE university_rankings_rur_world ADD INDEX idx_rur_world_name_year (university_name_chinese, ranking_year);

-- ----------------------------
-- US News 学科排名 (51 学科 × 多 年 ≈ 50000 行)
-- ----------------------------
CREATE TABLE IF NOT EXISTS university_rankings_usnews_subject
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（中文）',
    university_name_english VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（英文）',
    university_tags         VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（国家）',
    university_tags_state   VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（地区）',
    ranking_category        VARCHAR(100)          DEFAULT NULL COMMENT '排名类别（学科名，例如：生物化学）',
    ranking_year            VARCHAR(20)           DEFAULT NULL COMMENT '排名年份',
    current_rank_integer    INT                   DEFAULT NULL COMMENT '当前排名（整数）',
    current_rank_raw        VARCHAR(50)           DEFAULT NULL COMMENT '当前排名（原始数据，例如"#1"）',
    rank_variant            VARCHAR(100)          DEFAULT NULL COMMENT 'rank_variant slug（usnews_subject 固定值）',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='US News 大学学科排名';

ALTER TABLE university_rankings_usnews_subject ADD INDEX idx_usnews_subject_name_year (university_name_chinese, ranking_year);
ALTER TABLE university_rankings_usnews_subject ADD INDEX idx_usnews_subject_category (ranking_category);

-- ----------------------------
-- QS 可持续大学排名 (3 年 ≈ 1500 行)
-- ----------------------------
CREATE TABLE IF NOT EXISTS university_rankings_qs_sustainability
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（中文）',
    university_name_english VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（英文）',
    university_tags         VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（国家）',
    university_tags_state   VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（地区）',
    ranking_category        VARCHAR(100)          DEFAULT NULL COMMENT '排名类别（qs_sustainability 固定值）',
    ranking_year            VARCHAR(20)           DEFAULT NULL COMMENT '排名年份',
    current_rank_integer    INT                   DEFAULT NULL COMMENT '当前排名（整数）',
    current_rank_raw        VARCHAR(50)           DEFAULT NULL COMMENT '当前排名（原始数据，例如"#1"）',
    rank_variant            VARCHAR(100)          DEFAULT NULL COMMENT 'rank_variant slug（qs_sustainability 固定值）',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='QS 大学可持续排名';

ALTER TABLE university_rankings_qs_sustainability ADD INDEX idx_qs_sustainability_name_year (university_name_chinese, ranking_year);