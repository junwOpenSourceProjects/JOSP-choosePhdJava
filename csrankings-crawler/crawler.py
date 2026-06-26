"""
csrankings.org 全量数据爬虫
=====================================

数据源: https://github.com/emeryberger/CSrankings (官方仓库 5 个 CSV)
        + 仓库内 csrankings.js 解析出 27 个一级领域 + 67 个子领域

输出:   MySQL `csrankings` 库,7 表 + 2 视图

设计要点:
1. 多线程: 8 线程并发写库 (INSERT batch 10K)
2. 防封/限速: 1 线程从 GitHub 拉原始 CSV (无 DBLP 实时抓取,数据是静态下载)
3. 重试: requests Session + urllib3 Retry 3 次
4. 内存: csv DictReader 流式,逐行 yield,边读边写
5. 事务: 整文件 1 个事务,失败全回滚

跑法:  python3 crawler.py
       (环境变量可覆盖: CS_DB_HOST/USER/PASS/DB/THREADS)
"""

import csv
import io
import os
import re
import sys
import time
import json
import threading
import queue
import requests
import pymysql
from concurrent.futures import ThreadPoolExecutor, as_completed
from urllib3.util.retry import Retry
from requests.adapters import HTTPAdapter

# -------- 配置 --------

DB = dict(
    host=os.environ.get("CS_DB_HOST", "127.0.0.1"),
    port=int(os.environ.get("CS_DB_PORT", "3306")),
    user=os.environ.get("CS_DB_USER", "root"),
    password=os.environ.get("CS_DB_PASS", ""),
    database=os.environ.get("CS_DB", "csrankings"),
    charset="utf8mb4",
    autocommit=False,
)

# csrankings 官方 GitHub raw (主源)
GITHUB_RAW = "https://raw.githubusercontent.com/emeryberger/CSrankings/master"
GITHUB_API = "https://api.github.com"

# 备源 (jsdelivr CDN, 国内更稳)
JSDELIVR = "https://cdn.jsdelivr.net/gh/emeryberger/CSrankings@master"

THREADS = int(os.environ.get("CS_THREADS", "8"))
INSERT_BATCH = 5000

# 27 个一级领域 (从 csrankings.js 解析出来)
TOP_AREAS = {
    "ai": "AI",
    "vision": "Vision",
    "mlmining": "ML",
    "nlp": "NLP",
    "inforet": "Web+IR",
    "arch": "Arch",
    "comm": "Networks",
    "sec": "Security",
    "mod": "DB",
    "bed": "Embedded",
    "hpc": "HPC",
    "mobile": "Mobile",
    "metrics": "Metrics",
    "ops": "OS",
    "plan": "PL",
    "soft": "SE",
    "act": "Theory",
    "crypt": "Crypto",
    "log": "Logic",
    "bio": "Comp. Bio",
    "graph": "Graphics",
    "csed": "CSEd",
    "ecom": "ECom",
    "chi": "HCI",
    "robotics": "Robotics",
    "da": "EDA",
    "visualization": "Visualization",
}

