#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JOSP-choosePhd ranking_data 备份 2 → computer_rank 灌库脚本
================================================================
来源: /Users/junw/Desktop/ranking_data 备份 2/  (相对"备份 1" 的新增榜单)
目标: 7 张新表 (DDL 已建)
  1. university_rankings_arwu_subject        (ARWU 学科 11×多 年)
  2. university_rankings_edurank_region     (EduRank 6 个地区×多年)
  3. university_rankings_declining_trend     (下降趋势 6 source × 多年)
  4. university_rankings_mosiur_world       (MOSIUR 全球×多年)
  5. university_rankings_rur_world          (RUR 全球×多年)
  6. university_rankings_usnews_subject     (US News 学科 51×多年)
  7. university_rankings_qs_sustainability  (QS 可持续 3 年)

设计原则:
- 直连 MySQL, 用 INSERT 批量 1000 行/批 (单事务)
- 跟现有 university_rankings_qs 同 schema (10 列), 7 张表同结构只表名+注释+索引不同
- 排名整数解析规则: rank_alias 优先 (整数), 兜底 rank 字段 strip '#' 转 int, 失败置 None
- CSV BOM: utf-8-sig 处理
- tag_0/tag_1 字段: 12 列 schema (含 name_fanti) 是 tag_name/tag_1_name, 12 列 schema (无 name_fanti) 是 countries/regions, 12 列 schema (eng_name 起头) 是 tag_0_name

