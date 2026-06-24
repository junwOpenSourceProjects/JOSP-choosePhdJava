#!/usr/bin/env python3
"""
Import crawled menggy ranking data into the choosePhd project database.

Strategy:
- Read from `menggy_rankings` (source) and write to `choosephd` (target).
- Upsert universities by url_id, filling only empty fields.
- Upsert ranking_source by slug and subject by slug, preserving existing IDs.
- For ranking_entry, use the project's natural-key dedup: delete existing
  (source_id, subject_id, year) slice, then insert menggy entries for that slice.
"""

import argparse
import logging
import re
import sys
import time
from typing import Dict, List, Optional, Tuple

import pymysql
from pymysql.cursors import DictCursor

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
SOURCE_DB = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "",
    "database": "menggy_rankings",
    "charset": "utf8mb4",
}

TARGET_DB = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "",
    "database": "choosephd",
    "charset": "utf8mb4",
}

CHINESE_PROVINCES = {
    "山东", "山西", "江苏", "河南", "新疆", "河北", "湖北", "湖南", "广东", "广西",
    "海南", "四川", "贵州", "云南", "西藏", "陕西", "甘肃", "青海", "辽宁", "吉林",
    "黑龙江", "内蒙古", "宁夏", "北京", "天津", "上海", "重庆", "香港", "澳门", "台湾",
}
CONTINENT_NAMES = {"亚洲", "欧洲", "北美洲", "非洲", "南美洲", "大洋洲", "南极洲"}
NON_COUNTRY_NAMES = {
    "综合类", "师范类", "理工类", "政法类", "艺术类", "医药类", "财经类",
    "体育类", "农林类", "民族类", "语言类", "军事类",
} | CONTINENT_NAMES | CHINESE_PROVINCES

SUBJECT_PARENTS = (
    "qs-university-subject-rankings",
    "arwu-university-subject-rankings",
    "the-university-subject-rankings",
    "usnews-university-subject-rankings",
    "rur-university-subject-rankings",
)

HTML_TAG_RE = re.compile(r"<[^>]+>")
RANGE_RE = re.compile(r"^(\d+)\s*[-–—]\s*(\d+)$")


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
def connect(cfg: dict):
    return pymysql.connect(
        host=cfg["host"],
        port=cfg["port"],
        user=cfg["user"],
        password=cfg["password"],
        database=cfg["database"],
        charset=cfg["charset"],
        cursorclass=DictCursor,
        autocommit=False,
    )


def detect_kind(source_slug: str, subject_slug: Optional[str]) -> int:
    """Mirror of SourceResolver.detectKind."""
    if subject_slug is not None:
        return 3
    lower = source_slug.lower()
    if lower.startswith("growth-trend-") or lower.startswith("declining-trend-"):
        return 4
    region_keywords = (
        "-asia-", "-europe-", "-north-america-", "-oceania-", "-africa-",
        "-latin-america-", "-japan-", "-national-",
    )
    region_suffixes = (
        "-asia", "-europe", "-north-america", "-oceania", "-africa",
        "-latin-america", "-japan", "-national",
    )
    if any(k in lower for k in region_keywords) or lower.endswith(region_suffixes):
        return 2
    return 1


def extract_owner_org(source_slug: str) -> str:
    """Mirror of existing data conventions (trend sources are 'Other')."""
    lower = source_slug.lower()
    if lower.startswith("menggy"):
        return "Menggy"
    if lower.startswith("qs"):
        return "QS"
    if lower.startswith("the"):
        return "THE"
    if lower.startswith("arwu"):
        return "ARWU"
    if lower.startswith("usnews"):
        return "USN"
    if lower.startswith("cwur"):
        return "CWUR"
    if lower.startswith("rur"):
        return "RUR"
    if lower.startswith("edurank"):
        return "EduRank"
    if lower.startswith("mosiur"):
        return "Mosiur"
    return "Other"


def extract_subject_slug(source_slug: str) -> Optional[str]:
    for parent in SUBJECT_PARENTS:
        if source_slug.startswith(parent + "-"):
            return source_slug[len(parent) + 1 :]
    return None