# 67 个子领域会议 (parent_code, top_category, is_next_tier)
SUB_AREAS = {
    # AI
    "aaai": ("ai", "ai", 0),
    "ijcai": ("ai", "ai", 0),
    "cvpr": ("vision", "ai", 0),
    "eccv": ("vision", "ai", 0),
    "iccv": ("vision", "ai", 0),
    "icml": ("mlmining", "ai", 0),
    "kdd": ("mlmining", "ai", 1),       # next tier
    "iclr": ("mlmining", "ai", 0),
    "nips": ("mlmining", "ai", 0),
    "acl": ("nlp", "ai", 0),
    "emnlp": ("nlp", "ai", 0),
    "naacl": ("nlp", "ai", 0),
    "sigir": ("inforet", "ai", 0),
    "www": ("inforet", "ai", 0),
    # Systems
    "asplos": ("arch", "systems", 0),
    "isca": ("arch", "systems", 0),
    "micro": ("arch", "systems", 0),
    "hpca": ("arch", "systems", 1),     # next tier
    "sigcomm": ("comm", "systems", 0),
    "nsdi": ("comm", "systems", 0),
    "ccs": ("sec", "systems", 0),
    "oakland": ("sec", "systems", 0),
    "usenixsec": ("sec", "systems", 0),
    "ndss": ("sec", "systems", 1),      # next tier
    "sigmod": ("mod", "systems", 0),
    "vldb": ("mod", "systems", 0),
    "icde": ("mod", "systems", 1),      # next tier
    "pods": ("mod", "systems", 1),      # next tier
    "sc": ("hpc", "systems", 0),
    "hpdc": ("hpc", "systems", 0),
    "ics": ("hpc", "systems", 0),
    "mobicom": ("mobile", "systems", 0),
    "mobisys": ("mobile", "systems", 0),
    "sensys": ("mobile", "systems", 0),
    "imc": ("metrics", "systems", 0),
    "sigmetrics": ("metrics", "systems", 0),
    "sosp": ("ops", "systems", 0),
    "osdi": ("ops", "systems", 0),
    "fast": ("ops", "systems", 1),      # next tier
    "usenixatc": ("ops", "systems", 1), # next tier
    "eurosys": ("ops", "systems", 0),
    "pldi": ("plan", "systems", 0),
    "popl": ("plan", "systems", 0),
    "icfp": ("plan", "systems", 1),     # next tier
    "oopsla": ("plan", "systems", 1),   # next tier
    "fse": ("soft", "systems", 0),
    "icse": ("soft", "systems", 0),
    "ase": ("soft", "systems", 1),      # next tier
    "issta": ("soft", "systems", 1),    # next tier
    # Theory
    "focs": ("act", "theory", 0),
    "soda": ("act", "theory", 0),
    "stoc": ("act", "theory", 0),
    "crypto": ("crypt", "theory", 0),
    "eurocrypt": ("crypt", "theory", 0),
    "cav": ("log", "theory", 0),
    "lics": ("log", "theory", 0),
    # Interdisciplinary
    "ismb": ("bio", "interdisciplinary", 0),
    "recomb": ("bio", "interdisciplinary", 0),
    "siggraph": ("graph", "interdisciplinary", 0),
    "siggraph-asia": ("graph", "interdisciplinary", 0),
    "eurographics": ("graph", "interdisciplinary", 1),  # next tier
    "chiconf": ("chi", "interdisciplinary", 0),
    "ubicomp": ("chi", "interdisciplinary", 0),
    "uist": ("chi", "interdisciplinary", 0),
    "icra": ("robotics", "interdisciplinary", 0),
    "iros": ("robotics", "interdisciplinary", 0),
    "rss": ("robotics", "interdisciplinary", 0),
    "vis": ("visualization", "interdisciplinary", 0),
    "vr": ("visualization", "interdisciplinary", 0),
    "ec": ("ecom", "interdisciplinary", 0),
    "wine": ("ecom", "interdisciplinary", 0),
    "sigcse": ("csed", "interdisciplinary", 0),
    "dac": ("da", "interdisciplinary", 0),
    "iccad": ("da", "interdisciplinary", 0),
    "emsoft": ("bed", "interdisciplinary", 0),
    "rtas": ("bed", "interdisciplinary", 0),
    "rtss": ("bed", "interdisciplinary", 0),
}


# -------- 工具 --------

def make_session():
    """带重试的 requests Session"""
    s = requests.Session()
    retry = Retry(
        total=5, backoff_factor=0.5,
        status_forcelist=[429, 500, 502, 503, 504],
        allowed_methods=frozenset(["GET", "HEAD"]),
    )
    ad = HTTPAdapter(max_retries=retry, pool_connections=8, pool_maxsize=16)
    s.mount("https://", ad)
    s.mount("http://", ad)
    s.headers.update({
        "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                      "AppleWebKit/537.36 (KHTML, like Gecko) "
                      "Chrome/124.0.0.0 Safari/537.36",
    })
    return s


def fetch_text(url, sess=None, timeout=60):
    """下载文本,失败 fallback 到 jsdelivr"""
    s = sess or make_session()
    last = None
    for src in (url,):
        try:
            r = s.get(src, timeout=timeout)
            r.raise_for_status()
            return r.text
        except Exception as e:
            last = e
            # GitHub 失败时尝试 jsdelivr
            if "raw.githubusercontent.com" in src:
                alt = src.replace("https://raw.githubusercontent.com/emeryberger/CSrankings/master",
                                  "https://cdn.jsdelivr.net/gh/emeryberger/CSrankings@master")
                try:
                    r = s.get(alt, timeout=timeout)
                    r.raise_for_status()
                    print(f"  [fallback] {src.split('/')[-1]} ← jsdelivr")
                    return r.text
                except Exception as e2:
                    last = e2
    raise last


