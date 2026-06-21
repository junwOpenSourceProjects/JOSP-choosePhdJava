-- =============================================================================
-- JOSP-choosePhd 选校系统 · 数据库初始化脚本
-- =============================================================================
-- 用法:
--   mysql -uroot -p < db/init.sql
-- 或 (空密码):
--   mysql -uroot < db/init.sql
--
-- 行为:
--   DROP + CREATE 整个 computer_rank 库, 然后重建 9 张表 + admin 账号 + 索引
--   ⚠️ 这会清空所有现存数据, 跑前请先备份!
--
-- 跟 src/main/resources/schema.sql 的关系:
--   schema.sql 是 Spring 启动时的兜底, 用 CREATE TABLE IF NOT EXISTS 不丢数据
--   init.sql 是 user 手动 reset 用, DROP+CREATE 完全重置
--   两份 schema 同步维护, 改 schema 时记得同步两份
-- =============================================================================

DROP DATABASE IF EXISTS computer_rank;
CREATE DATABASE computer_rank
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE computer_rank;

-- ----------------------------
-- 登录用户表
-- ----------------------------
CREATE TABLE login_user
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
CREATE TABLE choose_phd
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
CREATE TABLE university_rankings_all
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
CREATE TABLE university_rankings_qs
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
CREATE TABLE university_rankings_qs_cs
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
CREATE TABLE university_rankings_usnews
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
  COLLATE = utf8mb4_unicode_ci COMMENT ='大学usnews排名数据';

-- ----------------------------
-- 大学USNews排名计算机数据
-- ----------------------------
CREATE TABLE university_rankings_usnews_cs
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
  COLLATE = utf8mb4_unicode_ci COMMENT ='大学usnews排名计算机数据';

-- ----------------------------
-- echarts排名数据
-- ----------------------------
CREATE TABLE university_rankings_echarts
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（中文）',
    university_tags         VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（例如：国家）',
    university_tags_state   VARCHAR(100)          DEFAULT NULL COMMENT '大学标签（例如：洲）',
    ranking_qs              VARCHAR(500)          DEFAULT NULL COMMENT 'qs排名历史数组（JSON）',
    ranking_qs_cs           VARCHAR(500)          DEFAULT NULL COMMENT 'qs计算机排名历史数组（JSON）',
    ranking_usnews          VARCHAR(500)          DEFAULT NULL COMMENT 'usnews排名历史数组（JSON）',
    ranking_usnews_cs       VARCHAR(500)          DEFAULT NULL COMMENT 'usnews计算机排名历史数组（JSON）',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='echarts排名数据';

-- ----------------------------
-- 意向学校信息
-- ----------------------------
CREATE TABLE university_consider
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL COMMENT '大学名称（中文）',
    priority                INT                   DEFAULT NULL COMMENT '优先级',
    note                    TEXT                  DEFAULT NULL COMMENT '备注',
    create_time             DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time             DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='意向学校信息';

-- =============================================================================
-- 初始化数据
-- =============================================================================

-- admin 账号: 用户名 admin / 密码 admin
-- 密码 hash 是 BCrypt, 由前端 vue-element-admin 标准的 /user/login 接口验证
-- ⚠️ 生产环境务必改密码 + 加 JWT 鉴权
INSERT INTO login_user (username, password, role)
VALUES ('admin', '$2b$12$H37dBiw.cVxC8QVLaQHD0u7aJn7gWKmqy/2RnkhcIW5PDJm8YHZNu', 'ROLE_ADMIN');

-- university_rankings_echarts 示例: 亚利桑那州立大学
-- (13 年历史趋势数据, JSON 数组: 2014..2026)
INSERT INTO university_rankings_echarts (university_name_chinese, university_tags, university_tags_state,
                                         ranking_qs, ranking_qs_cs, ranking_usnews, ranking_usnews_cs)