def format_source_name(slug: str) -> str:
    return slug.replace("-", " ").upper()


def format_subject_name(slug: str) -> str:
    return slug.replace("-", " ")


def clean_country(name: Optional[str]) -> Optional[str]:
    if not name:
        return None
    name = name.strip()
    if name in NON_COUNTRY_NAMES:
        return None
    return name if name else None


def clean_region(name: Optional[str]) -> Optional[str]:
    if not name:
        return None
    name = name.strip()
    return name if name in CONTINENT_NAMES else None


def compute_rank_value(rank_display: Optional[str], rank_raw: Optional[str]) -> Optional[int]:
    """Compute rank_value matching project conventions."""
    display = (rank_display or "").strip()
    raw = (rank_raw or "").strip()

    # Reporter / Unranked / infinity icon
    if not display or "Reporter" in raw or "Unranked" in raw or "infinity" in display:
        return 9999

    # Strip HTML (trend arrows etc.)
    display = HTML_TAG_RE.sub("", display).strip()
    raw = HTML_TAG_RE.sub("", raw).strip()

    # >1k
    if display.startswith(">"):
        return None

    # Try range from raw first (authoritative)
    for text in (raw, display):
        text = text.replace("#", "").replace("=", "").strip()
        m = RANGE_RE.match(text)
        if m:
            start, end = int(m.group(1)), int(m.group(2))
            return (start + end) // 2
        # Try plain integer
        try:
            return int(text)
        except ValueError:
            continue

    return None


# ---------------------------------------------------------------------------
# Loaders
# ---------------------------------------------------------------------------
def load_target_caches(target_conn):
    caches = {
        "source_by_slug": {},
        "subject_by_slug": {},
        "university_by_url_id": {},
    }
    with target_conn.cursor() as cur:
        cur.execute("SELECT id, slug FROM ranking_source WHERE deleted = 0")
        for row in cur.fetchall():
            caches["source_by_slug"][row["slug"]] = row["id"]

        cur.execute("SELECT id, slug, name_zh FROM subject WHERE deleted = 0")
        for row in cur.fetchall():
            caches["subject_by_slug"][row["slug"]] = {
                "id": row["id"],
                "name_zh": row["name_zh"],
            }

        cur.execute(
            "SELECT url_id, name_zh, name_en, name_zh_tw, country, region, badge_url, website, motto, founded_date, address FROM university WHERE deleted = 0"
        )
        for row in cur.fetchall():
            caches["university_by_url_id"][row["url_id"]] = row
    return caches


def load_menggy_lists(source_conn) -> Dict[int, dict]:
    """Return list_id -> {id, slug, year, system_slug, name_zh}."""
    lists = {}
    with source_conn.cursor() as cur:
        cur.execute("""
            SELECT l.id, l.slug, l.year, l.name_zh, s.slug AS system_slug
            FROM ranking_lists l
            JOIN ranking_systems s ON l.system_id = s.id
        """)
        for row in cur.fetchall():
            lists[row["id"]] = row
    return lists


def load_menggy_university_tags(source_conn) -> Tuple[Dict[str, str], Dict[str, str]]:
    """Return (url_id -> country, url_id -> region).

    menggy tags are not consistently typed: some country/region tags use
    tag_type='other'. We prefer explicitly typed tags and fall back to
    'other' tags that look like country or region names.
    """
    country_map: Dict[str, str] = {}
    region_map: Dict[str, str] = {}
    other_tags: Dict[str, List[str]] = {}

    with source_conn.cursor() as cur:
        cur.execute("""
            SELECT u.url_id, t.name_zh, t.tag_type
            FROM university_tags ut
            JOIN universities u ON ut.university_id = u.id
            JOIN tags t ON ut.tag_id = t.id
            WHERE t.tag_type IN ('country', 'region', 'other')
        """)
        for row in cur.fetchall():
            url_id = row["url_id"]
            name = row["name_zh"]
            ttype = row["tag_type"]
            if ttype == "country":
                country_map[url_id] = name
            elif ttype == "region":
                region_map[url_id] = name
            else:
                other_tags.setdefault(url_id, []).append(name)

    # Fallback: use 'other' tags that are valid country/region names
    for url_id, names in other_tags.items():
        if url_id not in country_map:
            for name in names:
                if clean_country(name):
                    country_map[url_id] = name
                    break
        if url_id not in region_map:
            for name in names:
                if clean_region(name):
                    region_map[url_id] = name
                    break

    return country_map, region_map