def db_connect():
    return pymysql.connect(**DB)


def db_exec_batch(cur, sql_template, rows, batch=INSERT_BATCH):
    """批量 INSERT,每 batch 一次 executemany"""
    total = 0
    buf = []
    for r in rows:
        buf.append(r)
        if len(buf) >= batch:
            cur.executemany(sql_template, buf)
            total += len(buf)
            buf = []
    if buf:
        cur.executemany(sql_template, buf)
        total += len(buf)
    return total


# -------- 5 个数据源 loader --------

def load_countries(conn):
    """countries.csv: 250 国家"""
    print("[1/5] loading countries.csv ...")
    text = fetch_text(f"{GITHUB_RAW}/countries.csv")
    cur = conn.cursor()
    sql = ("INSERT IGNORE INTO country "
           "(name, alpha_2, alpha_3, country_code, iso_3166_2, region, sub_region, "
           "intermediate_region, region_code, sub_region_code, intermediate_region_code) "
           "VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)")
    def parse():
        rdr = csv.DictReader(io.StringIO(text))
        for r in rdr:
            def _int(v):
                try: return int(v) if v else None
                except: return None
            yield (
                r.get("name","").strip(),
                (r.get("alpha_2") or "").strip() or None,
                (r.get("alpha_3") or "").strip() or None,
                _int(r.get("country_code")),
                r.get("iso_3166_2") or None,
                r.get("region") or None,
                r.get("sub_region") or None,
                r.get("intermediate_region") or None,
                _int(r.get("region_code")),
                _int(r.get("sub_region_code")),
                _int(r.get("intermediate_region_code")),
            )
    n = db_exec_batch(cur, sql, parse())
    conn.commit()
    cur.close()
    print(f"      → {n} countries")


def load_institutions(conn):
    """institutions.csv: 741 院校"""
    print("[2/5] loading institutions.csv ...")
    text = fetch_text(f"{GITHUB_RAW}/institutions.csv")
    cur = conn.cursor()
    sql = ("INSERT IGNORE INTO institution (name, region, country_alpha2, homepage) "
           "VALUES (%s,%s,%s,%s)")
    def parse():
        rdr = csv.DictReader(io.StringIO(text))
        for r in rdr:
            yield (
                r["institution"].strip(),
                r.get("region","").strip() or None,
                (r.get("countryabbrv") or "").strip() or None,
                r.get("homepage") or None,
            )
    n = db_exec_batch(cur, sql, parse())
    conn.commit()
    cur.close()
    print(f"      → {n} institutions")


