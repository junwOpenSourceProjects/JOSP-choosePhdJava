-- choosePhd core schema
-- 8 tables: region, subject, ranking_source, university, university_alias, ranking_entry, user_account, user_shortlist

CREATE TABLE IF NOT EXISTS region (
    id INT PRIMARY KEY,
    slug VARCHAR(40) NOT NULL UNIQUE,
    name_zh VARCHAR(40) NOT NULL,
    name_en VARCHAR(40) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS subject (
    id INT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    name_zh VARCHAR(120) NOT NULL,
    name_en VARCHAR(120) NOT NULL,
    owner_org VARCHAR(40) NOT NULL,
    active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ranking_source (
    id INT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    name_zh VARCHAR(120) NOT NULL,
    name_en VARCHAR(120),
    kind TINYINT NOT NULL COMMENT '1=综合主榜 2=区域榜 3=学科榜 4=趋势榜',
    owner_org VARCHAR(40) NOT NULL,
    active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS university (
    url_id VARCHAR(120) PRIMARY KEY,
    name_zh VARCHAR(120) NOT NULL,
    name_en VARCHAR(200) NOT NULL,
    name_zh_tw VARCHAR(120),
    country VARCHAR(80) NOT NULL DEFAULT 'unknown',
    region VARCHAR(40) NOT NULL DEFAULT 'unknown',
    badge_url VARCHAR(300),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS university_alias (
    alias_url_id VARCHAR(120) PRIMARY KEY,
    target_url_id VARCHAR(120) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (target_url_id) REFERENCES university(url_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ranking_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    university_id VARCHAR(120) NOT NULL,
    source_id INT NOT NULL,
    subject_id INT,
    year SMALLINT NOT NULL,
    rank_display VARCHAR(64) NOT NULL,
    rank_value INT,
    rank_delta INT,
    direction TINYINT COMMENT '1=up -1=down 0=flat',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_entry_natural (university_id, source_id, subject_id, year),
    KEY idx_entry_source_year_rank (source_id, year, rank_value),
    KEY idx_entry_uni_source_year (university_id, source_id, year),
    FOREIGN KEY (university_id) REFERENCES university(url_id),
    FOREIGN KEY (source_id) REFERENCES ranking_source(id),
    FOREIGN KEY (subject_id) REFERENCES subject(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(80) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_shortlist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    university_id VARCHAR(120) NOT NULL,
    priority TINYINT NOT NULL DEFAULT 2 COMMENT '1=强 2=中 3=弱 4=不考虑',
    note TEXT,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_shortlist_user_uni (user_id, university_id),
    FOREIGN KEY (user_id) REFERENCES user_account(id),
    FOREIGN KEY (university_id) REFERENCES university(url_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