# ---------------------------------------------------------------------------
# Transform & write
# ---------------------------------------------------------------------------
def upsert_sources(target_conn, source_slugs: List[str], caches: dict) -> Dict[str, int]:
    """Insert missing ranking_source rows, return slug -> id mapping."""
    result = dict(caches["source_by_slug"])
    new_sources = []
    for slug in source_slugs:
        if slug in result:
            continue
        kind = detect_kind(slug, extract_subject_slug(slug))
        owner_org = extract_owner_org(slug)
        name_zh = format_source_name(slug)
        new_sources.append((slug, name_zh, kind, owner_org))

    if not new_sources:
        logger.info("No new ranking_source rows to insert")
        return result

    with target_conn.cursor() as cur:
        cur.executemany(
            """
            INSERT INTO ranking_source (slug, name_zh, kind, owner_org, active, deleted)
            VALUES (%s, %s, %s, %s, 1, 0)
            ON DUPLICATE KEY UPDATE
                name_zh = VALUES(name_zh),
                kind = VALUES(kind),
                owner_org = VALUES(owner_org),
                active = 1,
                deleted = 0
            """,
            new_sources,
        )
        target_conn.commit()

    # Refresh cache
    with target_conn.cursor() as cur:
        cur.execute("SELECT id, slug FROM ranking_source")
        for row in cur.fetchall():
            result[row["slug"]] = row["id"]

    logger.info("Upserted %d ranking_source rows", len(new_sources))
    return result


def upsert_subjects(target_conn, subject_slugs: Dict[str, str], caches: dict) -> Dict[str, int]:
    """subject_slugs: slug -> preferred name_zh. Insert missing, return slug -> id."""
    result = {slug: info["id"] for slug, info in caches["subject_by_slug"].items()}
    new_subjects = []
    for slug, name_zh in subject_slugs.items():
        if slug in result:
            continue
        new_subjects.append((slug, name_zh or format_subject_name(slug), extract_owner_org(slug)))

    if not new_subjects:
        logger.info("No new subject rows to insert")
        return result

    with target_conn.cursor() as cur:
        cur.executemany(
            """
            INSERT INTO subject (slug, name_zh, name_en, owner_org, active, deleted)
            VALUES (%s, %s, %s, %s, 1, 0)
            ON DUPLICATE KEY UPDATE
                name_zh = VALUES(name_zh),
                owner_org = VALUES(owner_org),
                active = 1,
                deleted = 0
            """,
            [(s, n, s.replace("-", " "), o) for s, n, o in new_subjects],
        )
        target_conn.commit()

    with target_conn.cursor() as cur:
        cur.execute("SELECT id, slug FROM subject")
        for row in cur.fetchall():
            result[row["slug"]] = row["id"]

    logger.info("Upserted %d subject rows", len(new_subjects))
    return result


def _clean_detail_value(value: Optional[str], max_len: int = 512) -> Optional[str]:
    if not value:
        return None
    text = value.strip()
    if not text or text == ".":
        return None
    return text[:max_len] or None


def load_menggy_university_details(source_conn) -> Dict[str, Dict[str, Optional[str]]]:
    """Return url_id -> {website, motto, founded_date, address}."""
    details: Dict[str, Dict[str, Optional[str]]] = {}
    with source_conn.cursor() as cur:
        cur.execute("""
            SELECT u.url_id, d.website, d.motto, d.founded_year, d.address
            FROM universities u
            JOIN university_details d ON u.id = d.university_id
        """)
        for row in cur.fetchall():
            url_id = row["url_id"].lower().strip()
            details[url_id] = {
                "website": _clean_detail_value(row["website"], 512),
                "motto": _clean_detail_value(row["motto"], 512),
                "founded_date": _clean_detail_value(row["founded_year"], 32),
                "address": _clean_detail_value(row["address"], 512),
            }
    return details


