#!/usr/bin/env python3
"""
导入院校标签：985 / 211 / 双一流 / 藤校
执行前请确保 university_tag 与 university_tag_relation 表已创建。
"""

import logging
import os
import sys

import mysql.connector

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)

DB_CONFIG = {
    "host": os.getenv("DB_HOST", "127.0.0.1"),
    "port": int(os.getenv("DB_PORT", "3306")),
    "user": os.getenv("DB_USER", "root"),
    "password": os.getenv("DB_PASSWORD", ""),
    "database": os.getenv("DB_NAME", "choosephd"),
    "charset": "utf8mb4",
    "collation": "utf8mb4_unicode_ci",
}

TAGS = [
    {"slug": "985", "name_zh": "985", "name_en": "Project 985", "category": "domestic", "color": "#C8102E", "sort_order": 1},
    {"slug": "211", "name_zh": "211", "name_en": "Project 211", "category": "domestic", "color": "#005BAC", "sort_order": 2},
    {"slug": "double-first", "name_zh": "双一流", "name_en": "Double First Class", "category": "domestic", "color": "#00843D", "sort_order": 3},
    {"slug": "ivy-league", "name_zh": "藤校", "name_en": "Ivy League", "category": "foreign", "color": "#5B21B6", "sort_order": 10},
]

# 39 所 985（含已合并/改名院校，按当前常用名）
NAMES_985 = [
    "清华大学", "北京大学", "中国人民大学", "北京航空航天大学", "北京理工大学",
    "北京师范大学", "中国农业大学", "中央民族大学", "南开大学", "天津大学",
    "大连理工大学", "东北大学", "吉林大学", "哈尔滨工业大学", "复旦大学",
    "同济大学", "上海交通大学", "华东师范大学", "南京大学", "东南大学",
    "浙江大学", "中国科学技术大学", "厦门大学", "山东大学", "中国海洋大学",
    "武汉大学", "华中科技大学", "湖南大学", "中南大学", "国防科技大学",
    "中山大学", "华南理工大学", "四川大学", "电子科技大学", "重庆大学",
    "西安交通大学", "西北工业大学", "西北农林科技大学", "兰州大学"
]

# 211 高校名单（116 所，含 985）
NAMES_211 = [
    # 北京 26
    "清华大学", "北京大学", "中国人民大学", "北京交通大学", "北京工业大学",
    "北京航空航天大学", "北京理工大学", "北京科技大学", "北京化工大学",
    "北京邮电大学", "中国农业大学", "北京林业大学", "北京中医药大学",
    "北京师范大学", "北京外国语大学", "中国传媒大学", "中央财经大学",
    "对外经济贸易大学", "北京体育大学", "中央音乐学院", "中央民族大学",
    "中国政法大学", "华北电力大学", "中国矿业大学（北京）", "中国石油大学（北京）",
    "中国地质大学（北京）",
    # 天津 3
    "南开大学", "天津大学", "天津医科大学",
    # 河北 1
    "河北工业大学",
    # 山西 1
    "太原理工大学",
    # 内蒙古 1
    "内蒙古大学",
    # 辽宁 4
    "大连理工大学", "东北大学", "辽宁大学", "大连海事大学",
    # 吉林 3
    "吉林大学", "延边大学", "东北师范大学",
    # 黑龙江 4
    "哈尔滨工业大学", "哈尔滨工程大学", "东北农业大学", "东北林业大学",
    # 上海 10
    "复旦大学", "同济大学", "上海交通大学", "华东理工大学", "东华大学",
    "华东师范大学", "上海外国语大学", "上海财经大学", "上海大学", "海军军医大学",
    # 江苏 11
    "南京大学", "苏州大学", "东南大学", "南京航空航天大学", "南京理工大学",
    "中国矿业大学", "河海大学", "江南大学", "南京农业大学", "中国药科大学",
    "南京师范大学",
    # 浙江 1
    "浙江大学",
    # 安徽 3
    "安徽大学", "中国科学技术大学", "合肥工业大学",
    # 福建 2
    "厦门大学", "福州大学",
    # 江西 1
    "南昌大学",
    # 山东 3
    "山东大学", "中国海洋大学", "中国石油大学（华东）",
    # 河南 1
    "郑州大学",
    # 湖北 7
    "武汉大学", "华中科技大学", "中国地质大学（武汉）", "武汉理工大学",
    "华中农业大学", "华中师范大学", "中南财经政法大学",
    # 湖南 4
    "湖南大学", "中南大学", "湖南师范大学", "国防科技大学",
    # 广东 4
    "中山大学", "暨南大学", "华南理工大学", "华南师范大学",
    # 广西 1
    "广西大学",
    # 海南 1
    "海南大学",
    # 重庆 2
    "重庆大学", "西南大学",
    # 四川 5
    "四川大学", "西南交通大学", "电子科技大学", "西南财经大学", "四川农业大学",
    # 贵州 1
    "贵州大学",
    # 云南 1
    "云南大学",
    # 西藏 1
    "西藏大学",
    # 陕西 8
    "西安交通大学", "西北工业大学", "西安电子科技大学", "长安大学",
    "西北农林科技大学", "陕西师范大学", "西北大学", "空军军医大学",
    # 甘肃 1
    "兰州大学",
    # 青海 1
    "青海大学",
    # 宁夏 1
    "宁夏大学",
    # 新疆 2
    "新疆大学", "石河子大学",
]

