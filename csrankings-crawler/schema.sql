-- csrankings.org 全量数据入库 schema
-- MySQL 8+ (utf8mb4)
-- 数据来源: https://github.com/emeryberger/CSrankings (csrankings.org 官方仓库)

DROP DATABASE IF EXISTS csrankings;
CREATE DATABASE csrankings DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE csrankings;

-- 1. 国家 / 地区 (ISO codes + 大洲)
CREATE TABLE country (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(120) NOT NULL,
    alpha_2         CHAR(2)  NULL,
    alpha_3         CHAR(3)  NULL,
    country_code    INT      NULL,
    iso_3166_2      VARCHAR(40) NULL,
    region          VARCHAR(60)  NULL,   -- Continent (e.g. Asia)
    sub_region      VARCHAR(60)  NULL,
    intermediate_region VARCHAR(80) NULL,
    region_code     INT NULL,
    sub_region_code INT NULL,
    intermediate_region_code INT NULL,
    UNIQUE KEY uk_alpha2 (alpha_2),
    UNIQUE KEY uk_alpha3 (alpha_3),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB;

-- 2. 院校 (归属 country by alpha_2)
CREATE TABLE institution (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(200) NOT NULL,
    region          VARCHAR(60)  NULL,    -- e.g. "europe" "northamerica" "asia" (csrankings 自定义 region)
    country_alpha2  CHAR(2)  NULL,
    homepage        VARCHAR(500) NULL,
    faculty_count   INT NOT NULL DEFAULT 0,    -- 派生: 教员人数 (从 faculty 表统计)
    UNIQUE KEY uk_name (name),
    KEY idx_country (country_alpha2),
    KEY idx_region (region)
) ENGINE=InnoDB;

-- 3. 教员
CREATE TABLE faculty (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(200) NOT NULL,
    institution_id  INT NULL,
    homepage        VARCHAR(500) NULL,
    scholar_id      VARCHAR(40)  NULL,        -- Google Scholar ID
    orcid           VARCHAR(40)  NULL,
    is_turing       TINYINT(1) NOT NULL DEFAULT 0,
    is_acm_fellow   TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_name_institution (name, institution_id),
    KEY idx_institution (institution_id),
    KEY idx_name (name)
) ENGINE=InnoDB;

-- 4. 研究领域 (csrankings areaMap: 27 top + 67 sub)
CREATE TABLE research_area (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    code            VARCHAR(40) NOT NULL,    -- e.g. "ai" "mlmining" "icml"
    title           VARCHAR(80) NOT NULL,    -- e.g. "AI" "ML"
    parent_code     VARCHAR(40) NULL,        -- 子领域指向一级 (e.g. "icml" -> "mlmining")
    top_category    ENUM('ai','systems','theory','interdisciplinary') NULL,
    is_top_tier     TINYINT(1) NOT NULL DEFAULT 0,  -- csrankings 默认勾选
    is_next_tier    TINYINT(1) NOT NULL DEFAULT 0,  -- csrankings 隐藏
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB;

-- 5. 教员 × 领域 × 年份 论文计数 (csrankings 网站排名的"原始事实")
-- 来自 generated-author-info.csv: name, dept, area, count, adjustedcount, year
CREATE TABLE faculty_publication_count (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    faculty_id      INT NOT NULL,
    area_code       VARCHAR(40) NOT NULL,
    year            INT NOT NULL,
    count_pubs      DECIMAL(8,3) NOT NULL DEFAULT 0,  -- 原始论文数
    adjusted_count  DECIMAL(8,3) NOT NULL DEFAULT 0,  -- 调整后(按作者数)
    UNIQUE KEY uk_faculty_area_year (faculty_id, area_code, year),
    KEY idx_faculty (faculty_id),
    KEY idx_area (area_code),
    KEY idx_year (year)
) ENGINE=InnoDB;

-- 6. 图灵奖
CREATE TABLE turing_award (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(200) NOT NULL,
    year            INT NOT NULL,
    UNIQUE KEY uk_name_year (name, year)
) ENGINE=InnoDB;

-- 7. ACM Fellow
CREATE TABLE acm_fellow (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(200) NOT NULL,
    year            INT NOT NULL,
    UNIQUE KEY uk_name_year (name, year)
) ENGINE=InnoDB;

-- 视图: 院校 × 领域 × 年份 聚合排名(模拟 csrankings 网页 ranking)
-- 按 adjusted_count 累加
CREATE OR REPLACE VIEW v_institution_area_ranking AS
SELECT
    i.id          AS institution_id,
    i.name        AS institution_name,
    i.region,
    i.country_alpha2,
    ra.code       AS area_code,
    ra.title      AS area_title,
    ra.top_category,
    fpc.year,
    SUM(fpc.adjusted_count) AS total_adjusted_count,
    SUM(fpc.count_pubs)     AS total_count_pubs,
    COUNT(DISTINCT fpc.faculty_id) AS contributing_faculty
FROM institution i
JOIN faculty f ON f.institution_id = i.id
JOIN faculty_publication_count fpc ON fpc.faculty_id = f.id
JOIN research_area ra ON ra.code = fpc.area_code
GROUP BY i.id, i.name, i.region, i.country_alpha2, ra.code, ra.title, ra.top_category, fpc.year;

-- 视图: 教员个人排名(按年份范围累计)
CREATE OR REPLACE VIEW v_faculty_ranking AS
SELECT
    f.id          AS faculty_id,
    f.name        AS faculty_name,
    i.name        AS institution_name,
    i.country_alpha2,
    f.is_turing,
    f.is_acm_fellow,
    SUM(fpc.adjusted_count) AS total_adjusted_count,
    SUM(fpc.count_pubs)     AS total_count_pubs
FROM faculty f
JOIN institution i ON i.id = f.institution_id
LEFT JOIN faculty_publication_count fpc ON fpc.faculty_id = f.id
GROUP BY f.id, f.name, i.name, i.country_alpha2, f.is_turing, f.is_acm_fellow;
