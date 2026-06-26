"""
解析 WDQS SPARQL XML 结果, 写入 institution 表新加的 name_zh / name_zh_source 字段
"""
import defusedxml.ElementTree as ET  # WDQS 公网响应, 不需处理外部实体, 防御性使用
import pymysql
import sys

ns = {"s": "http://www.w3.org/2005/sparql-results#"}

def parse(path):
    tree = ET.parse(path)
    out = {}
    for r in tree.getroot().findall(".//s:result", ns):
        en = r.find("s:binding[@name='itemLabelEn']/s:literal", ns)
        zh = r.find("s:binding[@name='itemLabelZh']/s:literal", ns)
        if en is None or zh is None: continue
        out[en.text.strip()] = zh.text.strip()
    return out

m = {}
m.update(parse("/tmp/wd_r1.xml"))
m.update(parse("/tmp/wd_r2.xml"))
print(f"WDQS 返回 {len(m)} 个 en→zh 映射")

conn = pymysql.connect(host="127.0.0.1", user="root", password="", database="csrankings", charset="utf8mb4")
cur = conn.cursor()
# 1) 加列 (MySQL 8 不支持 IF NOT EXISTS on ADD COLUMN, 用 information_schema 判)
cur.execute("""
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='csrankings' AND TABLE_NAME='institution' AND COLUMN_NAME='name_zh'
""")
if cur.fetchone()[0] == 0:
    cur.execute("ALTER TABLE institution ADD COLUMN name_zh VARCHAR(200) NULL AFTER name")
    cur.execute("ALTER TABLE institution ADD COLUMN name_zh_source VARCHAR(40) NULL AFTER name_zh")
conn.commit()

# 2) 全量 SELECT, Python 侧匹配更新
cur.execute("SELECT id, name, name_zh FROM institution")
rows = cur.fetchall()
print(f"DB {len(rows)} 院校")

# 3) 简单归一化匹配: lower + strip
def norm(s): return s.lower().replace(".", "").replace(",", "").replace("-", " ").strip()
zh_by_norm = {norm(k): v for k, v in m.items()}

update_batch = []
hit = 0
miss_list = []
for i, name, name_zh in rows:
    if name_zh: continue  # 已有
    zh = zh_by_norm.get(norm(name))
    if zh:
        update_batch.append((zh, "wikidata", i))
        hit += 1
    else:
        miss_list.append((i, name))

print(f"WDQS 命中: {hit}/{len(rows)} ({hit*100//len(rows)}%)")
print(f"未命中 {len(miss_list)} 个, 前 30:")
for x in miss_list[:30]: print(f"  [{x[0]}] {x[1]}")

# 4) 批量更新
if update_batch:
    cur.executemany("UPDATE institution SET name_zh=%s, name_zh_source=%s WHERE id=%s", update_batch)
    conn.commit()
    print(f"已更新 {len(update_batch)} 条")
cur.close()
conn.close()