# 147 所双一流高校（含所有 211）
NAMES_DOUBLE_FIRST = [
    # A 类 36
    "北京大学", "中国人民大学", "清华大学", "北京航空航天大学", "北京理工大学",
    "中国农业大学", "北京师范大学", "中央民族大学", "南开大学", "天津大学",
    "大连理工大学", "吉林大学", "哈尔滨工业大学", "复旦大学", "同济大学",
    "上海交通大学", "华东师范大学", "南京大学", "东南大学", "浙江大学",
    "中国科学技术大学", "厦门大学", "山东大学", "中国海洋大学", "武汉大学",
    "华中科技大学", "中南大学", "中山大学", "华南理工大学", "四川大学",
    "重庆大学", "电子科技大学", "西安交通大学", "西北工业大学", "兰州大学",
    "国防科技大学",
    # B 类 6
    "东北大学", "郑州大学", "湖南大学", "云南大学", "西北农林科技大学", "新疆大学",
    # 学科高校（部分，约 105 所）
    "北京交通大学", "北京工业大学", "北京科技大学", "北京化工大学", "北京邮电大学",
    "北京林业大学", "北京协和医学院", "北京中医药大学", "北京外国语大学", "中国传媒大学",
    "中央财经大学", "对外经济贸易大学", "外交学院", "中国人民公安大学", "北京体育大学",
    "中央音乐学院", "中国音乐学院", "中央美术学院", "中央戏剧学院", "中国政法大学",
    "天津工业大学", "天津医科大学", "天津中医药大学", "华北电力大学", "河北工业大学",
    "太原理工大学", "内蒙古大学", "辽宁大学", "大连海事大学", "延边大学",
    "东北师范大学", "哈尔滨工程大学", "东北农业大学", "东北林业大学", "华东理工大学",
    "东华大学", "上海海洋大学", "上海中医药大学", "上海外国语大学", "上海财经大学",
    "上海体育学院", "上海音乐学院", "上海大学", "苏州大学", "南京航空航天大学",
    "南京理工大学", "中国矿业大学", "南京邮电大学", "河海大学", "江南大学",
    "南京林业大学", "南京信息工程大学", "南京农业大学", "南京中医药大学", "中国药科大学",
    "南京师范大学", "安徽大学", "合肥工业大学", "福州大学", "南昌大学",
    "中国石油大学（华东）", "河南大学", "中国地质大学（武汉）", "武汉理工大学", "华中农业大学",
    "华中师范大学", "中南财经政法大学", "湖南师范大学", "暨南大学", "华南师范大学",
    "广州中医药大学", "南方科技大学", "广西大学", "海南大学", "西南交通大学",
    "西南石油大学", "成都理工大学", "四川农业大学", "成都中医药大学", "西南大学",
    "西南财经大学", "贵州大学", "西藏大学", "西北大学", "西安电子科技大学",
    "长安大学", "陕西师范大学", "空军军医大学", "青海大学", "宁夏大学",
    "石河子大学", "中国石油大学（北京）", "中国矿业大学（北京）", "中国地质大学（北京）", "宁波大学",
    "中国科学院大学"
]

