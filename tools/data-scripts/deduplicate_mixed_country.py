#!/usr/bin/env python3
"""
Handle mixed-country duplicates where one entry is (incorrectly) tagged as 中国.
Merge the 中国 entry into the non-China entry.
"""
import logging
import sys
from collections import defaultdict
from typing import Dict, List, Tuple

import pymysql
from pymysql.cursors import DictCursor

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(message)s")
logger = logging.getLogger(__name__)

SOURCE = {"host": "localhost", "port": 3306, "user": "root", "password": "", "database": "choosephd", "charset": "utf8mb4"}


def connect():
    return pymysql.connect(cursorclass=DictCursor, **SOURCE)


def normalize(t):
    return (t or "").strip()


def main(dry_run: bool = False):
    conn = connect()
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT url_id, name_zh, country FROM university WHERE deleted = 0 AND name_zh IS NOT NULL AND name_zh != ''")
            rows = cur.fetchall()

        groups: Dict[str, List[Tuple[str, str]]] = defaultdict(list)
        for r in rows:
            groups[normalize(r["name_zh"])].append((r["url_id"], normalize(r["country"])))

        merged = deleted = aliases = ranking_moved = 0

        for name, items in groups.items():
            if len(items) <= 1:
                continue
            countries = {c for _, c in items}
            if len(countries) <= 1:
                continue
            # Only handle cases where exactly one is 中国 and the rest are not
            china_items = [u for u, c in items if c == "中国"]
            non_china_items = [(u, c) for u, c in items if c != "中国"]
            if len(china_items) != 1 or len(non_china_items) != 1:
                continue

            china_url = china_items[0]
            canonical, canonical_country = non_china_items[0]

            logger.info("Merging %s: %s(中国) -> %s(%s)", name, china_url, canonical, canonical_country)

            if dry_run:
                continue

            with conn.cursor() as cur:
                # Move ranking entries
                cur.execute("SELECT id, source_id, subject_id, year, rank_value FROM ranking_entry WHERE deleted=0 AND university_id = %s", (china_url,))
                entries = cur.fetchall()
                for e in entries:
                    cur.execute("""
                        SELECT id, rank_value FROM ranking_entry
                        WHERE deleted=0 AND university_id = %s AND source_id = %s AND subject_id <=> %s AND year = %s
                    """, (canonical, e["source_id"], e["subject_id"], e["year"]))
                    existing = cur.fetchone()
                    if existing:
                        e_val = e["rank_value"] if e["rank_value"] is not None else 999999
                        ex_val = existing["rank_value"] if existing["rank_value"] is not None else 999999
                        if e_val < ex_val:
                            cur.execute("UPDATE ranking_entry SET deleted=1 WHERE id = %s", (existing["id"],))
                            cur.execute("UPDATE ranking_entry SET university_id = %s WHERE id = %s", (canonical, e["id"]))
                            ranking_moved += 1
                        else:
                            cur.execute("UPDATE ranking_entry SET deleted=1 WHERE id = %s", (e["id"],))
                    else:
                        cur.execute("UPDATE ranking_entry SET university_id = %s WHERE id = %s", (canonical, e["id"]))
                        ranking_moved += 1

                cur.execute("""
                    INSERT INTO university_alias (alias_url_id, target_url_id)
                    VALUES (%s, %s)
                    ON DUPLICATE KEY UPDATE target_url_id = VALUES(target_url_id)
                """, (china_url, canonical))
                cur.execute("UPDATE university SET deleted=1 WHERE url_id = %s", (china_url,))
                aliases += 1
                deleted += 1
                merged += 1

        if not dry_run:
            conn.commit()

        logger.info("Merged %d mixed-country groups, deleted %d, aliases %d, moved %d ranking entries", merged, deleted, aliases, ranking_moved)

    finally:
        conn.close()


if __name__ == "__main__":
    dry = "--dry-run" in sys.argv
    main(dry_run=dry)
