-- 防爬虫审计日志 — 记录所有 UA 黑名单 / 频率限制拦截
-- 仅写不删：admin 页面支持分页查询，按 IP/时间窗口聚合告警

CREATE TABLE scrape_audit (
    id              BIGINT          NOT NULL            COMMENT '雪花 ID',
    ip              VARCHAR(45)     NOT NULL            COMMENT '客户端 IP（兼容 IPv6）',
    user_agent      VARCHAR(500)    DEFAULT NULL        COMMENT '原始 UA',
    method          VARCHAR(10)     NOT NULL            COMMENT 'HTTP 方法',
    path            VARCHAR(500)    NOT NULL            COMMENT '请求路径',
    status_code     INT             NOT NULL            COMMENT '拦截返回码（403/429）',
    reject_reason   VARCHAR(100)    NOT NULL            COMMENT '拦截原因标签',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拦截时间',
    PRIMARY KEY (id),
    INDEX idx_ip_time (ip, created_at),
    INDEX idx_path_time (path, created_at),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='反爬虫审计日志';