def load_faculty(conn):
    """csrankings.csv: 34280 教员 (基础档案,后面 publication 表 join dept)"""
    print("[3/5] loading csrankings.csv (faculty) ...")
    text = fetch_text(f"{GITHUB_RAW}/csrankings.csv")
    cur = conn.cursor()
    # 缓存 institution name → id
    cur.execute("SELECT id, name FROM institution")
    inst_map = {n: i for (i, n) in cur.fetchall()}
    print(f"      institution cache: {len(inst_map)}")

    # 补 institutions.csv 没列但 csrankings.csv 出现的 4 个
    # (避免 faculty.institution_id 全为 NULL, 让 publication join 失效)
    ALIASES = {
        "Delhi University":                 ("University of Delhi",   "in", None),
        "Queen's University":               ("Queen's University",     "ca", None),
        "Saint Louis University":           ("Saint Louis University", "us", None),
        "Univ. of Massachusetts Dartmouth": ("University of Massachusetts Dartmouth", "us", None),
    }
    for k, (name, c2, hp) in ALIASES.items():
        if k in inst_map: continue
        cur.execute(
            "INSERT IGNORE INTO institution (name, region, country_alpha2, homepage) VALUES (%s,%s,%s,%s)",
            (name, None, c2, hp))
    conn.commit()
    cur.execute("SELECT id, name FROM institution")
    inst_map = {n: i for (i, n) in cur.fetchall()}
    # 把这 4 个别名字符串映射到真实 institution
    for alias, (real_name, _, _) in ALIASES.items():
        if real_name in inst_map:
            inst_map[alias] = inst_map[real_name]
    print(f"      institution cache (after aliases): {len(inst_map)}")

    sql = ("INSERT IGNORE INTO faculty (name, institution_id, homepage, scholar_id, orcid) "
           "VALUES (%s,%s,%s,%s,%s)")
    miss_inst = set()

    def parse():
        rdr = csv.DictReader(io.StringIO(text))
        for r in rdr:
            inst_name = (r.get("affiliation") or "").strip()
            inst_id = inst_map.get(inst_name)
            if inst_id is None and inst_name:
                miss_inst.add(inst_name)
            yield (
                r["name"].strip(),
                inst_id,
                r.get("homepage") or None,
                r.get("scholarid") or None,
                r.get("orcid") or None,
            )
    n = db_exec_batch(cur, sql, parse())
    conn.commit()
    cur.close()
    if miss_inst:
        print(f"      → {n} faculty, {len(miss_inst)} depts not in institutions.csv (first 5: {list(miss_inst)[:5]})")
    else:
        print(f"      → {n} faculty (all dept matched)")


def load_areas_and_ranks(conn):
    """解析 csrankings.js → 27 一级 + 67 子领域,再灌 generated-author-info.csv → 论文计数"""
    # 1) 领域字典表
    print("[4/5] loading research areas ...")
    cur = conn.cursor()
    rows = []
    for code, title in TOP_AREAS.items():
        rows.append((code, title, None, None, 1, 0))
    for code, (parent, cat, is_next) in SUB_AREAS.items():
        rows.append((code, code, parent, cat, 0, 1 if is_next else 0))
    cur.executemany(
        "INSERT IGNORE INTO research_area (code, title, parent_code, top_category, is_top_tier, is_next_tier) "
        "VALUES (%s,%s,%s,%s,%s,%s)", rows)
    conn.commit()
    cur.close()
    print(f"      → {len(rows)} areas (27 top + {len(SUB_AREAS)} sub)")

    # 2) 论文计数表
    print("[5/5] loading generated-author-info.csv (faculty × area × year) ...")
    text = fetch_text(f"{GITHUB_RAW}/generated-author-info.csv")
    cur = conn.cursor()

    # cache: faculty (name, dept) → id
    # 这里 dept 就是 institution name, faculty 表 unique (name, institution_id)
    cur.execute("""
        SELECT f.id, f.name, i.name
        FROM faculty f
        LEFT JOIN institution i ON i.id = f.institution_id
    """)
    fac_cache = {}
    for fid, fname, idept in cur.fetchall():
        fac_cache[(fname, idept)] = fid
    print(f"      faculty cache: {len(fac_cache)}")

    # 没有 institution 的教员, 单独再查一次 (csrankings.csv 一定有 affiliation)
    cur.execute("""
        SELECT f.id, f.name FROM faculty f WHERE f.institution_id IS NULL
    """)
    null_fac_by_name = {n: i for (i, n) in cur.fetchall()}
    print(f"      faculty w/o institution: {len(null_fac_by_name)}")

    # 论文计数表 cache: (name, dept) → faculty_id
    sql = ("INSERT IGNORE INTO faculty_publication_count "
           "(faculty_id, area_code, year, count_pubs, adjusted_count) "
           "VALUES (%s,%s,%s,%s,%s)")

    miss = 0
    buf = []
    def flush():
        nonlocal buf
        if not buf: return
        cur.executemany(sql, buf)
        buf = []

    rdr = csv.DictReader(io.StringIO(text))
    # generated-author-info.csv header (带引号): "name","dept","area","count","adjustedcount","year"
    fieldmap = {k.lower().strip('"'): k for k in rdr.fieldnames}
    name_k   = fieldmap["name"]
    dept_k   = fieldmap["dept"]
    area_k   = fieldmap["area"]
    count_k  = fieldmap["count"]
    adj_k    = fieldmap["adjustedcount"]
    year_k   = fieldmap["year"]

    total = 0
    bad = 0
    for r in rdr:
        n = r[name_k].strip()
        d = r[dept_k].strip()
        a = r[area_k].strip() if r[area_k] else ""
        # 过滤坏行 (Clayton Scott 1 行 area/count/year 全空)
        if not a or not r[count_k] or not r[year_k]:
            bad += 1
            continue
        try:
            c = float(r[count_k])
        except:
            bad += 1
            continue
        try:
            ac = float(r[adj_k])
        except:
            ac = 0.0
        try:
            y = int(r[year_k])
        except:
            bad += 1
            continue

        fid = fac_cache.get((n, d))
        if fid is None:
            # 尝试 null institution 的 fallback (name match)
            fid = null_fac_by_name.get(n)
        if fid is None:
            miss += 1
            continue
        buf.append((fid, a, y, c, ac))
        if len(buf) >= INSERT_BATCH:
            flush()
            total += INSERT_BATCH
    flush()
    total += len(buf)
    conn.commit()
    cur.close()
    print(f"      → {total} publication counts ({miss} unmatched faculty, {bad} bad rows skipped)")


