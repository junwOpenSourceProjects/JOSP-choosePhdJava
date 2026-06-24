"""MySQL schema and batched storage helpers."""
import logging
from contextlib import contextmanager
from typing import Any, Dict, List, Optional, Tuple

import pymysql

from config import DB_HOST, DB_PORT, DB_USER, DB_PASSWORD, DB_NAME

logger = logging.getLogger(__name__)

CREATE_SCHEMA_SQL = """
CREATE DATABASE IF NOT EXISTS `{db_name}`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
"""

CREATE_TABLES_SQL = """
USE `{db_name}`;

CREATE TABLE IF NOT EXISTS ranking_systems (
    id INT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(128) NOT NULL COMMENT 'URL slug, e.g. qs-world-universities-rankings',
    name_zh VARCHAR(128) NOT NULL COMMENT '中文名称',
    name_en VARCHAR(256) DEFAULT NULL COMMENT '英文名称',
    category VARCHAR(64) DEFAULT NULL COMMENT '大类, e.g. QS/THE/ARWU',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_system_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排名体系';

CREATE TABLE IF NOT EXISTS ranking_lists (
    id INT AUTO_INCREMENT PRIMARY KEY,
    system_id INT NOT NULL,
    slug VARCHAR(128) NOT NULL COMMENT 'URL slug',
    year INT NOT NULL COMMENT '排名年份',
    name_zh VARCHAR(256) DEFAULT NULL,
    total_entities INT DEFAULT NULL COMMENT '该年榜单大学总数',
    url VARCHAR(512) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_list_slug_year (slug, year),
    KEY idx_system (system_id),
    CONSTRAINT fk_list_system FOREIGN KEY (system_id) REFERENCES ranking_systems(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='具体榜单+年份';

CREATE TABLE IF NOT EXISTS universities (
    id INT AUTO_INCREMENT PRIMARY KEY,
    url_id VARCHAR(128) NOT NULL COMMENT 'URL slug',
    name_zh VARCHAR(256) NOT NULL COMMENT '中文名',
    name_en VARCHAR(512) DEFAULT NULL COMMENT '英文名',
    name_fanti VARCHAR(256) DEFAULT NULL COMMENT '繁体中文名',
    badge_url VARCHAR(512) DEFAULT NULL COMMENT 'Logo URL',
    detail_url VARCHAR(512) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_univ_url_id (url_id),
    KEY idx_name_zh (name_zh),
    KEY idx_name_en (name_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大学去重主表';

CREATE TABLE IF NOT EXISTS tags (
    id INT AUTO_INCREMENT PRIMARY KEY,
    external_id INT DEFAULT NULL COMMENT '网站标签ID',
    url_id VARCHAR(128) DEFAULT NULL,
    name_zh VARCHAR(128) NOT NULL,
    name_en VARCHAR(128) DEFAULT NULL,
    tag_type VARCHAR(32) DEFAULT NULL COMMENT 'continent/country/region/other',
    parent_ids VARCHAR(128) DEFAULT NULL,
    icon_url VARCHAR(512) DEFAULT NULL,
    UNIQUE KEY uk_tag_name_type (name_zh, tag_type),
    KEY idx_external_id (external_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签字典';

CREATE TABLE IF NOT EXISTS university_tags (
    id INT AUTO_INCREMENT PRIMARY KEY,
    university_id INT NOT NULL,
    tag_id INT NOT NULL,
    UNIQUE KEY uk_univ_tag (university_id, tag_id),
    CONSTRAINT fk_ut_univ FOREIGN KEY (university_id) REFERENCES universities(id) ON DELETE CASCADE,
    CONSTRAINT fk_ut_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大学标签关联';

CREATE TABLE IF NOT EXISTS ranking_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    list_id INT NOT NULL,
    university_id INT NOT NULL,
    rank_display VARCHAR(128) NOT NULL COMMENT '如 #1, =2, 1001-1200',
    rank_raw VARCHAR(128) DEFAULT NULL COMMENT '原始排名字符串',
    rank_int INT DEFAULT NULL COMMENT '取整后排名，并列取最小值',
    rank_range_start INT DEFAULT NULL,
    rank_range_end INT DEFAULT NULL,
    year INT NOT NULL,
    data_source VARCHAR(64) DEFAULT 'entity_api' COMMENT 'entity_api / download_api',
    liked TINYINT(1) DEFAULT 0,
    crawled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_entry_list_univ (list_id, university_id),
    KEY idx_list_rank (list_id, rank_int),
    CONSTRAINT fk_entry_list FOREIGN KEY (list_id) REFERENCES ranking_lists(id) ON DELETE CASCADE,
    CONSTRAINT fk_entry_univ FOREIGN KEY (university_id) REFERENCES universities(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排名记录';

CREATE TABLE IF NOT EXISTS university_details (
    id INT AUTO_INCREMENT PRIMARY KEY,
    university_id INT NOT NULL,
    page_html MEDIUMTEXT DEFAULT NULL,
    description TEXT DEFAULT NULL COMMENT '学校简介',
    website VARCHAR(1024) DEFAULT NULL,
    location VARCHAR(512) DEFAULT NULL,
    founded_year VARCHAR(256) DEFAULT NULL,
    motto VARCHAR(1024) DEFAULT NULL,
    address VARCHAR(1024) DEFAULT NULL,
    extracted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_detail_univ (university_id),
    CONSTRAINT fk_detail_univ FOREIGN KEY (university_id) REFERENCES universities(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大学详情页原始与解析数据';

CREATE TABLE IF NOT EXISTS crawl_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task VARCHAR(128) NOT NULL,
    identifier VARCHAR(256) DEFAULT NULL,
    status VARCHAR(32) NOT NULL COMMENT 'started/success/failed',
    message TEXT,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP NULL DEFAULT NULL,
    KEY idx_task_status (task, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='爬取日志';
"""