def upsert_universities(target_conn, source_conn, caches: dict) -> None:
    """Upsert all menggy universities into choosephd."""
    country_map, region_map = load_menggy_university_tags(source_conn)
    detail_map = load_menggy_university_details(source_conn)
    existing = caches["university_by_url_id"]

    inserts = []
    updates = []

    with source_conn.cursor() as cur:
        cur.execute("SELECT url_id, name_zh, name_en, name_fanti, badge_url FROM universities")
        for row in cur.fetchall():
            url_id = row["url_id"].lower().strip()
            name_zh = (row["name_zh"] or "").strip() or None
            name_en = (row["name_en"] or "").strip() or None
            name_fanti = (row["name_fanti"] or "").strip() or None
            badge_url = (row["badge_url"] or "").strip() or None
            country = clean_country(country_map.get(url_id))
            region = clean_region(region_map.get(url_id))
            detail = detail_map.get(url_id, {})

            if url_id in existing:
                ex = existing[url_id]
                upd_fields = {}
                # Fill only empty fields to match existing importer behavior
                if not ex.get("country") and country:
                    upd_fields["country"] = country
                if not ex.get("region") and region:
                    upd_fields["region"] = region
                if not ex.get("name_zh_tw") and name_fanti:
                    upd_fields["name_zh_tw"] = name_fanti
                if not ex.get("badge_url") and badge_url:
                    upd_fields["badge_url"] = badge_url
                # Always keep name_zh/en from menggy if present (authoritative)
                if name_zh:
                    upd_fields["name_zh"] = name_zh
                if name_en:
                    upd_fields["name_en"] = name_en
                # Overwrite detail fields with menggy values (crawled source is authoritative)
                for field, value in detail.items():
                    if value:
                        upd_fields[field] = value
                if upd_fields:
                    updates.append((upd_fields, url_id))
            else:
                inserts.append((
                    url_id,
                    name_zh or name_en or url_id,
                    name_en or name_zh or url_id,
                    name_fanti,
                    country or "unknown",
                    region or "unknown",
                    badge_url,
                    detail.get("website"),
                    detail.get("motto"),
                    detail.get("founded_date"),
                    detail.get("address"),
                ))

    with target_conn.cursor() as cur:
        if inserts:
            cur.executemany(
                """
                INSERT INTO university (url_id, name_zh, name_en, name_zh_tw, country, region, badge_url,
                                      website, motto, founded_date, address, deleted)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, 0)
                ON DUPLICATE KEY UPDATE
                    name_zh = VALUES(name_zh),
                    name_en = VALUES(name_en),
                    name_zh_tw = VALUES(name_zh_tw),
                    country = VALUES(country),
                    region = VALUES(region),
                    badge_url = VALUES(badge_url),
                    website = VALUES(website),
                    motto = VALUES(motto),
                    founded_date = VALUES(founded_date),
                    address = VALUES(address),
                    deleted = 0
                """,
                inserts,
            )
        for fields, url_id in updates:
            cols = ", ".join(f"{k} = %s" for k in fields)
            cur.execute(f"UPDATE university SET {cols} WHERE url_id = %s", list(fields.values()) + [url_id])
        target_conn.commit()

    logger.info("Upserted universities: %d inserts, %d updates", len(inserts), len(updates))


