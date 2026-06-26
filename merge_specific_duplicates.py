#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
手动合并名称不完全一致但实际为同一所院校的重复记录。
"""

import pymysql
from contextlib import closing

DB_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "",
    "database": "choosephd",
    "charset": "utf8mb4",
}

# (canonical_url_id, [loser_url_id, ...])
MERGE_PLAN = [
    ("beihang-university", ["beihang-university-former-buaa"]),
    ("beijing-institute-of-technology", ["beijing-institute-technology"]),
    ("dalian-university-of-technology", ["dalian-university-technology"]),
    ("harbin-institute-of-technology", ["harbin-institute-technology"]),
    ("huazhong-university-of-science-and-technology", ["huazhong-university-science-technology"]),
    ("minzu-university-of-china", ["minzu-university-china"]),
    ("nankai-university", ["nankai-university-nku"]),
    ("northwest-a-f-university", ["northwest-agriculture-forestry-university"]),
    ("ocean-university-of-china", ["ocean-university-china"]),
    ("renmin-university-of-china", ["renmin-peoples-university-china"]),
    ("south-china-university-of-technology", ["south-china-university-technology"]),
    ("sun-yat-sen-university", ["sun-yat-sen-university-sysu"]),
    ("university-of-electronic-science-and-technology-of-china", ["university-electronic-science-technology-china"]),
]


def merge_group(cursor, canonical_id, losers):
    for loser in losers:
        # tag 关联
        cursor.execute(
            """
            INSERT IGNORE INTO university_tag_relation (university_id, tag_id)
            SELECT %s, tag_id FROM university_tag_relation WHERE university_id=%s
            """,
            (canonical_id, loser),
        )
        cursor.execute("DELETE FROM university_tag_relation WHERE university_id=%s", (loser,))

        # ranking_entry 去重后迁移
        cursor.execute(
            """
            DELETE re_loser FROM ranking_entry re_loser
            JOIN ranking_entry re_canonical
              ON re_loser.source_id = re_canonical.source_id
             AND re_loser.year = re_canonical.year
             AND (re_loser.subject_id = re_canonical.subject_id
                  OR (re_loser.subject_id IS NULL AND re_canonical.subject_id IS NULL))
            WHERE re_loser.university_id = %s
              AND re_canonical.university_id = %s
            """,
            (loser, canonical_id),
        )
        cursor.execute("UPDATE ranking_entry SET university_id=%s WHERE university_id=%s", (canonical_id, loser))

        # alias
        cursor.execute("UPDATE IGNORE university_alias SET target_url_id=%s WHERE target_url_id=%s", (canonical_id, loser))
        cursor.execute("DELETE FROM university_alias WHERE target_url_id=%s", (loser,))

        # shortlist
        cursor.execute(
            """
            INSERT IGNORE INTO user_shortlist (user_id, university_id, note)
            SELECT user_id, %s, note FROM user_shortlist WHERE university_id=%s
            """,
            (canonical_id, loser),
        )
        cursor.execute("DELETE FROM user_shortlist WHERE university_id=%s", (loser,))

        # soft delete
        cursor.execute("UPDATE university SET deleted=1, updated_at=NOW() WHERE url_id=%s", (loser,))


def main(dry_run=True):
    with closing(pymysql.connect(**DB_CONFIG)) as conn:
        with conn.cursor() as cursor:
            for canonical, losers in MERGE_PLAN:
                cursor.execute("SELECT 1 FROM university WHERE url_id=%s AND deleted=0", (canonical,))
                if not cursor.fetchone():
                    print(f"跳过: canonical {canonical} 不存在或已删除")
                    continue

                valid_losers = []
                for loser in losers:
                    cursor.execute("SELECT 1 FROM university WHERE url_id=%s AND deleted=0", (loser,))
                    if cursor.fetchone():
                        valid_losers.append(loser)

                if not valid_losers:
                    continue

                print(f"[{'DRY-RUN' if dry_run else 'MERGE'}] {canonical} <- {valid_losers}")
                if not dry_run:
                    merge_group(cursor, canonical, valid_losers)

            if not dry_run:
                conn.commit()


if __name__ == "__main__":
    import sys

    dry = "--apply" not in sys.argv
    if dry:
        print("== 模拟运行（dry-run），加 --apply 执行实际合并 ==\n")
    main(dry_run=dry)
