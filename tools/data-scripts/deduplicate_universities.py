#!/usr/bin/env python3
"""
Deduplicate choosephd.university entries that share the same Chinese name and country.
Merges ranking entries under the canonical url_id, deletes duplicate rows, and records aliases.
"""
import logging
import re
import sys
from collections import defaultdict
from typing import Dict, List, Optional, Tuple

import pymysql
from pymysql.cursors import DictCursor

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(message)s")
logger = logging.getLogger(__name__)

SOURCE = {"host": "localhost", "port": 3306, "user": "root", "password": "", "database": "choosephd", "charset": "utf8mb4"}


def connect():
    return pymysql.connect(cursorclass=DictCursor, **SOURCE)


def normalize(text: Optional[str]) -> str:
    return (text or "").strip()


def slug_quality(url_id: str) -> int:
    """Lower score = better canonical slug."""
    score = 0
    # Prefer ASCII-ish, shorter, no trailing abbreviations like -bmsu if duplicated
    if re.search(r"[^\x00-\x7f]", url_id):
        score += 100
    score += len(url_id)
    return score


def choose_canonical(url_ids: List[str], ranking_counts: Dict[str, int]) -> str:
    """Prefer more ranking entries, then better slug."""
    def sort_key(u: str) -> Tuple[int, int, str]:
        return (-ranking_counts.get(u, 0), slug_quality(u), u)
    return sorted(url_ids, key=sort_key)[0]


def main(dry_run: bool = False):
    conn = connect()
    try:
        # 1. Load all universities
        with conn.cursor() as cur:
            cur.execute("SELECT url_id, name_zh, country FROM university WHERE deleted = 0")
            rows = cur.fetchall()

        # 2. Group by (name_zh, country)
        groups: Dict[Tuple[str, str], List[str]] = defaultdict(list)
        mixed_country_groups: Dict[str, List[Tuple[str, str, str]]] = defaultdict(list)
        for r in rows:
            name = normalize(r["name_zh"])
            country = normalize(r["country"])
            if not name:
                continue
            if country:
                groups[(name, country)].append(r["url_id"])
            mixed_country_groups[name].append((r["url_id"], country, r["url_id"]))

        # 3. Find mergeable groups (same name_zh + same country, size > 1)
        mergeable = {k: v for k, v in groups.items() if len(v) > 1}

        # 4. Load ranking entry counts
        all_url_ids = {uid for g in mergeable.values() for uid in g}
        ranking_counts: Dict[str, int] = defaultdict(int)
        with conn.cursor() as cur:
            if all_url_ids:
                fmt = ",".join(["%s"] * len(all_url_ids))
                cur.execute(f"SELECT university_id, COUNT(*) AS c FROM ranking_entry WHERE deleted=0 AND university_id IN ({fmt}) GROUP BY university_id", tuple(all_url_ids))
                for row in cur.fetchall():
                    ranking_counts[row["university_id"]] = row["c"]

        # 5. Process merges
        merged = 0
        aliases = 0
        deleted = 0
        ranking_moved = 0

        for (name, country), url_ids in mergeable.items():
            canonical = choose_canonical(url_ids, ranking_counts)
            others = [u for u in url_ids if u != canonical]
            logger.info("Merging %s (%s) -> canonical %s, dupes: %s", name, country, canonical, others)

            if dry_run:
                continue

            with conn.cursor() as cur:
                for other in others:
                    # a) Move ranking entries, resolving natural-key conflicts
                    cur.execute("""
                        SELECT id, source_id, subject_id, year, rank_value
                        FROM ranking_entry
                        WHERE deleted=0 AND university_id = %s
                    """, (other,))
                    entries = cur.fetchall()

                    kept_ids = set()
                    for e in entries:
                        # Check if canonical already has this natural key
                        cur.execute("""
                            SELECT id, rank_value FROM ranking_entry
                            WHERE deleted=0 AND university_id = %s AND source_id = %s AND subject_id <=> %s AND year = %s
                        """, (canonical, e["source_id"], e["subject_id"], e["year"]))
                        existing = cur.fetchone()

                        if existing:
                            # Keep the better (lower) rank_value; delete duplicate
                            e_val = e["rank_value"] if e["rank_value"] is not None else 999999
                            ex_val = existing["rank_value"] if existing["rank_value"] is not None else 999999
                            if e_val < ex_val:
                                cur.execute("UPDATE ranking_entry SET deleted=1 WHERE id = %s", (existing["id"],))
                                cur.execute("UPDATE ranking_entry SET university_id = %s WHERE id = %s", (canonical, e["id"]))
                                kept_ids.add(e["id"])
                            else:
                                cur.execute("UPDATE ranking_entry SET deleted=1 WHERE id = %s", (e["id"],))
                        else:
                            cur.execute("UPDATE ranking_entry SET university_id = %s WHERE id = %s", (canonical, e["id"]))
                            kept_ids.add(e["id"])

                    ranking_moved += len(kept_ids)

                    # b) Record alias
                    cur.execute("""
                        INSERT INTO university_alias (alias_url_id, target_url_id)
                        VALUES (%s, %s)
                        ON DUPLICATE KEY UPDATE target_url_id = VALUES(target_url_id)
                    """, (other, canonical))
                    aliases += 1

                    # c) Delete duplicate university (soft delete)
                    cur.execute("UPDATE university SET deleted=1 WHERE url_id = %s", (other,))
                    deleted += 1

                merged += 1

        if not dry_run:
            conn.commit()

        logger.info("Merged %d groups, deleted %d duplicate universities, created %d aliases, moved %d ranking entries", merged, deleted, aliases, ranking_moved)

        # Report mixed-country duplicates for manual review
        mixed = {name: items for name, items in mixed_country_groups.items() if len({c for _, c, _ in items}) > 1}
        logger.info("Mixed-country duplicate names (manual review needed): %d", len(mixed))
        if mixed:
            sample = list(mixed.items())[:10]
            for name, items in sample:
                logger.info("  %s: %s", name, ", ".join(f"{u}({c})" for u, c, _ in items))

    finally:
        conn.close()


if __name__ == "__main__":
    dry = "--dry-run" in sys.argv
    main(dry_run=dry)
