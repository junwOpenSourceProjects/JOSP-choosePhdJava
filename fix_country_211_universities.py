#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
清洗 university.country='211' 的异常数据。

背景：部分中国院校在导入时被错误写入 country='211'。本脚本：
1. 对 name_en + name_zh 完全相同的 country='211' 与 country='中国' 记录进行合并，保留中国记录；
2. 处理名称略有差异但实为同一所院校的近重复对；
3. 将已无重复、仅剩的 country='211' 记录修正为 country='中国'；
4. 最后校验 39 所 985 院校的标签覆盖，缺失则补齐 985/211/双一流标签。

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

# 名称差异但实际同一院校的合并对：(canonical_url_id, [loser_url_id, ...])
NEAR_DUPLICATES = [
    ("national-university-defense-technology", ["national-university-of-defense-technology"]),
    ("air-force-medical-university", ["the-fourth-military-medical-university"]),
]

# 39 所 985 院校中文名称（用于最终校验）
C9_UNIVERSITIES = [
    "清华大学", "北京大学", "浙江大学", "上海交通大学", "复旦大学",
    "南京大学", "中国科学技术大学", "哈尔滨工业大学", "西安交通大学",
]
PROJECT_985_UNIVERSITIES = [
    "中国人民大学", "北京航空航天大学", "北京理工大学", "北京师范大学",
    "中国农业大学", "中央民族大学", "南开大学", "天津大学", "大连理工大学",
    "东北大学", "吉林大学", "哈尔滨工业大学", "同济大学", "上海交通大学",
    "华东师范大学", "南京大学", "东南大学", "浙江大学", "中国科学技术大学",
    "厦门大学", "山东大学", "中国海洋大学", "武汉大学", "华中科技大学",
    "湖南大学", "中南大学", "国防科技大学", "中山大学", "华南理工大学",
    "四川大学", "电子科技大学", "重庆大学", "西安交通大学", "西北工业大学",
    "西北农林科技大学", "兰州大学",
]
ALL_985_ZH = list(set(C9_UNIVERSITIES + PROJECT_985_UNIVERSITIES))


def count_refs(cursor, url_id):
    cursor.execute("SELECT COUNT(*) FROM university_tag_relation WHERE university_id=%s", (url_id,))
    tags = cursor.fetchone()[0]
    cursor.execute("SELECT COUNT(*) FROM ranking_entry WHERE university_id=%s", (url_id,))
    rankings = cursor.fetchone()[0]
    cursor.execute("SELECT COUNT(*) FROM user_shortlist WHERE university_id=%s", (url_id,))
    shortlists = cursor.fetchone()[0]
    cursor.execute("SELECT COUNT(*) FROM university_alias WHERE target_url_id=%s", (url_id,))
    aliases = cursor.fetchone()[0]
    return tags, rankings, shortlists, aliases


def merge_group(cursor, canonical_id, losers):
    for loser in losers:
        # 1. 合并 tag 关联
        cursor.execute(
            """
            INSERT IGNORE INTO university_tag_relation (university_id, tag_id)
            SELECT %s, tag_id FROM university_tag_relation WHERE university_id=%s
            """,
            (canonical_id, loser),
        )
        cursor.execute("DELETE FROM university_tag_relation WHERE university_id=%s", (loser,))

        # 2. 合并 ranking_entry（source+year+subject 去重）
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

        # 3. 合并 alias
        cursor.execute("UPDATE IGNORE university_alias SET target_url_id=%s WHERE target_url_id=%s", (canonical_id, loser))
        cursor.execute("DELETE FROM university_alias WHERE target_url_id=%s", (loser,))

        # 4. 合并 shortlist
        cursor.execute(
            """
            INSERT IGNORE INTO user_shortlist (user_id, university_id, note)
            SELECT user_id, %s, note FROM user_shortlist WHERE university_id=%s
            """,
            (canonical_id, loser),
        )
        cursor.execute("DELETE FROM user_shortlist WHERE university_id=%s", (loser,))

        # 5. 软删除 loser
        cursor.execute("UPDATE university SET deleted=1, updated_at=NOW() WHERE url_id=%s", (loser,))


def fix_exact_match_duplicates(cursor, dry_run=True):
    """合并 name_en+name_zh 完全相同的 211 与中国记录，保留中国记录。"""
    cursor.execute(
        """
        SELECT a.url_id AS id_211, b.url_id AS id_cn, a.name_en, a.name_zh
        FROM university a
        JOIN university b ON a.name_en = b.name_en AND a.name_zh = b.name_zh
        WHERE a.country = '211' AND b.country = '中国'
          AND a.deleted = 0 AND b.deleted = 0
        ORDER BY a.name_en
        """
    )
    pairs = cursor.fetchall()
    merged = 0
    for id_211, id_cn, name_en, name_zh in pairs:
        print(f"[{'DRY-RUN' if dry_run else 'MERGE'}] {name_en} ({name_zh}): {id_211} -> {id_cn}")
        if not dry_run:
            merge_group(cursor, id_cn, [id_211])
        merged += 1
    return merged