VALUES ('亚利桑那州立大学', '美国', '北美洲',
        '[330.0, 293.0, 294.0, 249.0, 222.0, 209.0, 212.0, 215.0, 220.0, 216.0, 219.0, 179.0, 200.0]',
        '[0.0, 0.0, 201.0, 201.0, 151.0, 151.0, 151.0, 201.0, 151.0, 148.0, 151.0, 155.0, 0.0]',
        '[0.0, 0.0, 143.0, 148.0, 121.0, 134.0, 145.0, 146.0, 146.0, 165.0, 156.0, 0.0, 179.0]',
        '[0.0, 0.0, 65.0, 72.0, 75.0, 89.0, 65.0, 78.0, 65.0, 89.0, 87.0, 0.0, 0.0]');

-- university_rankings_qs 示例: QS世界大学排名 2024 (20 强)
-- 跟 qs 排名/QS世界大学排名_2024.txt 前 20 行一致, init 后页面立刻有数据
INSERT INTO university_rankings_qs
    (university_name_chinese, university_name_english, university_tags, university_tags_state,
     ranking_category, ranking_year, current_rank_integer, current_rank_raw, rank_variant)
VALUES
    ('麻省理工学院',       'Massachusetts Institute of Technology, Mit', '美国',  '北美洲', 'QS世界大学排名', '2024',  1, '1',   'QS世界大学排名'),
    ('剑桥大学',           'University of Cambridge',                    '英国',  '欧洲',   'QS世界大学排名', '2024',  2, '2',   'QS世界大学排名'),
    ('牛津大学',           'University of Oxford',                       '英国',  '欧洲',   'QS世界大学排名', '2024',  3, '3',   'QS世界大学排名'),
    ('哈佛大学',           'Harvard University',                         '美国',  '北美洲', 'QS世界大学排名', '2024',  4, '4',   'QS世界大学排名'),
    ('斯坦福大学',         'Stanford University',                        '美国',  '北美洲', 'QS世界大学排名', '2024',  5, '5',   'QS世界大学排名'),
    ('帝国理工学院',       'Imperial College London',                    '英国',  '欧洲',   'QS世界大学排名', '2024',  6, '6',   'QS世界大学排名'),
    ('苏黎世联邦理工学院', 'Swiss Federal Institute of Technology Zurich','瑞士','欧洲',   'QS世界大学排名', '2024',  7, '7',   'QS世界大学排名'),
    ('新加坡国立大学',     'National University of Singapore',           '新加坡','亚洲',   'QS世界大学排名', '2024',  8, '8',   'QS世界大学排名'),
    ('伦敦大学学院',       'University College London',                  '英国',  '欧洲',   'QS世界大学排名', '2024',  9, '9',   'QS世界大学排名'),
    ('加州大学-伯克利',    'University of California, Berkeley',         '美国',  '北美洲', 'QS世界大学排名', '2024', 10, '10',  'QS世界大学排名'),
    ('芝加哥大学',         'University of Chicago',                      '美国',  '北美洲', 'QS世界大学排名', '2024', 11, '11',  'QS世界大学排名'),
    ('宾夕法尼亚大学',     'University of Pennsylvania',                 '美国',  '北美洲', 'QS世界大学排名', '2024', 12, '12',  'QS世界大学排名'),
    ('康奈尔大学',         'Cornell University',                         '美国',  '北美洲', 'QS世界大学排名', '2024', 13, '13',  'QS世界大学排名'),
    ('墨尔本大学',         'The University of Melbourne',                '澳大利亚','大洋洲','QS世界大学排名','2024', 14, '14',  'QS世界大学排名'),
    ('加州理工学院',       'California Institute of Technology',         '美国',  '北美洲', 'QS世界大学排名', '2024', 15, '15',  'QS世界大学排名'),
    ('耶鲁大学',           'Yale University',                            '美国',  '北美洲', 'QS世界大学排名', '2024', 16, '16',  'QS世界大学排名'),
    ('北京大学',           'Peking University',                          '中国',  '亚洲',   'QS世界大学排名', '2024', 17, '=17', 'QS世界大学排名'),
    ('普林斯顿大学',       'Princeton University',                       '美国',  '北美洲', 'QS世界大学排名', '2024', 17, '=17', 'QS世界大学排名'),
    ('新南威尔士大学',     'The University of New South Wales',          '澳大利亚','大洋洲','QS世界大学排名','2024', 19, '=19', 'QS世界大学排名'),
    ('悉尼大学',           'The University of Sydney',                   '澳大利亚','大洋洲','QS世界大学排名','2024', 19, '=19', 'QS世界大学排名');