class Storage:
    def __init__(self):
        self.conn_kwargs = {
            "host": DB_HOST,
            "port": DB_PORT,
            "user": DB_USER,
            "password": DB_PASSWORD,
            "charset": "utf8mb4",
            "cursorclass": pymysql.cursors.DictCursor,
            "autocommit": False,
        }
        # In-memory caches to avoid repeated SELECTs
        self._univ_cache: Dict[str, int] = {}
        self._tag_cache: Dict[Tuple[str, Optional[str]], int] = {}
        self._list_cache: Dict[Tuple[str, int], int] = {}
        self._system_cache: Dict[str, int] = {}

    @contextmanager
    def _conn(self, database: Optional[str] = None):
        kwargs = self.conn_kwargs.copy()
        if database:
            kwargs["database"] = database
        conn = pymysql.connect(**kwargs)
        try:
            yield conn
            conn.commit()
        except Exception:
            conn.rollback()
            raise
        finally:
            conn.close()

    @contextmanager
    def _cursor(self, database: Optional[str] = None):
        with self._conn(database) as conn:
            yield conn.cursor()

    def init_database(self):
        with self._cursor() as cur:
            cur.execute(CREATE_SCHEMA_SQL.format(db_name=DB_NAME))
        with self._cursor(database=DB_NAME) as cur:
            for stmt in CREATE_TABLES_SQL.format(db_name=DB_NAME).split(";"):
                stmt = stmt.strip()
                if stmt:
                    cur.execute(stmt + ";")
        self._load_caches()
        logger.info("Database %s initialized", DB_NAME)

    def _load_caches(self):
        """Preload existing IDs into memory caches."""
        with self._cursor(database=DB_NAME) as cur:
            cur.execute("SELECT id, slug FROM ranking_systems")
            for row in cur.fetchall():
                self._system_cache[row["slug"]] = row["id"]
            cur.execute("SELECT id, slug, year FROM ranking_lists")
            for row in cur.fetchall():
                self._list_cache[(row["slug"], row["year"])] = row["id"]
            cur.execute("SELECT id, url_id FROM universities")
            for row in cur.fetchall():
                self._univ_cache[row["url_id"]] = row["id"]
            cur.execute("SELECT id, name_zh, tag_type FROM tags")
            for row in cur.fetchall():
                self._tag_cache[(row["name_zh"], row["tag_type"])] = row["id"]

    # --- batch insert helpers ---

    def bulk_insert_systems(self, systems: List[Tuple[str, str, Optional[str], Optional[str]]]):
        if not systems:
            return
        with self._cursor(database=DB_NAME) as cur:
            cur.executemany(
                """
                INSERT INTO ranking_systems (slug, name_zh, name_en, category)
                VALUES (%s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE
                    name_zh=VALUES(name_zh), name_en=VALUES(name_en), category=VALUES(category)
                """,
                systems,
            )
        self._refresh_system_cache()

    def _refresh_system_cache(self):
        with self._cursor(database=DB_NAME) as cur:
            cur.execute("SELECT id, slug FROM ranking_systems")
            for row in cur.fetchall():
                self._system_cache[row["slug"]] = row["id"]

    def bulk_insert_lists(self, lists: List[Tuple[int, str, int, Optional[str], Optional[int], Optional[str]]]):
        if not lists:
            return
        with self._cursor(database=DB_NAME) as cur:
            cur.executemany(
                """
                INSERT INTO ranking_lists (system_id, slug, year, name_zh, total_entities, url)
                VALUES (%s, %s, %s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE
                    system_id=VALUES(system_id), name_zh=VALUES(name_zh),
                    total_entities=VALUES(total_entities), url=VALUES(url)
                """,
                lists,
            )
        self._refresh_list_cache()

    def _refresh_list_cache(self):
        with self._cursor(database=DB_NAME) as cur:
            cur.execute("SELECT id, slug, year FROM ranking_lists")
            for row in cur.fetchall():
                self._list_cache[(row["slug"], row["year"])] = row["id"]

    def bulk_insert_universities(self, universities: List[Tuple[str, str, Optional[str], Optional[str], Optional[str], Optional[str]]]):
        if not universities:
            return
        with self._cursor(database=DB_NAME) as cur:
            cur.executemany(
                """
                INSERT INTO universities (url_id, name_zh, name_en, name_fanti, badge_url, detail_url)
                VALUES (%s, %s, %s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE
                    name_zh=VALUES(name_zh), name_en=VALUES(name_en),
                    name_fanti=VALUES(name_fanti), badge_url=VALUES(badge_url),
                    detail_url=VALUES(detail_url)
                """,
                universities,
            )
        self._refresh_university_cache()

    def _refresh_university_cache(self):
        with self._cursor(database=DB_NAME) as cur:
            cur.execute("SELECT id, url_id FROM universities")
            for row in cur.fetchall():
                self._univ_cache[row["url_id"]] = row["id"]

    def bulk_insert_tags(self, tags: List[Tuple[Optional[int], Optional[str], str, Optional[str], Optional[str], Optional[str], Optional[str]]]):
        if not tags:
            return
        with self._cursor(database=DB_NAME) as cur:
            cur.executemany(
                """
                INSERT INTO tags (external_id, url_id, name_zh, name_en, tag_type, parent_ids, icon_url)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE
                    external_id=VALUES(external_id), url_id=VALUES(url_id),
                    name_en=VALUES(name_en), tag_type=VALUES(tag_type),
                    parent_ids=VALUES(parent_ids), icon_url=VALUES(icon_url)
                """,
                tags,
            )
        self._refresh_tag_cache()

    def _refresh_tag_cache(self):
        with self._cursor(database=DB_NAME) as cur:
            cur.execute("SELECT id, name_zh, tag_type FROM tags")
            for row in cur.fetchall():
                self._tag_cache[(row["name_zh"], row["tag_type"])] = row["id"]

    def bulk_insert_university_tags(self, links: List[Tuple[int, int]]):
        if not links:
            return
        with self._cursor(database=DB_NAME) as cur:
            cur.executemany(
                "INSERT IGNORE INTO university_tags (university_id, tag_id) VALUES (%s, %s)",
                links,
            )

    def bulk_insert_ranking_entries(self, entries: List[Tuple[int, int, str, Optional[str], Optional[int], Optional[int], Optional[int], int, str, int]]):
        if not entries:
            return
        with self._cursor(database=DB_NAME) as cur:
            cur.executemany(
                """
                INSERT INTO ranking_entries
                    (list_id, university_id, rank_display, rank_raw, rank_int,
                     rank_range_start, rank_range_end, year, data_source, liked)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE
                    rank_display=IF(data_source='download_api', rank_display, VALUES(rank_display)),
                    rank_raw=IF(data_source='download_api', rank_raw, VALUES(rank_raw)),
                    rank_int=IF(data_source='download_api', rank_int, VALUES(rank_int)),
                    rank_range_start=IF(data_source='download_api', rank_range_start, VALUES(rank_range_start)),
                    rank_range_end=IF(data_source='download_api', rank_range_end, VALUES(rank_range_end)),
                    data_source=IF(data_source='download_api', data_source, VALUES(data_source)),
                    liked=IF(data_source='download_api', liked, VALUES(liked)),
                    crawled_at=VALUES(crawled_at)
                """,
                entries,
            )

    # --- cache accessors ---

    def get_system_id(self, slug: str) -> Optional[int]:
        return self._system_cache.get(slug)

    def get_list_id(self, slug: str, year: int) -> Optional[int]:
        return self._list_cache.get((slug, year))

    def get_university_id(self, url_id: str) -> Optional[int]:
        return self._univ_cache.get(url_id)

    def get_tag_id(self, name_zh: str, tag_type: Optional[str]) -> Optional[int]:
        return self._tag_cache.get((name_zh, tag_type))

    # --- single insert fallbacks ---

    def insert_system(self, slug: str, name_zh: str, name_en: Optional[str] = None,
                      category: Optional[str] = None) -> int:
        if slug in self._system_cache:
            return self._system_cache[slug]
        self.bulk_insert_systems([(slug, name_zh, name_en, category)])
        return self._system_cache[slug]

    def insert_list(self, system_id: int, slug: str, year: int,
                    name_zh: Optional[str] = None,
                    total_entities: Optional[int] = None,
                    url: Optional[str] = None) -> int:
        key = (slug, year)
        if key in self._list_cache:
            return self._list_cache[key]
        self.bulk_insert_lists([(system_id, slug, year, name_zh, total_entities, url)])
        return self._list_cache[key]

    def update_list_total(self, list_id: int, total_entities: int):
        """Update total_entities for an existing ranking list."""
        with self._cursor(database=DB_NAME) as cur:
            cur.execute(
                "UPDATE ranking_lists SET total_entities = %s WHERE id = %s",
                (total_entities, list_id),
            )

    def insert_university(self, url_id: str, name_zh: str, name_en: Optional[str] = None,
                          name_fanti: Optional[str] = None,
                          badge_url: Optional[str] = None,
                          detail_url: Optional[str] = None) -> int:
        if url_id in self._univ_cache:
            return self._univ_cache[url_id]
        self.bulk_insert_universities([(url_id, name_zh, name_en, name_fanti, badge_url, detail_url)])
        return self._univ_cache[url_id]

    def insert_tag(self, external_id: Optional[int], url_id: Optional[str],
                   name_zh: str, name_en: Optional[str] = None,
                   tag_type: Optional[str] = None,
                   parent_ids: Optional[str] = None,
                   icon_url: Optional[str] = None) -> int:
        key = (name_zh, tag_type)
        if key in self._tag_cache:
            return self._tag_cache[key]
        self.bulk_insert_tags([(external_id, url_id, name_zh, name_en, tag_type, parent_ids, icon_url)])
        return self._tag_cache[key]

    def link_university_tag(self, university_id: int, tag_id: int):
        self.bulk_insert_university_tags([(university_id, tag_id)])

    def insert_ranking_entry(self, list_id: int, university_id: int,
                             rank_display: str, rank_raw: Optional[str],
                             rank_int: Optional[int],
                             rank_range_start: Optional[int],
                             rank_range_end: Optional[int],
                             year: int,
                             data_source: str = "entity_api",
                             liked: bool = False):
        self.bulk_insert_ranking_entries([
            (list_id, university_id, rank_display, rank_raw, rank_int,
             rank_range_start, rank_range_end,
             year, data_source, int(liked))
        ])

    def insert_detail(self, university_id: int, page_html: Optional[str] = None,
                      description: Optional[str] = None,
                      website: Optional[str] = None,
                      location: Optional[str] = None,
                      founded_year: Optional[str] = None,
                      motto: Optional[str] = None,
                      address: Optional[str] = None):
        with self._cursor(database=DB_NAME) as cur:
            cur.execute(
                """
                INSERT INTO university_details
                    (university_id, page_html, description, website, location, founded_year, motto, address)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE
                    page_html=VALUES(page_html), description=VALUES(description),
                    website=VALUES(website), location=VALUES(location),
                    founded_year=VALUES(founded_year), motto=VALUES(motto),
                    address=VALUES(address), extracted_at=VALUES(extracted_at)
                """,
                (university_id, page_html, description, website, location, founded_year, motto, address),
            )

    def log(self, task: str, status: str, identifier: Optional[str] = None,
            message: Optional[str] = None):
        with self._cursor(database=DB_NAME) as cur:
            cur.execute(
                """
                INSERT INTO crawl_log (task, identifier, status, message)
                VALUES (%s, %s, %s, %s)
                """,
                (task, identifier, status, message),
            )

    def get_university_slugs_without_detail(self) -> List[Dict[str, Any]]:
        with self._cursor(database=DB_NAME) as cur:
            cur.execute(
                """
                SELECT u.id, u.url_id, u.detail_url
                FROM universities u
                LEFT JOIN university_details d ON u.id = d.university_id
                WHERE d.id IS NULL
                """
            )
            return list(cur.fetchall())

    def get_all_university_slugs(self) -> List[Dict[str, Any]]:
        with self._cursor(database=DB_NAME) as cur:
            cur.execute(
                """
                SELECT u.id, u.url_id, u.detail_url
                FROM universities u
                """
            )
            return list(cur.fetchall())

    def get_all_lists(self) -> List[Dict[str, Any]]:
        with self._cursor(database=DB_NAME) as cur:
            cur.execute("SELECT * FROM ranking_lists ORDER BY id")
            return list(cur.fetchall())

    def get_successful_list_crawls(self) -> List[Dict[str, Any]]:
        with self._cursor(database=DB_NAME) as cur:
            cur.execute(
                """
                SELECT identifier, MAX(started_at) as started_at
                FROM crawl_log
                WHERE task = 'crawl_list' AND status = 'success'
                GROUP BY identifier
                """
            )
            return list(cur.fetchall())

    def get_list_entry_counts(self) -> List[Dict[str, Any]]:
        """Return current entry counts per ranking list."""
        with self._cursor(database=DB_NAME) as cur:
            cur.execute(
                """
                SELECT l.id, l.slug, l.year, l.name_zh, l.total_entities,
                       COUNT(e.id) AS entry_count
                FROM ranking_lists l
                LEFT JOIN ranking_entries e ON l.id = e.list_id
                GROUP BY l.id, l.slug, l.year, l.name_zh, l.total_entities
                ORDER BY l.slug, l.year
                """
            )
            return list(cur.fetchall())

    def get_lists_without_entries(self) -> List[Dict[str, Any]]:
        """Return lists that have never produced any ranking entries."""
        with self._cursor(database=DB_NAME) as cur:
            cur.execute(
                """
                SELECT l.id, l.slug, l.year, l.name_zh
                FROM ranking_lists l
                LEFT JOIN ranking_entries e ON l.id = e.list_id
                WHERE e.id IS NULL
                ORDER BY l.slug, l.year
                """
            )
            return list(cur.fetchall())