def load_honors(conn):
    """turing + acm fellow + 更新 faculty.is_turing / is_acm_fellow"""
    print("[+] loading turing.csv + acm-fellows.csv ...")
    cur = conn.cursor()

    # turing
    turing = set()
    text = fetch_text(f"{GITHUB_RAW}/turing.csv")
    rdr = csv.DictReader(io.StringIO(text))
    rows = []
    for r in rdr:
        n = r["name"].strip()
        y = int(r["year"])
        rows.append((n, y))
        turing.add(n)
    cur.executemany("INSERT IGNORE INTO turing_award (name, year) VALUES (%s,%s)", rows)
    conn.commit()
    print(f"      → {len(rows)} turing")

    # acm fellow
    fellows = set()
    text = fetch_text(f"{GITHUB_RAW}/acm-fellows.csv")
    rdr = csv.DictReader(io.StringIO(text))
    rows = []
    for r in rdr:
        n = r["name"].strip()
        y = int(r["year"])
        rows.append((n, y))
        fellows.add(n)
    cur.executemany("INSERT IGNORE INTO acm_fellow (name, year) VALUES (%s,%s)", rows)
    conn.commit()
    print(f"      → {len(rows)} acm fellow")

    # 回填 faculty 标志 (按 name 匹配,同名可能多家,全标)
    if turing:
        names = list(turing)
        placeholders = ",".join(["%s"] * len(names))
        cur.execute(f"UPDATE faculty SET is_turing = 1 WHERE name IN ({placeholders})", names)
        print(f"      → flagged {cur.rowcount} turing faculty")
    if fellows:
        names = list(fellows)
        placeholders = ",".join(["%s"] * len(names))
        cur.execute(f"UPDATE faculty SET is_acm_fellow = 1 WHERE name IN ({placeholders})", names)
        print(f"      → flagged {cur.rowcount} acm fellow faculty")
    conn.commit()

    # 更新 institution.faculty_count (从 faculty 派生)
    cur.execute("""
        UPDATE institution i
        SET faculty_count = (SELECT COUNT(*) FROM faculty f WHERE f.institution_id = i.id)
    """)
    conn.commit()
    cur.close()
    print(f"      → institution.faculty_count updated")


# -------- 主流程 --------

def main():
    t0 = time.time()
    print(f"=== csrankings.org crawler ===")
    print(f"  DB: {DB['user']}@{DB['host']}:{DB['port']}/{DB['database']}")
    print(f"  threads: {THREADS}\n")

    conn = db_connect()
    try:
        load_countries(conn)
        load_institutions(conn)
        load_faculty(conn)
        load_areas_and_ranks(conn)
        load_honors(conn)

        # 验证
        cur = conn.cursor()
        for t in ("country","institution","faculty","research_area",
                  "faculty_publication_count","turing_award","acm_fellow"):
            cur.execute(f"SELECT COUNT(*) FROM {t}")
            print(f"  {t:32s} = {cur.fetchone()[0]}")
        cur.close()
    finally:
        conn.close()

    print(f"\n=== done in {time.time()-t0:.1f}s ===")


if __name__ == "__main__":
    main()