-- =============================================================================
-- 常用查询索引
-- =============================================================================
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

-- =============================================================================
-- "备份 2" 新增的 7 张榜单表 (2026-06-21 灌库, 详见 scripts/ranking_import/)
-- =============================================================================

-- ARWU 学科排名
CREATE TABLE IF NOT EXISTS university_rankings_arwu_subject
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL,
    university_name_english VARCHAR(255)          DEFAULT NULL,
    university_tags         VARCHAR(100)          DEFAULT NULL,
    university_tags_state   VARCHAR(100)          DEFAULT NULL,
    ranking_category        VARCHAR(100)          DEFAULT NULL,
    ranking_year            VARCHAR(20)           DEFAULT NULL,
    current_rank_integer    INT                   DEFAULT NULL,
    current_rank_raw        VARCHAR(50)           DEFAULT NULL,
    rank_variant            VARCHAR(100)          DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='ARWU 大学学科排名';

ALTER TABLE university_rankings_arwu_subject ADD INDEX idx_arwu_subject_name_year (university_name_chinese, ranking_year);
ALTER TABLE university_rankings_arwu_subject ADD INDEX idx_arwu_subject_category (ranking_category);
ALTER TABLE university_rankings_arwu_subject ADD INDEX idx_arwu_subject_variant (rank_variant);

-- EduRank 6 个地区排名
CREATE TABLE IF NOT EXISTS university_rankings_edurank_region
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL,
    university_name_english VARCHAR(255)          DEFAULT NULL,
    university_tags         VARCHAR(100)          DEFAULT NULL,
    university_tags_state   VARCHAR(100)          DEFAULT NULL,
    ranking_category        VARCHAR(100)          DEFAULT NULL,
    ranking_year            VARCHAR(20)           DEFAULT NULL,
    current_rank_integer    INT                   DEFAULT NULL,
    current_rank_raw        VARCHAR(50)           DEFAULT NULL,
    rank_variant            VARCHAR(100)          DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='EduRank 大学地区排名';

ALTER TABLE university_rankings_edurank_region ADD INDEX idx_edurank_region_name_year (university_name_chinese, ranking_year);
ALTER TABLE university_rankings_edurank_region ADD INDEX idx_edurank_region_category (ranking_category);

-- 下降趋势
CREATE TABLE IF NOT EXISTS university_rankings_declining_trend
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL,
    university_name_english VARCHAR(255)          DEFAULT NULL,
    university_tags         VARCHAR(100)          DEFAULT NULL,
    university_tags_state   VARCHAR(100)          DEFAULT NULL,
    ranking_category        VARCHAR(100)          DEFAULT NULL,
    ranking_year            VARCHAR(20)           DEFAULT NULL,
    current_rank_integer    INT                   DEFAULT NULL,
    current_rank_raw        VARCHAR(200)          DEFAULT NULL,
    rank_variant            VARCHAR(100)          DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='大学排名下降趋势';

ALTER TABLE university_rankings_declining_trend ADD INDEX idx_declining_name_year (university_name_chinese, ranking_year);
ALTER TABLE university_rankings_declining_trend ADD INDEX idx_declining_category (ranking_category);
ALTER TABLE university_rankings_declining_trend ADD UNIQUE INDEX uniq_declining_dedup (university_name_chinese, ranking_year, ranking_category, current_rank_integer, current_rank_raw);

