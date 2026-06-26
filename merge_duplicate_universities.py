#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
合并 university 表中的重复院校记录。

策略：
- 以 name_en + country 为重复判定键。
- 每组保留一条“规范记录”（canonical），规则：
  1) 已有关联 tag 数量最多；
  2) 已有 ranking_entry 数量最多；
  3) 已有 user_shortlist 数量最多；
  4) url_id 更短 / 更规范。
- 将其余记录的 tag 关联、ranking_entry、university_alias、user_shortlist 迁移到规范记录。
- 软删除（deleted=1）其余记录。

执行前请自行备份数据库。
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


def count_refs(cursor, url_id):
    cursor.execute(
        "SELECT COUNT(*) FROM university_tag_relation WHERE university_id=%s", (url_id,)
    )
    tags = cursor.fetchone()[0]
    cursor.execute(
        "SELECT COUNT(*) FROM ranking_entry WHERE university_id=%s", (url_id,)
    )
    rankings = cursor.fetchone()[0]
    cursor.execute(
        "SELECT COUNT(*) FROM user_shortlist WHERE university_id=%s", (url_id,)
    )
    shortlists = cursor.fetchone()[0]
    cursor.execute(
        "SELECT COUNT(*) FROM university_alias WHERE target_url_id=%s", (url_id,)
    )
    aliases = cursor.fetchone()[0]
    return tags, rankings, shortlists, aliases


def pick_canonical(cursor, ids):
    scored = []
    for url_id in ids:
        tags, rankings, shortlists, aliases = count_refs(cursor, url_id)
        # 优先排序：tags > rankings > shortlists > aliases > 长度短 > 字母序
        scored.append((-tags, -rankings, -shortlists, -aliases, len(url_id), url_id))
    scored.sort()
    return scored[0][5]


def merge_group(cursor, canonical_id, losers):
    for loser in losers:
        # 1. 合并 tag 关联（去重）
        cursor.execute(
            """
            INSERT IGNORE INTO university_tag_relation (university_id, tag_id)
            SELECT %s, tag_id FROM university_tag_relation WHERE university_id=%s
            """,
            (canonical_id, loser),
        )
        cursor.execute(
            "DELETE FROM university_tag_relation WHERE university_id=%s", (loser,)
        )

        # 2. 合并 ranking_entry：删除 loser 中与 canonical 重复（source+year+subject）的记录，
        #    其余迁移到 canonical
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
        cursor.execute(
            """
            UPDATE ranking_entry SET university_id=%s WHERE university_id=%s
            """,
            (canonical_id, loser),
        )

        # 3. 更新 university_alias：把指向 loser 的 alias 指向 canonical（如冲突则忽略）
        cursor.execute(
            """
            UPDATE IGNORE university_alias SET target_url_id=%s WHERE target_url_id=%s
            """,
            (canonical_id, loser),
        )
        cursor.execute(
            "DELETE FROM university_alias WHERE target_url_id=%s", (loser,)
        )

        # 4. 合并 user_shortlist（去重按 user_id）
        cursor.execute(
            """
            INSERT IGNORE INTO user_shortlist (user_id, university_id, note)
            SELECT user_id, %s, note FROM user_shortlist WHERE university_id=%s
            """,
            (canonical_id, loser),
        )
        cursor.execute(
            "DELETE FROM user_shortlist WHERE university_id=%s", (loser,)
        )

        # 5. 软删除 loser
        cursor.execute(
            "UPDATE university SET deleted=1, updated_at=NOW() WHERE url_id=%s",
            (loser,),
        )


def main(dry_run=True):
    with closing(pymysql.connect(**DB_CONFIG)) as conn:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                SELECT name_en, country, GROUP_CONCAT(url_id) AS ids, COUNT(*) AS cnt
                FROM university
                WHERE deleted=0
                GROUP BY name_en, country
                HAVING cnt > 1
                ORDER BY cnt DESC
                """
            )
            groups = cursor.fetchall()

            total_groups = len(groups)
            total_losers = 0
            for name_en, country, ids_str, cnt in groups:
                ids = ids_str.split(",")
                canonical = pick_canonical(cursor, ids)
                losers = [u for u in ids if u != canonical]
                total_losers += len(losers)

                print(
                    f"[{'DRY-RUN' if dry_run else 'MERGE'}] {name_en} ({country}): "
                    f"保留 {canonical}, 删除 {losers}"
                )

                if not dry_run:
                    merge_group(cursor, canonical, losers)

            if not dry_run:
                conn.commit()

            print(
                f"\n完成: {total_groups} 组重复, 共删除 {total_losers} 条记录"
            )


if __name__ == "__main__":
    import sys

    # 默认 dry-run，加 --apply 才真正执行
    dry = "--apply" not in sys.argv
    if dry:
        print("== 模拟运行（dry-run），不会修改数据库。加 --apply 执行实际合并。==\n")
    main(dry_run=dry)