def import_ranking_entries(source_conn, target_conn, lists: Dict[int, dict],
                           source_id_map: Dict[str, int], subject_id_map: Dict[str, int]) -> None:
    """Import ranking entries by (source_slug, year) slices."""
    # Group list_ids by source_slug
    source_years: Dict[str, List[int]] = {}
    for lst in lists.values():
        source_years.setdefault(lst["slug"], []).append(lst["year"])

    total_inserted = 0
    total_deleted = 0
    source_counter = 0

    for source_slug, years in source_years.items():
        source_counter += 1
        source_id = source_id_map[source_slug]
        subject_slug = extract_subject_slug(source_slug)
        subject_id = subject_id_map.get(subject_slug) if subject_slug else None

        for year in set(years):
            # 1. Delete existing slice
            with target_conn.cursor() as cur:
                if subject_id is not None:
                    cur.execute(
                        """
                        DELETE FROM ranking_entry
                        WHERE source_id = %s AND subject_id = %s AND year = %s
                        """,
                        (source_id, subject_id, year),
                    )
                else:
                    cur.execute(
                        """
                        DELETE FROM ranking_entry
                        WHERE source_id = %s AND subject_id IS NULL AND year = %s
                        """,
                        (source_id, year),
                    )
                deleted = cur.rowcount
                total_deleted += deleted

            # 2. Fetch menggy entries for this slice
            entries = []
            with source_conn.cursor() as cur:
                cur.execute(
                    """
                    SELECT u.url_id, e.rank_display, e.rank_raw
                    FROM ranking_entries e
                    JOIN ranking_lists l ON e.list_id = l.id
                    JOIN universities u ON e.university_id = u.id
                    WHERE l.slug = %s AND l.year = %s
                    """,
                    (source_slug, year),
                )
                for row in cur.fetchall():
                    url_id = row["url_id"].lower().strip()
                    rank_display = HTML_TAG_RE.sub("", row["rank_display"] or "").strip()
                    if not rank_display:
                        rank_display = "-"
                    rank_value = compute_rank_value(row["rank_display"], row["rank_raw"])
                    entries.append((
                        url_id,
                        source_id,
                        subject_id,
                        year,
                        rank_display,
                        rank_value,
                    ))

            # 3. Insert slice
            if entries:
                with target_conn.cursor() as cur:
                    cur.executemany(
                        """
                        INSERT INTO ranking_entry
                        (university_id, source_id, subject_id, year, rank_display, rank_value, rank_delta, direction, deleted)
                        VALUES (%s, %s, %s, %s, %s, %s, NULL, NULL, 0)
                        """,
                        entries,
                    )
                    target_conn.commit()
                total_inserted += len(entries)

        if source_counter % 10 == 0:
            logger.info(
                "Progress: %d/%d sources processed, inserted %d, deleted %d",
                source_counter, len(source_years), total_inserted, total_deleted,
            )

    logger.info(
        "Ranking entries import complete: inserted %d, deleted %d",
        total_inserted, total_deleted,
    )


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main():
    parser = argparse.ArgumentParser(description="Import menggy ranking data into choosephd")
    parser.add_argument("--skip-rankings", action="store_true", help="Skip ranking entries import")
    args = parser.parse_args()

    start = time.time()
    source_conn = connect(SOURCE_DB)
    target_conn = connect(TARGET_DB)
    logger.info("Connected to source=%s target=%s", SOURCE_DB["database"], TARGET_DB["database"])

    try:
        # Load caches
        caches = load_target_caches(target_conn)
        logger.info(
            "Target caches: %d sources, %d subjects, %d universities",
            len(caches["source_by_slug"]),
            len(caches["subject_by_slug"]),
            len(caches["university_by_url_id"]),
        )

        # Load menggy list metadata
        lists = load_menggy_lists(source_conn)
        logger.info("Loaded %d menggy ranking lists", len(lists))

        # Prepare source/subject mappings
        source_slugs = sorted({lst["slug"] for lst in lists.values()})
        subject_slugs: Dict[str, str] = {}
        for slug in source_slugs:
            sub = extract_subject_slug(slug)
            if sub:
                subject_slugs[sub] = None  # name_zh filled later

        # Upsert sources
        source_id_map = upsert_sources(target_conn, source_slugs, caches)

        # Upsert subjects
        subject_id_map = upsert_subjects(target_conn, subject_slugs, caches)

        # Upsert universities
        upsert_universities(target_conn, source_conn, caches)

        # Import ranking entries
        if not args.skip_rankings:
            import_ranking_entries(source_conn, target_conn, lists, source_id_map, subject_id_map)
        else:
            logger.info("Skipping ranking entries import")

    finally:
        source_conn.close()
        target_conn.close()

    logger.info("Total elapsed: %.1f seconds", time.time() - start)


if __name__ == "__main__":
    main()
