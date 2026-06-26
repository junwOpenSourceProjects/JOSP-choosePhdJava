-- 院校标签体系（独立表方案）

CREATE TABLE IF NOT EXISTS university_tag (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '标签 ID',
    slug VARCHAR(60) NOT NULL COMMENT '标签标识',
    name_zh VARCHAR(60) NOT NULL COMMENT '中文名称',
    name_en VARCHAR(60) DEFAULT NULL COMMENT '英文名称',
    category VARCHAR(40) DEFAULT 'other' COMMENT '分类：domestic 国内 / foreign 国外 / other 其他',
    color VARCHAR(20) DEFAULT NULL COMMENT '展示颜色',
    sort_order INT DEFAULT 0 COMMENT '排序',
    active TINYINT DEFAULT 1 COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_tag_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='院校标签定义表';

CREATE TABLE IF NOT EXISTS university_tag_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    university_id VARCHAR(120) NOT NULL COMMENT '院校 url_id',
    tag_id INT NOT NULL COMMENT '标签 ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_university_tag (university_id, tag_id),
    KEY idx_tag_id (tag_id),
    CONSTRAINT fk_utr_tag FOREIGN KEY (tag_id) REFERENCES university_tag(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='院校与标签关联表';
