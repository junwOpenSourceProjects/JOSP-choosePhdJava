-- =============================================================
-- choosePhd 重做版 · V1 初始化
-- 设计目标:把 7 张榜单各自一表 → 5 张核心事实表 + 2 张辅助字典
-- 设计原则:
--   1. url_id 跨榜单稳定主键(MIT 在 QS/THE/ARWU 写法差异不会重复)
--   2. 排名数据是事实表(ranking_entry),加新榜单 = 插 ranking_source 一行 + ETL 一次
--   3. trend(增长/下滑趋势)也落事实表(ranking_trend),不分表
--   4. 字典独立(region/subject/ranking_source),便于后端枚举
--   5. 全部 utf8mb4, 引擎 InnoDB, 软删(deleted TINYINT)
-- =============================================================

SET NAMES utf8mb4;

-- -------------------------------------------------------------
-- 1. region · 地区字典(中国/美国/英国...)
-- -------------------------------------------------------------
CREATE TABLE region (
    id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
    code          VARCHAR(32) NOT NULL COMMENT 'ISO 3166-1 alpha-2 或自定义区域码',
    name_en       VARCHAR(128) NOT NULL,
    name_zh       VARCHAR(128) NOT NULL,
    parent_id     INT UNSIGNED NULL COMMENT '支持多级,例如 CN > CN-31',
    latitude      DECIMAL(9,6) NULL,
    longitude     DECIMAL(9,6) NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_region_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地区字典';

-- -------------------------------------------------------------
-- 2. subject · 学科字典(计算机/工程/医学...)
-- -------------------------------------------------------------
CREATE TABLE subject (
    id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
    code          VARCHAR(64) NOT NULL COMMENT '内部编码, 例如 CS, ENG, MED',
    name_en       VARCHAR(128) NOT NULL,
    name_zh       VARCHAR(128) NOT NULL,
    parent_id     INT UNSIGNED NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_subject_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学科字典';

-- -------------------------------------------------------------
-- 3. ranking_source · 榜单元数据(加新榜单只插这一行)
-- -------------------------------------------------------------
CREATE TABLE ranking_source (
    id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
    code          VARCHAR(32) NOT NULL COMMENT '榜单短码: QS / THE / ARWU / USNEWS / GRUR / EDURANK / MOSIUR / RUR / CWUR / MENGY',
    name_en       VARCHAR(128) NOT NULL,
    name_zh       VARCHAR(128) NOT NULL,
    organization  VARCHAR(128) NULL COMMENT '发布机构',
    methodology_url VARCHAR(512) NULL,
    scale         VARCHAR(32) NULL COMMENT '榜单规模,例如 world / region / subject',
    has_trend     TINYINT NOT NULL DEFAULT 0 COMMENT '是否有 trend(增长/下滑)数据',
    has_score     TINYINT NOT NULL DEFAULT 0 COMMENT '是否给出综合分',
    active        TINYINT NOT NULL DEFAULT 1,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_source_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='榜单元数据';

-- -------------------------------------------------------------
-- 4. university · 院校主表(以 url_id 为跨源稳定主键)
-- -------------------------------------------------------------
CREATE TABLE university (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    url_id        VARCHAR(128) NOT NULL COMMENT '跨源稳定 id,例如 mit / stanford / tsinghua',
    name_en       VARCHAR(256) NOT NULL,
    name_zh       VARCHAR(256) NULL,
    logo_url      VARCHAR(512) NULL,
    region_id     INT UNSIGNED NULL,
    country_code  VARCHAR(8) NULL,
    city          VARCHAR(64) NULL,
    website       VARCHAR(256) NULL,
    founded_year  SMALLINT NULL,
    type          VARCHAR(32) NULL COMMENT 'public / private',
    aliases       JSON NULL COMMENT '别名词数组,跨榜单名称变体',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_university_url_id (url_id),
    KEY idx_university_region (region_id),
    KEY idx_university_country (country_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='院校主表';

-- -------------------------------------------------------------
-- 5. ranking_entry · 排名事实表(主体)
-- 同一所大学 × 同一榜单 × 同一学科 × 同一年 = 1 行
-- -------------------------------------------------------------
CREATE TABLE ranking_entry (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    university_id BIGINT UNSIGNED NOT NULL,
    source_id     INT UNSIGNED NOT NULL,
    subject_id    INT UNSIGNED NULL COMMENT 'NULL = 整体/综合榜',
    year          SMALLINT NOT NULL,
    rank_display  INT NULL COMMENT '榜单原始名次,可能并列',
    rank_exact    DECIMAL(10,2) NULL COMMENT '并列名次用加权精确值',
    score         DECIMAL(8,3) NULL COMMENT '综合分,无则为 NULL',
    indicators    JSON NULL COMMENT '细分指标, 例如 {teaching:80, research:90, ...}',
    source_raw_id VARCHAR(128) NULL COMMENT '在源榜单里的 url_id,用于回溯',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_entry (university_id, source_id, subject_id, year),
    KEY idx_entry_source_year (source_id, year),
    KEY idx_entry_subject_year (subject_id, year),
    KEY idx_entry_rank (source_id, year, rank_display)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排名事实表';

-- -------------------------------------------------------------
-- 6. ranking_trend · 增长/下滑趋势(单值/多值都用一行)
-- 来自各榜单 trend 目录:rank 字段可能是预渲染 HTML(已 strip 后入库)
-- -------------------------------------------------------------
CREATE TABLE ranking_trend (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    university_id BIGINT UNSIGNED NOT NULL,
    source_id     INT UNSIGNED NOT NULL,
    subject_id    INT UNSIGNED NULL,
    trend_type    VARCHAR(16) NOT NULL COMMENT 'GROWING / DECLINING / STABLE',
    base_year     SMALLINT NOT NULL COMMENT '基准年',
    target_year   SMALLINT NOT NULL COMMENT '目标年',
    rank_change   INT NULL COMMENT '正=上升,负=下滑,0=持平',
    rank_from     INT NULL,
    rank_to       INT NULL,
    note          VARCHAR(256) NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_trend (university_id, source_id, subject_id, trend_type, base_year, target_year),
    KEY idx_trend_source_type (source_id, trend_type, target_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排名趋势';

-- -------------------------------------------------------------
-- 7. users · 自用账号(单用户即可,留扩展空间)
-- -------------------------------------------------------------
CREATE TABLE users (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username      VARCHAR(64) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    display_name  VARCHAR(64) NULL,
    role          VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT 'USER / ADMIN',
    enabled       TINYINT NOT NULL DEFAULT 1,
    last_login_at DATETIME NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- -------------------------------------------------------------
-- 8. import_job · 数据导入进度(可重入)
-- -------------------------------------------------------------
CREATE TABLE import_job (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    job_key       VARCHAR(128) NOT NULL COMMENT '幂等 key,例如 source:QS:2024',
    source_id     INT UNSIGNED NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
    total_rows    INT NOT NULL DEFAULT 0,
    processed_rows INT NOT NULL DEFAULT 0,
    inserted_rows INT NOT NULL DEFAULT 0,
    updated_rows  INT NOT NULL DEFAULT 0,
    skipped_rows  INT NOT NULL DEFAULT 0,
    error_message TEXT NULL,
    started_at    DATETIME NULL,
    finished_at   DATETIME NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_job_key (job_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据导入任务';

-- =============================================================
-- 初始化 9 个 ranking_source 元数据
-- =============================================================
INSERT INTO ranking_source (code, name_en, name_zh, organization, has_trend, has_score) VALUES
 ('QS',       'QS World University Rankings',          'QS 世界大学排名',     'Quacquarelli Symonds', 1, 1),
 ('THE',      'Times Higher Education',                '泰晤士高等教育排名', 'Times Higher Education', 1, 1),
 ('ARWU',     'Academic Ranking of World Universities','上海交大世界大学学术排名','Shanghai Jiao Tong Univ.', 0, 1),
 ('USNEWS',   'US News Best Global Universities',      'US News 全球大学',   'U.S. News & World Report', 0, 1),
 ('GRUR',     'GRUR University Rankings',              'GRUR 大学排名',     'Global Research University Rank', 0, 1),
 ('EDURANK',  'EduRank',                                'EduRank 大学排名',  'EduRank.org', 0, 1),
 ('MOSIUR',   'MosIur University Rankings',            'MosIur 大学排名',   'Moscow International Univ. Rank', 0, 1),
 ('RUR',      'Round University Ranking',               'RUR 排名',          'RUR Rankings Agency', 1, 1),
 ('CWUR',     'Center for World University Rankings',  'CWUR 排名',         'Center for World University Rankings', 0, 1),
 ('MENGY',    'Menggy / MengYi University Rank',        '梦奇 / Menggy 排名', 'Menggy / MengYi', 1, 0);