# 藤校 8 所（按英文名校匹）
IVY_LEAGUE_EN_NAMES = {
    "Harvard University",
    "Yale University",
    "Princeton University",
    "Columbia University",
    "Brown University",
    "Dartmouth College",
    "University of Pennsylvania",
    "Cornell University",
}


def connect():
    return mysql.connector.connect(**DB_CONFIG)


def ensure_tags(cursor):
    tag_id_map = {}
    for tag in TAGS:
        cursor.execute(
            "SELECT id FROM university_tag WHERE slug = %s",
            (tag["slug"],)
        )
        row = cursor.fetchone()
        if row:
            tag_id_map[tag["slug"]] = row[0]
            logger.info("Tag '%s' already exists with id=%s", tag["slug"], row[0])
        else:
            cursor.execute(
                """INSERT INTO university_tag (slug, name_zh, name_en, category, color, sort_order, active)
                   VALUES (%s, %s, %s, %s, %s, %s, 1)""",
                (tag["slug"], tag["name_zh"], tag["name_en"],
                 tag["category"], tag["color"], tag["sort_order"])
            )
            tag_id_map[tag["slug"]] = cursor.lastrowid
            logger.info("Created tag '%s' with id=%s", tag["slug"], cursor.lastrowid)
    return tag_id_map


def find_universities_by_names(cursor, names):
    placeholders = ", ".join(["%s"] * len(names))
    cursor.execute(
        f"SELECT url_id, name_zh, name_en FROM university WHERE name_zh IN ({placeholders}) AND deleted = 0",
        tuple(names)
    )
    return {row[0]: {"name_zh": row[1], "name_en": row[2]} for row in cursor.fetchall()}


def find_ivy_league(cursor):
    placeholders = ", ".join(["%s"] * len(IVY_LEAGUE_EN_NAMES))
    cursor.execute(
        f"SELECT url_id, name_zh, name_en FROM university WHERE name_en IN ({placeholders}) AND deleted = 0",
        tuple(IVY_LEAGUE_EN_NAMES)
    )
    return {row[0]: {"name_zh": row[1], "name_en": row[2]} for row in cursor.fetchall()}


def assign_tag(cursor, tag_id, url_ids):
    inserted = 0
    for url_id in url_ids:
        cursor.execute(
            """INSERT IGNORE INTO university_tag_relation (university_id, tag_id)
               VALUES (%s, %s)""",
            (url_id, tag_id)
        )
        if cursor.rowcount > 0:
            inserted += 1
    return inserted


def main():
    conn = connect()
    cursor = conn.cursor()
    try:
        tag_id_map = ensure_tags(cursor)

        # 985
        universities_985 = find_universities_by_names(cursor, NAMES_985)
        inserted_985 = assign_tag(cursor, tag_id_map["985"], universities_985.keys())
        logger.info("985: matched %d universities, inserted %d relations", len(universities_985), inserted_985)

        # 211
        universities_211 = find_universities_by_names(cursor, NAMES_211)
        inserted_211 = assign_tag(cursor, tag_id_map["211"], universities_211.keys())
        logger.info("211: matched %d universities, inserted %d relations", len(universities_211), inserted_211)

        # 双一流
        universities_df = find_universities_by_names(cursor, NAMES_DOUBLE_FIRST)
        inserted_df = assign_tag(cursor, tag_id_map["double-first"], universities_df.keys())
        logger.info("双一流: matched %d universities, inserted %d relations", len(universities_df), inserted_df)

        # 藤校
        universities_ivy = find_ivy_league(cursor)
        inserted_ivy = assign_tag(cursor, tag_id_map["ivy-league"], universities_ivy.keys())
        logger.info("藤校: matched %d universities, inserted %d relations", len(universities_ivy), inserted_ivy)

        conn.commit()
        logger.info("University tags imported successfully")
    except Exception as e:
        conn.rollback()
        logger.error("Import failed: %s", e)
        raise
    finally:
        cursor.close()
        conn.close()


if __name__ == "__main__":
    main()