-- MOSIUR 全球排名
CREATE TABLE IF NOT EXISTS university_rankings_mosiur_world
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL,
    university_name_english VARCHAR(255)          DEFAULT NULL,
    university_tags         VARCHAR(100)          DEFAULT NULL,
    university_tags_state   VARCHAR(100)          DEFAULT NULL,
    ranking_category        VARCHAR(100)          DEFAULT NULL,
    ranking_year            VARCHAR(20)           DEFAULT NULL,
    current_rank_integer    INT                   DEFAULT NULL,
    current_rank_raw        VARCHAR(50)           DEFAULT NULL,
    rank_variant            VARCHAR(100)          DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='MOSIUR 大学全球排名';

ALTER TABLE university_rankings_mosiur_world ADD INDEX idx_mosiur_world_name_year (university_name_chinese, ranking_year);

-- RUR 全球学术排名
CREATE TABLE IF NOT EXISTS university_rankings_rur_world
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL,
    university_name_english VARCHAR(255)          DEFAULT NULL,
    university_tags         VARCHAR(100)          DEFAULT NULL,
    university_tags_state   VARCHAR(100)          DEFAULT NULL,
    ranking_category        VARCHAR(100)          DEFAULT NULL,
    ranking_year            VARCHAR(20)           DEFAULT NULL,
    current_rank_integer    INT                   DEFAULT NULL,
    current_rank_raw        VARCHAR(50)           DEFAULT NULL,
    rank_variant            VARCHAR(100)          DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='RUR 大学全球学术排名';

ALTER TABLE university_rankings_rur_world ADD INDEX idx_rur_world_name_year (university_name_chinese, ranking_year);

-- US News 学科排名
CREATE TABLE IF NOT EXISTS university_rankings_usnews_subject
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL,
    university_name_english VARCHAR(255)          DEFAULT NULL,
    university_tags         VARCHAR(100)          DEFAULT NULL,
    university_tags_state   VARCHAR(100)          DEFAULT NULL,
    ranking_category        VARCHAR(100)          DEFAULT NULL,
    ranking_year            VARCHAR(20)           DEFAULT NULL,
    current_rank_integer    INT                   DEFAULT NULL,
    current_rank_raw        VARCHAR(50)           DEFAULT NULL,
    rank_variant            VARCHAR(100)          DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='US News 大学学科排名';

ALTER TABLE university_rankings_usnews_subject ADD INDEX idx_usnews_subject_name_year (university_name_chinese, ranking_year);
ALTER TABLE university_rankings_usnews_subject ADD INDEX idx_usnews_subject_category (ranking_category);

-- QS 可持续大学排名
CREATE TABLE IF NOT EXISTS university_rankings_qs_sustainability
(
    id                      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(255)          DEFAULT NULL,
    university_name_english VARCHAR(255)          DEFAULT NULL,
    university_tags         VARCHAR(100)          DEFAULT NULL,
    university_tags_state   VARCHAR(100)          DEFAULT NULL,
    ranking_category        VARCHAR(100)          DEFAULT NULL,
    ranking_year            VARCHAR(20)           DEFAULT NULL,
    current_rank_integer    INT                   DEFAULT NULL,
    current_rank_raw        VARCHAR(50)           DEFAULT NULL,
    rank_variant            VARCHAR(100)          DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='QS 大学可持续排名';

ALTER TABLE university_rankings_qs_sustainability ADD INDEX idx_qs_sustainability_name_year (university_name_chinese, ranking_year);

-- =============================================================================
-- 初始化完成
-- =============================================================================
-- 9 张表 + 1 admin 账号 + 20 行 QS 2024 排名样例数据 + 17 个索引
-- 跑完后即可 mysql -uroot computer_rank -e "SELECT * FROM login_user; SELECT COUNT(*) FROM university_rankings_qs;"
-- 应分别看到 1 行 (admin) 和 20 行 QS 排名
--
-- 完整导入数据: 后端启起来后 POST /api/v1/import/rankings/scanLocal 即可
-- (前提: 把 'qs 排名/' 文件夹放在后端工作目录下, 14 个 .txt 文件 = 1400+ 条数据)