def fix_near_duplicates(cursor, dry_run=True):
    """合并且名称略有差异的重复对。"""
    merged = 0
    for canonical, losers in NEAR_DUPLICATES:
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
        merged += len(valid_losers)
    return merged


def update_remaining_211_to_china(cursor, dry_run=True):
    """将剩余的 country='211' 修正为 '中国'。"""
    cursor.execute(
        """
        SELECT url_id, name_en, name_zh FROM university
        WHERE country = '211' AND deleted = 0
        ORDER BY name_en
        """
    )
    rows = cursor.fetchall()
    updated = 0
    for url_id, name_en, name_zh in rows:
        print(f"[{'DRY-RUN' if dry_run else 'UPDATE'}] {name_en} ({name_zh}): country '211' -> '中国'")
        if not dry_run:
            cursor.execute(
                "UPDATE university SET country='中国', updated_at=NOW() WHERE url_id=%s",
                (url_id,),
            )
        updated += 1
    return updated


def find_985_university_url(cursor, short_name):
    """根据 985 简称找到对应院校 url_id，支持精确与模式匹配。"""
    # 部分院校在库中的中文名带后缀或前缀，需用 LIKE 匹配
    patterns = {
        "东北大学": ("东北大学，%",),
        "国防科技大学": ("%国防科技大学%",),
    }
    if short_name in patterns:
        cursor.execute(
            "SELECT url_id FROM university WHERE country='中国' AND deleted=0 AND name_zh LIKE %s LIMIT 1",
            patterns[short_name],
        )
    else:
        cursor.execute(
            "SELECT url_id FROM university WHERE country='中国' AND deleted=0 AND name_zh=%s LIMIT 1",
            (short_name,),
        )
    row = cursor.fetchone()
    return row[0] if row else None


def ensure_985_tags(cursor, dry_run=True):
    """确保 39 所 985 院校都关联 985/211/双一流标签。"""
    tag_map = {}
    cursor.execute("SELECT slug, id FROM university_tag WHERE slug IN ('985','211','double-first')")
    for slug, tid in cursor.fetchall():
        tag_map[slug] = tid

    missing = []
    for short_name in ALL_985_ZH:
        url_id = find_985_university_url(cursor, short_name)
        if not url_id:
            missing.append((short_name, "未找到院校"))
            continue
        for slug in ("985", "211", "double-first"):
            tid = tag_map.get(slug)
            if not tid:
                continue
            cursor.execute(
                "SELECT 1 FROM university_tag_relation WHERE university_id=%s AND tag_id=%s",
                (url_id, tid),
            )
            if not cursor.fetchone():
                print(f"[{'DRY-RUN' if dry_run else 'TAG'}] {short_name} 缺失 {slug}，补充")
                if not dry_run:
                    cursor.execute(
                        "INSERT INTO university_tag_relation (university_id, tag_id) VALUES (%s, %s)",
                        (url_id, tid),
                    )
    return missing


def main(dry_run=True):
    with closing(pymysql.connect(**DB_CONFIG)) as conn:
        with conn.cursor() as cursor:
            print("== 步骤 1/4: 合并完全重复记录 ==")
            exact_merged = fix_exact_match_duplicates(cursor, dry_run)

            print("\n== 步骤 2/4: 合并近重复记录 ==")
            near_merged = fix_near_duplicates(cursor, dry_run)

            print("\n== 步骤 3/4: 修正剩余 country='211' ==")
            updated = update_remaining_211_to_china(cursor, dry_run)

            print("\n== 步骤 4/4: 校验并补全 985/211/双一流标签 ==")
            missing = ensure_985_tags(cursor, dry_run)

            if missing:
                print(f"\n警告: 以下 985 院校未找到或缺失: {missing}")

            if not dry_run:
                conn.commit()

            print(
                f"\n完成: 完全重复合并 {exact_merged} 条, "
                f"近重复合并 {near_merged} 条, 修正 country {updated} 条"
            )


if __name__ == "__main__":
    import sys

    dry = "--apply" not in sys.argv
    if dry:
        print("== 模拟运行（dry-run），不会修改数据库。加 --apply 执行实际修正。==\n")
    main(dry_run=dry)
