# ranking_import — ranking_data 备份 2 灌库脚本

## 概述

`/Users/junw/Desktop/ranking_data 备份 2/` 相对"备份 1" 多出 8 类榜单:
- ARWU 学科 (11 学科×多 年)
- EduRank 6 个地区排名
- 6 类下降趋势 (qs/the/arwu/usnews/cwur/edurank)
- MOSIUR 全球
- RUR 全球学术
- US News 学科 (51 学科×多 年)
- QS 可持续

数据库 `computer_rank` 之前没这 7 张表,这套脚本一次性建表 + 灌库。

## 文件

| 文件 | 作用 |
|------|------|
| `ddl_new_tables.sql` | 7 张新表 CREATE TABLE IF NOT EXISTS + 索引, 跑多次安全 |
| `import_ranking_backup2.py` | Python 灌库脚本, 直连 MySQL, TRUNCATE+INSERT 模式 |

## 用法

```bash
# 1. 建表 (幂等)
mysql -uroot < ddl_new_tables.sql

# 2. 灌库 (TRUNCATE + INSERT, 幂等)
python3 import_ranking_backup2.py
```

## 输出 (2026-06-21 实测)

```
>>> gen_arwu_subject        29887 rows
>>> gen_edurank_region       1391 rows
>>> gen_declining_trend     10679 rows
>>> gen_mosiur_world         8000 rows
>>> gen_rur_world           13177 rows
>>> gen_usnews_subject      30449 rows
>>> gen_qs_sustainability    4132 rows
=== TOTAL === 97715 rows across 7 tables
```

## 设计

- 表 schema 跟 `university_rankings_qs` 保持一致 (10 列: id + chinese + english + tags + tags_state + ranking_category + ranking_year + current_rank_integer + current_rank_raw + rank_variant)
- `rank_variant` 字段固定值区分榜单类型 (arwu_subject / edurank_region / declining_qs / mosiur_world / rur_world / usnews_subject / qs_sustainability)
- `current_rank_integer` 解析规则: rank_alias (整数) 优先 → 兜底 rank 字段 strip `#` → None
- CSV BOM 用 `utf-8-sig` 处理
- usnews subject tags 字段是 JSON 字符串含逗号, csv module DictReader 自动处理引号

## 关联

- `src/main/resources/schema.sql` — 同步追加 7 张表 DDL, Spring Boot 启动兜底
- `db/init.sql` — 同步追加 7 张表 DDL, user 手动 reset 用

## 依赖

```bash
pip3 install pymysql
```