执行:  python3 import_ranking_backup2.py
幂等:  跑第二次 = 全删 + 重灌 (TRUNCATE), 避免重复键
"""

import csv
import glob
import json
import os
import re
import sys
from typing import Optional

import pymysql

SRC_ROOT = '/Users/junw/Desktop/ranking_data 备份 2'

DB_CONFIG = dict(
    host='127.0.0.1',
    port=3306,
    user='root',
    password='',
    database='computer_rank',
    charset='utf8mb4',
    autocommit=False,
)


# ---------------------------- 工具函数 ----------------------------

def parse_int_rank(rank_alias: str, rank: str) -> Optional[int]:
    """统一把 rank_alias (整数字符串) 解析为 int; 失败兜底 rank 字段"""
    if rank_alias is not None:
        s = str(rank_alias).strip()
        if s and s.lower() not in ('nan', 'none', ''):
            try:
                return int(s)
            except (ValueError, TypeError):
                pass
    if rank is not None:
        m = re.search(r'-?\d+', str(rank))
        if m:
            try:
                return int(m.group(0))
            except (ValueError, TypeError):
                pass
    return None


def extract_year_from_filename(filename: str) -> Optional[str]:
    """从 2017.csv / 2007_1.json 提取年份"""
    m = re.match(r'^(\d{4})(?:_\d+)?\.', os.path.basename(filename))
    return m.group(1) if m else None


def extract_year_from_row(row: dict) -> Optional[str]:
    """ARWU 学科 schema 自带 year 列, 优先用"""
    y = row.get('year')
    if y and str(y).strip().isdigit():
        return str(y).strip()
    return None


def extract_tags_from_usnews_subject(tags_json_str: str) -> tuple:
    """usnews subject 的 tags 是 JSON 字符串: [{country}, {region}]"""
    if not tags_json_str:
        return (None, None)
    try:
        # csv DictReader 已经把 JSON 当字符串读出, ast.literal_eval 比 json.loads 更宽松
        import ast
        tags = ast.literal_eval(tags_json_str)
        country = region = None
        for t in tags:
            eng = t.get('eng_name', '')
            if eng in ('U.S.', 'China', 'Japan', 'United Kingdom', 'Germany', 'France',
                       'Canada', 'Australia', 'India', 'Brazil', 'Russia', 'Netherlands',
                       'Switzerland', 'Sweden', 'Singapore', 'South Korea', 'Italy',
                       'Spain', 'Belgium', 'Hong Kong', 'Taiwan', 'New Zealand', 'Mexico',
                       'Norway', 'Denmark', 'Finland', 'Israel', 'Ireland', 'Austria',
                       'Poland', 'Czech Republic', 'Hungary', 'Portugal', 'Greece',
                       'Turkey', 'Saudi Arabia', 'South Africa', 'Argentina', 'Chile',
                       'Colombia', 'Egypt', 'Indonesia', 'Malaysia', 'Thailand',
                       'Pakistan', 'Bangladesh', 'Philippines', 'Vietnam', 'Iran',
                       'Iraq', 'Lebanon', 'Kuwait', 'UAE', 'Qatar'):
                country = t.get('name')
            elif eng in ('Asia', 'Europe', 'North America', 'South America', 'Africa',
                         'Oceania', 'Central America', 'Eastern Europe', 'Western Europe',
                         'Northern Europe', 'Southern Europe', 'Southeast Asia',
                         'East Asia', 'South Asia', 'Middle East', 'Caribbean',
                         'Central Asia'):
                region = t.get('name')
        return (country, region)
    except Exception:
        return (None, None)


def normalize_chinese(name: str) -> Optional[str]:
    if name is None:
        return None
    s = str(name).strip()
    return s if s else None


def make_row(name: str, eng_name: str, tags: Optional[str], tags_state: Optional[str],
             category: Optional[str], year: Optional[str], rank_int: Optional[int],
             rank_raw: Optional[str], variant: str) -> tuple:
    """统一 10 列表, 给 INSERT 用"""
    return (
        normalize_chinese(name),
        eng_name.strip() if eng_name else None,
        tags.strip() if tags else None,
        tags_state.strip() if tags_state else None,
        category.strip() if category else None,
        year.strip() if year else None,
        rank_int,
        rank_raw.strip() if rank_raw else None,
        variant,
    )


# ---------------------------- 7 个处理器 ----------------------------

def gen_arwu_subject(conn):
    """arwu-university-subject-rankings/{subject}/{year}.csv"""
    table = 'university_rankings_arwu_subject'
    src = os.path.join(SRC_ROOT, 'arwu-university-subject-rankings')
    if not os.path.isdir(src):
        print(f'  [skip] missing {src}'); return 0
    rows_all = []
    for sub_dir in sorted(os.listdir(src)):
        sub_path = os.path.join(src, sub_dir)
        if not os.path.isdir(sub_path): continue
        # slug 转中文 category
        category = sub_dir.replace('-', ' ').title()
        for csv_file in sorted(glob.glob(f'{sub_path}/*.csv')):
            year = extract_year_from_filename(csv_file)
            with open(csv_file, 'r', encoding='utf-8-sig') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    country = row.get('tag_0_name') or None
                    region = row.get('tag_1_name') or None
                    rows_all.append(make_row(
                        row.get('name'), row.get('eng_name'),
                        country, region, category, year,
                        parse_int_rank(row.get('rank_alias'), row.get('rank')),
                        row.get('rank'), 'arwu_subject'))
    if rows_all:
        with conn.cursor() as cur:
            cur.execute(f'TRUNCATE TABLE {table}')
            cur.executemany(
                f'INSERT INTO {table} (university_name_chinese, university_name_english, '
                f'university_tags, university_tags_state, ranking_category, ranking_year, '
                f'current_rank_integer, current_rank_raw, rank_variant) '
                f'VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)', rows_all)
    print(f'  arwu_subject: {len(rows_all)} rows')
    return len(rows_all)


def gen_edurank_region(conn):
    """edurank-{region}-university-rankings/{year}.csv (6 个 region)"""
    table = 'university_rankings_edurank_region'
    rows_all = []
    regions = ['africa', 'asia', 'eu', 'latin-america', 'north-america', 'oceania']
    region_cn = {'africa': '非洲', 'asia': '亚洲', 'eu': '欧洲',
                 'latin-america': '拉丁美洲', 'north-america': '北美洲', 'oceania': '大洋洲'}
    for reg in regions:
        src = os.path.join(SRC_ROOT, f'edurank-{reg}-university-rankings')
        if not os.path.isdir(src):
            print(f'  [skip] missing {src}'); continue
        cat_cn = region_cn[reg]
        for csv_file in sorted(glob.glob(f'{src}/*.csv')):
            year = extract_year_from_filename(csv_file)
            with open(csv_file, 'r', encoding='utf-8-sig') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    # 12 列 schema: 有 countries/regions 列 (asia/latin/oceania) 或
                    #               有 tag_name (africa/eu/north-america)
                    country = row.get('countries') or row.get('tag_name') or None
                    region = row.get('regions') or row.get('tag_1_name') or None
                    rows_all.append(make_row(
                        row.get('name'), row.get('eng_name'),
                        country, region, cat_cn, year,
                        parse_int_rank(row.get('rank_alias'), row.get('rank')),
                        row.get('rank'), 'edurank_region'))
    if rows_all:
        with conn.cursor() as cur:
            cur.execute(f'TRUNCATE TABLE {table}')
            cur.executemany(
                f'INSERT INTO {table} (university_name_chinese, university_name_english, '
                f'university_tags, university_tags_state, ranking_category, ranking_year, '
                f'current_rank_integer, current_rank_raw, rank_variant) '
                f'VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)', rows_all)
    print(f'  edurank_region: {len(rows_all)} rows')
    return len(rows_all)


def gen_declining_trend(conn):
    """declining-trend-{source}-*/{year}.csv (6 source)"""
    table = 'university_rankings_declining_trend'
    rows_all = []
    sources = ['qs', 'the', 'arwu', 'usnews', 'cwur', 'edurank']
    src_dirs = [
        'declining-trend-qs-world-universities-rankings',
        'declining-trend-the-world-universities-rankings',
        'declining-trend-arwu-world-universities-rankings',
        'declining-trend-usnews-world-universities-rankings',
        'declining-trend-cwur-world-university-rankings',
        'declining-trend-edurank-world-university-rankings',
    ]
    for src_name in src_dirs:
        src = os.path.join(SRC_ROOT, src_name)
        if not os.path.isdir(src):
            print(f'  [skip] missing {src}'); continue
        # category 用 source slug
        source = src_name.replace('declining-trend-', '').replace('-world-universities-rankings', '')\
                         .replace('-world-university-rankings', '')
        for csv_file in sorted(glob.glob(f'{src}/*.csv')):
            year = extract_year_from_filename(csv_file)
            with open(csv_file, 'r', encoding='utf-8-sig') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    country = row.get('tag_name') or None
                    region = row.get('tag_1_name') or None
                    rows_all.append(make_row(
                        row.get('name'), row.get('eng_name'),
                        country, region, source, year,
                        parse_int_rank(row.get('rank_alias'), row.get('rank')),
                        row.get('rank'), f'declining_{source}'))
    if rows_all:
        with conn.cursor() as cur:
            cur.execute(f'TRUNCATE TABLE {table}')
            cur.executemany(
                f'INSERT INTO {table} (university_name_chinese, university_name_english, '
                f'university_tags, university_tags_state, ranking_category, ranking_year, '
                f'current_rank_integer, current_rank_raw, rank_variant) '
                f'VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)', rows_all)
    print(f'  declining_trend: {len(rows_all)} rows')
    return len(rows_all)


def gen_mosiur_world(conn):
    """mosiur-world-university-ranking/{year}.csv"""
    table = 'university_rankings_mosiur_world'
    src = os.path.join(SRC_ROOT, 'mosiur-world-university-ranking')
    rows_all = []
    if os.path.isdir(src):
        for csv_file in sorted(glob.glob(f'{src}/*.csv')):
            year = extract_year_from_filename(csv_file)
            with open(csv_file, 'r', encoding='utf-8-sig') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    rows_all.append(make_row(
                        row.get('name'), row.get('eng_name'),
                        row.get('countries'), row.get('regions'),
                        'mosiur_world', year,
                        parse_int_rank(row.get('rank_alias'), row.get('rank')),
                        row.get('rank'), 'mosiur_world'))
    if rows_all:
        with conn.cursor() as cur:
            cur.execute(f'TRUNCATE TABLE {table}')
            cur.executemany(
                f'INSERT INTO {table} (university_name_chinese, university_name_english, '
                f'university_tags, university_tags_state, ranking_category, ranking_year, '
                f'current_rank_integer, current_rank_raw, rank_variant) '
                f'VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)', rows_all)
    print(f'  mosiur_world: {len(rows_all)} rows')
    return len(rows_all)


def gen_rur_world(conn):
    """rur-world-university-rankings/{year}.csv (schema: rank, rank_alias, name, eng_name, country, region, url_id)"""
    table = 'university_rankings_rur_world'
    src = os.path.join(SRC_ROOT, 'rur-world-university-rankings')
    rows_all = []
    if os.path.isdir(src):
        for csv_file in sorted(glob.glob(f'{src}/*.csv')):
            year = extract_year_from_filename(csv_file)
            with open(csv_file, 'r', encoding='utf-8-sig') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    rows_all.append(make_row(
                        row.get('name'), row.get('eng_name'),
                        row.get('country'), row.get('region'),
                        'rur_world', year,
                        parse_int_rank(row.get('rank_alias'), row.get('rank')),
                        row.get('rank'), 'rur_world'))
    if rows_all:
        with conn.cursor() as cur:
            cur.execute(f'TRUNCATE TABLE {table}')
            cur.executemany(
                f'INSERT INTO {table} (university_name_chinese, university_name_english, '
                f'university_tags, university_tags_state, ranking_category, ranking_year, '
                f'current_rank_integer, current_rank_raw, rank_variant) '
                f'VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)', rows_all)
    print(f'  rur_world: {len(rows_all)} rows')
    return len(rows_all)


def gen_usnews_subject(conn):
    """usnews-university-subject-rankings/{subject}/{year}.csv"""
    table = 'university_rankings_usnews_subject'
    src = os.path.join(SRC_ROOT, 'usnews-university-subject-rankings')
    rows_all = []
    if os.path.isdir(src):
        for sub_dir in sorted(os.listdir(src)):
            sub_path = os.path.join(src, sub_dir)
            if not os.path.isdir(sub_path): continue
            category = sub_dir.replace('-', ' ').title()
            for csv_file in sorted(glob.glob(f'{sub_path}/*.csv')):
                year = extract_year_from_filename(csv_file)
                with open(csv_file, 'r', encoding='utf-8-sig') as f:
                    reader = csv.DictReader(f)
                    for row in reader:
                        country, region = extract_tags_from_usnews_subject(row.get('tags'))
                        rows_all.append(make_row(
                            row.get('name'), row.get('eng_name'),
                            country, region, category, year,
                            parse_int_rank(row.get('rank_alias'), row.get('rank')),
                            row.get('rank'), 'usnews_subject'))
    if rows_all:
        with conn.cursor() as cur:
            cur.execute(f'TRUNCATE TABLE {table}')
            cur.executemany(
                f'INSERT INTO {table} (university_name_chinese, university_name_english, '
                f'university_tags, university_tags_state, ranking_category, ranking_year, '
                f'current_rank_integer, current_rank_raw, rank_variant) '
                f'VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)', rows_all)
    print(f'  usnews_subject: {len(rows_all)} rows')
    return len(rows_all)


def gen_qs_sustainability(conn):
    """qs-sustainability-university-rankings/{year}.csv (schema: rank, rank_alias, name, eng_name, country, region, url_id)"""
    table = 'university_rankings_qs_sustainability'
    src = os.path.join(SRC_ROOT, 'qs-sustainability-university-rankings')
    rows_all = []
    if os.path.isdir(src):
        for csv_file in sorted(glob.glob(f'{src}/*.csv')):
            year = extract_year_from_filename(csv_file)
            with open(csv_file, 'r', encoding='utf-8-sig') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    rows_all.append(make_row(
                        row.get('name'), row.get('eng_name'),
                        row.get('country'), row.get('region'),
                        'qs_sustainability', year,
                        parse_int_rank(row.get('rank_alias'), row.get('rank')),
                        row.get('rank'), 'qs_sustainability'))
    if rows_all:
        with conn.cursor() as cur:
            cur.execute(f'TRUNCATE TABLE {table}')
            cur.executemany(
                f'INSERT INTO {table} (university_name_chinese, university_name_english, '
                f'university_tags, university_tags_state, ranking_category, ranking_year, '
                f'current_rank_integer, current_rank_raw, rank_variant) '
                f'VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)', rows_all)
    print(f'  qs_sustainability: {len(rows_all)} rows')
    return len(rows_all)


# ---------------------------- Main ----------------------------

def main():
    print(f'Connecting to MySQL {DB_CONFIG["host"]}:{DB_CONFIG["port"]} db={DB_CONFIG["database"]} ...')
    conn = pymysql.connect(**DB_CONFIG)
    try:
        totals = []
        for fn in (gen_arwu_subject, gen_edurank_region, gen_declining_trend,
                   gen_mosiur_world, gen_rur_world, gen_usnews_subject,
                   gen_qs_sustainability):
            print(f'\n>>> {fn.__name__}')
            try:
                totals.append(fn(conn))
            except Exception as e:
                conn.rollback()
                print(f'  [ERROR] {fn.__name__}: {e}')
                import traceback; traceback.print_exc()
                totals.append(0)
        conn.commit()
        print(f'\n=== TOTAL ===\n  inserted: {sum(totals)} rows across 7 tables')
    finally:
        conn.close()


if __name__ == '__main__':
    main()