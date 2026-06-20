CREATE DATABASE IF NOT EXISTS computer_rank
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE computer_rank;

-- ----------------------------
-- 登录用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS login_user
(
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(255)         DEFAULT NULL COMMENT '姓名',
    username    VARCHAR(255)         DEFAULT NULL COMMENT '用户名',
    password    VARCHAR(255)         DEFAULT NULL COMMENT '密码',
    phone       VARCHAR(50)          DEFAULT NULL COMMENT '手机号',
    sex         VARCHAR(10)          DEFAULT NULL COMMENT '性别',
    id_number   VARCHAR(50)          DEFAULT NULL COMMENT '身份证号',
    status      INT                  DEFAULT NULL COMMENT '状态 0:禁用，1:正常',
    role        VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER' COMMENT '角色 ROLE_USER/ROLE_ADMIN',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    create_user BIGINT               DEFAULT NULL COMMENT '创建人',
    update_user BIGINT               DEFAULT NULL COMMENT '修改人',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='登录用户表';

-- ----------------------------
-- 院校信息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS choose_phd
(
    id                       BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键，雪花',
    university_name          VARCHAR(255)            DEFAULT NULL COMMENT '大学名称',
    ranking_data             TEXT                    DEFAULT NULL COMMENT '大学排名相关数据，包含名称和链接',
    official_website         VARCHAR(500)            DEFAULT NULL COMMENT '院校官方网站链接',
    recruitment_website      VARCHAR(500)            DEFAULT NULL COMMENT '社招网站链接，可为空',
    priority                 INT                     DEFAULT NULL COMMENT '优先级',
    country_region           VARCHAR(100)            DEFAULT NULL COMMENT '国家/地区',
    scholarship              TEXT                    DEFAULT NULL COMMENT '奖学金信息',
    salary_amount            DECIMAL(10, 2)          DEFAULT NULL COMMENT '薪资金额',
    salary_currency          VARCHAR(10)             DEFAULT NULL COMMENT '薪资货币类型，如£、US$',
    living_expenses_amount   DECIMAL(10, 2)          DEFAULT NULL COMMENT '生活费用金额',
    living_expenses_currency VARCHAR(10)             DEFAULT NULL COMMENT '生活费用货币类型，如£、US$',
    research_field           TEXT                    DEFAULT NULL COMMENT '研究方向',
    application_requirements TEXT                    DEFAULT NULL COMMENT '申请要求',
    application_deadline     DATETIME                DEFAULT NULL COMMENT '招生截止时间',
    drug_prohibition         TINYINT(1)              DEFAULT NULL COMMENT '是否禁毒（1为是，0为否）',
    gun_control              TINYINT(1)              DEFAULT NULL COMMENT '是否控枪（1为是，0为否）',
    qs_rank                  INT                     DEFAULT NULL COMMENT 'QS排名',
    usnews_rank              INT                     DEFAULT NULL COMMENT 'US News排名',
    education_duration       VARCHAR(100)            DEFAULT NULL COMMENT '学制，如4年制',
    application_difficulty   VARCHAR(100)            DEFAULT NULL COMMENT '身份难度，如难',
    reference_material       TEXT                    DEFAULT NULL COMMENT '参考资料链接或描述',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='院校信息表';

-- ----------------------------
-- 大学排名数据汇总
-- ----------------------------
CREATE TABLE IF NOT EXISTS university_rankings_all
(
    id                           INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese      VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（中文）',
    university_name_english      VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（英文）',
    university_tags              VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（例如：国家）',
    university_tags_state        VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（例如：洲）',
    ranking_year                 VARCHAR(20)           DEFAULT NULL COMMENT '排名年份',
    current_rank_integer_qs      INT                   DEFAULT NULL COMMENT '当前qs排名（整数）',
    current_rank_integer_qs_cs   INT                   DEFAULT NULL COMMENT '当前qs计算机排名（整数）',
    current_rank_integer_usnews  INT                   DEFAULT NULL COMMENT '当前usnews排名（整数）',
    current_rank_integer_usnews_cs INT                 DEFAULT NULL COMMENT '当前usnews计算机排名（整数）',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='大学排名数据汇总';

-- ----------------------------
-- 大学qs排名数据
-- ----------------------------
CREATE TABLE IF NOT EXISTS university_rankings_qs
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（中文）',
    university_name_english VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（英文）',
    university_tags         VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（例如：国家）',
    university_tags_state   VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（例如：洲）',
    ranking_category        VARCHAR(100)          DEFAULT NULL COMMENT '排名类别（例如：计算机科学）',
    ranking_year            VARCHAR(20)           DEFAULT NULL COMMENT '排名年份',
    current_rank_integer    INT                   DEFAULT NULL COMMENT '当前排名（整数）',
    current_rank_raw        VARCHAR(50)           DEFAULT NULL COMMENT '当前排名（原始数据，例如“=2”）',
    rank_variant            VARCHAR(100)          DEFAULT NULL COMMENT '排名类别',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='大学qs排名数据';

-- ----------------------------
-- 大学qs排名计算机数据
-- ----------------------------
CREATE TABLE IF NOT EXISTS university_rankings_qs_cs
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（中文）',
    university_name_english VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（英文）',
    university_tags         VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（例如：国家）',
    university_tags_state   VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（例如：洲）',
    ranking_category        VARCHAR(100)          DEFAULT NULL COMMENT '排名类别（例如：计算机科学）',
    ranking_year            VARCHAR(20)           DEFAULT NULL COMMENT '排名年份',
    current_rank_integer    INT                   DEFAULT NULL COMMENT '当前排名（整数）',
    current_rank_raw        VARCHAR(50)           DEFAULT NULL COMMENT '当前排名（原始数据，例如“=2”）',
    rank_variant            VARCHAR(100)          DEFAULT NULL COMMENT '排名类别',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='大学qs排名计算机数据';

-- ----------------------------
-- 大学USNews排名数据
-- ----------------------------
CREATE TABLE IF NOT EXISTS university_rankings_usnews
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（中文）',
    university_name_english VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（英文）',
    university_tags         VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（例如：国家）',
    university_tags_state   VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（例如：洲）',
    ranking_category        VARCHAR(100)          DEFAULT NULL COMMENT '排名类别（例如：计算机科学）',
    ranking_year            VARCHAR(20)           DEFAULT NULL COMMENT '排名年份',
    current_rank_integer    INT                   DEFAULT NULL COMMENT '当前排名（整数）',
    current_rank_raw        VARCHAR(50)           DEFAULT NULL COMMENT '当前排名（原始数据，例如“=2”）',
    rank_variant            VARCHAR(100)          DEFAULT NULL COMMENT '排名类别',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='大学USNews排名数据';

-- ----------------------------
-- 大学USNews排名计算机数据
-- ----------------------------
CREATE TABLE IF NOT EXISTS university_rankings_usnews_cs
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（中文）',
    university_name_english VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（英文）',
    university_tags         VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（例如：国家）',
    university_tags_state   VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（例如：洲）',
    ranking_category        VARCHAR(100)          DEFAULT NULL COMMENT '排名类别（例如：计算机科学）',
    ranking_year            VARCHAR(20)           DEFAULT NULL COMMENT '排名年份',
    current_rank_integer    INT                   DEFAULT NULL COMMENT '当前排名（整数）',
    current_rank_raw        VARCHAR(50)           DEFAULT NULL COMMENT '当前排名（原始数据，例如“=2”）',
    rank_variant            VARCHAR(100)          DEFAULT NULL COMMENT '排名类别',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='大学USNews排名计算机数据';

-- ----------------------------
-- echarts排名数据
-- ----------------------------
CREATE TABLE IF NOT EXISTS university_rankings_echarts
(
    id                      INT     NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)       DEFAULT NULL COMMENT '大学名称（中文）',
    university_tags         VARCHAR(100)       DEFAULT NULL COMMENT '大学标签（例如：国家）',
    university_tags_state   VARCHAR(100)       DEFAULT NULL COMMENT '大学标签（例如：洲）',
    ranking_qs              TEXT               DEFAULT NULL COMMENT 'qs数据',
    ranking_qs_cs           TEXT               DEFAULT NULL COMMENT 'qs计算机科学数据',
    ranking_usnews          TEXT               DEFAULT NULL COMMENT 'usnews数据',
    ranking_usnews_cs       TEXT               DEFAULT NULL COMMENT 'usnews计算机科学数据',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='echarts排名数据';

-- ----------------------------
-- 意向学校信息
-- ----------------------------
CREATE TABLE IF NOT EXISTS university_consider
(
    id                INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)    DEFAULT NULL COMMENT '大学名称（中文）',
    status_qs         INT                     DEFAULT NULL COMMENT 'QS排名强弱，0：弱，1：中等，2：强',
    status_qs_cs      INT                     DEFAULT NULL COMMENT 'QS计算机排名强弱，0：弱，1：中等，2：强',
    status_usnews     INT                     DEFAULT NULL COMMENT 'US News排名强弱，0：弱，1：中等，2：强',
    status_usnews_cs  INT                     DEFAULT NULL COMMENT 'US News计算机排名强弱，0：弱，1：中等，2：强',
    status_total      INT                     DEFAULT NULL COMMENT '整体排名强弱，0：弱，1：中等，2：强',
    consider          INT                     DEFAULT NULL COMMENT '是否考虑，0：不考虑，1：考虑',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='意向学校信息';

-- ----------------------------
-- 初始化数据
-- ----------------------------
INSERT INTO login_user (username, password, role)
VALUES ('admin', '$2b$12$H37dBiw.cVxC8QVLaQHD0u7aJn7gWKmqy/2RnkhcIW5PDJm8YHZNu', 'ROLE_ADMIN');

INSERT INTO university_rankings_echarts (university_name_chinese, university_tags, university_tags_state,
                                         ranking_qs, ranking_qs_cs, ranking_usnews, ranking_usnews_cs)
VALUES ('亚利桑那州立大学', '美国', '北美洲',
        '[330.0, 293.0, 294.0, 249.0, 222.0, 209.0, 212.0, 215.0, 220.0, 216.0, 219.0, 179.0, 200.0]',
        '[0.0, 0.0, 201.0, 201.0, 151.0, 151.0, 151.0, 201.0, 151.0, 148.0, 151.0, 155.0, 0.0]',
        '[0.0, 0.0, 143.0, 148.0, 121.0, 134.0, 145.0, 146.0, 146.0, 165.0, 156.0, 0.0, 179.0]',
        '[0.0, 0.0, 65.0, 72.0, 75.0, 89.0, 65.0, 78.0, 65.0, 89.0, 87.0, 0.0, 0.0]');

-- ----------------------------
-- 常用查询索引
-- ----------------------------
ALTER TABLE login_user ADD INDEX idx_username (username);

ALTER TABLE university_rankings_all ADD INDEX idx_all_name_year (university_name_chinese, ranking_year);
ALTER TABLE university_rankings_all ADD INDEX idx_all_tags (university_tags, university_tags_state);
ALTER TABLE university_rankings_all ADD INDEX idx_all_qs (current_rank_integer_qs);

ALTER TABLE university_rankings_qs ADD INDEX idx_qs_name_year (university_name_chinese, ranking_year);
ALTER TABLE university_rankings_qs ADD INDEX idx_qs_tags (university_tags, university_tags_state);

ALTER TABLE university_rankings_qs_cs ADD INDEX idx_qs_cs_name_year (university_name_chinese, ranking_year);
ALTER TABLE university_rankings_qs_cs ADD INDEX idx_qs_cs_tags (university_tags, university_tags_state);

ALTER TABLE university_rankings_usnews ADD INDEX idx_usnews_name_year (university_name_chinese, ranking_year);
ALTER TABLE university_rankings_usnews ADD INDEX idx_usnews_tags (university_tags, university_tags_state);

ALTER TABLE university_rankings_usnews_cs ADD INDEX idx_usnews_cs_name_year (university_name_chinese, ranking_year);
ALTER TABLE university_rankings_usnews_cs ADD INDEX idx_usnews_cs_tags (university_tags, university_tags_state);

ALTER TABLE university_rankings_echarts ADD INDEX idx_echarts_name (university_name_chinese);

ALTER TABLE university_consider ADD INDEX idx_consider_name (university_name_chinese);

ALTER TABLE choose_phd ADD INDEX idx_phd_name (university_name);
ALTER TABLE choose_phd ADD INDEX idx_phd_country (country_region);
