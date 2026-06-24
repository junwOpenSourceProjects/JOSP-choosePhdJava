#!/usr/bin/env python3
"""One-time cleanup: fill unknown country/region from menggy 'other' tags."""
import logging
import sys
from typing import Dict, List, Tuple

import pymysql
from pymysql.cursors import DictCursor

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(message)s")
logger = logging.getLogger(__name__)

SOURCE = {"host": "localhost", "port": 3306, "user": "root", "password": "", "database": "menggy_rankings", "charset": "utf8mb4"}
TARGET = {"host": "localhost", "port": 3306, "user": "root", "password": "", "database": "choosephd", "charset": "utf8mb4"}

CONTINENTS = {"亚洲", "欧洲", "北美洲", "非洲", "南美洲", "大洋洲", "南极洲"}
PROVINCES = {
    "山东", "山西", "江苏", "河南", "新疆", "河北", "湖北", "湖南", "广东", "广西", "海南", "四川", "贵州", "云南", "西藏",
    "陕西", "甘肃", "青海", "辽宁", "吉林", "黑龙江", "内蒙古", "宁夏", "北京", "天津", "上海", "重庆", "香港", "澳门", "台湾",
}
NON_COUNTRY = {"综合类", "师范类", "理工类", "政法类", "艺术类", "医药类", "财经类", "体育类", "农林类", "民族类", "语言类", "军事类"} | CONTINENTS | PROVINCES


def clean_country(name):
    name = (name or "").strip()
    return name if name and name not in NON_COUNTRY else None


def clean_region(name):
    name = (name or "").strip()
    return name if name in CONTINENTS else None


def load_menggy_other_tags(conn) -> Tuple[Dict[str, List[str]], Dict[str, List[str]]]:
    country_opts: Dict[str, List[str]] = {}
    region_opts: Dict[str, List[str]] = {}
    with conn.cursor() as cur:
        cur.execute("""
            SELECT u.url_id, t.name_zh
            FROM university_tags ut
            JOIN universities u ON ut.university_id = u.id
            JOIN tags t ON ut.tag_id = t.id
            WHERE t.tag_type = 'other'
        """)
        for row in cur.fetchall():
            uid = row["url_id"].lower().strip()
            name = row["name_zh"]
            if clean_country(name):
                country_opts.setdefault(uid, []).append(name)
            if clean_region(name):
                region_opts.setdefault(uid, []).append(name)
    return country_opts, region_opts


def main():
    src = pymysql.connect(cursorclass=DictCursor, **SOURCE)
    tgt = pymysql.connect(cursorclass=DictCursor, **TARGET)

    country_opts, region_opts = load_menggy_other_tags(src)

    updated = 0
    with tgt.cursor() as cur:
        cur.execute("SELECT url_id, country, region FROM university WHERE deleted = 0 AND (country = 'unknown' OR region = 'unknown')")
        for row in cur.fetchall():
            uid = row["url_id"]
            new_country = None
            new_region = None
            if row["country"] == "unknown":
                opts = country_opts.get(uid, [])
                for name in opts:
                    if clean_country(name):
                        new_country = name
                        break
            if row["region"] == "unknown":
                opts = region_opts.get(uid, [])
                for name in opts:
                    if clean_region(name):
                        new_region = name
                        break
            if new_country or new_region:
                sets = []
                vals = []
                if new_country:
                    sets.append("country = %s")
                    vals.append(new_country)
                if new_region:
                    sets.append("region = %s")
                    vals.append(new_region)
                cur.execute(f"UPDATE university SET {', '.join(sets)} WHERE url_id = %s", vals + [uid])
                updated += 1
        tgt.commit()

    logger.info("Updated %d universities with country/region from 'other' tags", updated)
    src.close()
    tgt.close()


if __name__ == "__main__":
    main()
