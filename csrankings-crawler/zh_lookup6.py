"""
第 6 步: 1) 繁简转换 (opencc t2s)
       2) 处理 abbrev_dict_unanalyzed 的 98 条: 用 en 全称查 wiki 拿中文
"""
import pymysql, time, requests
from opencc import OpenCC
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

cc = OpenCC('t2s')

def make_sess():
    s = requests.Session()
    s.mount('https://', HTTPAdapter(max_retries=Retry(total=2, backoff_factor=0.5,
        status_forcelist=[429,500,502,503,504]), pool_connections=4, pool_maxsize=8))
    s.headers['User-Agent'] = 'csrankings-zh/1.0'
    return s

SESS = make_sess()

conn = pymysql.connect(host='127.0.0.1', user='root', password='', database='csrankings', charset='utf8mb4')
cur = conn.cursor()

# 1) 繁简转换 (排除 NULL)
cur.execute("SELECT id, name_zh FROM institution WHERE name_zh IS NOT NULL")
rows = cur.fetchall()
upd = []
for iid, zh in rows:
    zh_s = cc.convert(zh)
    if zh_s != zh:
        upd.append((zh_s, iid))
if upd:
    cur.executemany("UPDATE institution SET name_zh=%s WHERE id=%s", upd)
    conn.commit()
print(f"繁简转换: {len(upd)} 条")

# 2) 把 abbrev_dict_unanalyzed (98 条) 用当前 name_zh(是英文全称) 查 wiki
cur.execute("SELECT id, name_zh FROM institution WHERE name_zh_source='abbrev_dict_unanalyzed' ORDER BY id")
jobs = cur.fetchall()
print(f"abbrev_dict 兜底查 wiki: {len(jobs)}")

def query_wiki(name):
    if not name or name == name.strip() == '':
        return None
    try:
        r = SESS.get('https://zh.wikipedia.org/w/api.php',
            params={'action':'query','titles':name,'prop':'langlinks','lllang':'zh','format':'json','redirects':1},
            timeout=12)
        data = r.json()
        pages = data.get('query', {}).get('pages', {})
        for pid, p in pages.items():
            ll = p.get('langlinks')
            if ll and ll[0].get('*'):
                return cc.convert(ll[0]['*'])
    except Exception:
        return None
    return None

upd2 = []
t0 = time.time()
for i, (iid, en_full) in enumerate(jobs):
    zh = query_wiki(en_full)
    if zh:
        upd2.append((zh, 'wikipedia_zh', iid))
    if (i+1) % 20 == 0:
        print(f"  {i+1}/{len(jobs)} ({(time.time()-t0):.0f}s, hit={len(upd2)})", flush=True)
if upd2:
    cur.executemany("UPDATE institution SET name_zh=%s, name_zh_source=%s WHERE id=%s", upd2)
    conn.commit()
print(f"abbrev_dict wiki 命中: {len(upd2)}")

# 3) 最终统计
cur.execute("SELECT name_zh_source, COUNT(*) FROM institution GROUP BY name_zh_source")
for src, n in cur.fetchall():
    print(f"  {src or 'NULL'}: {n}")

cur.execute("SELECT COUNT(*) FROM institution WHERE name_zh IS NULL")
print(f"剩余 NULL: {cur.fetchone()[0]}")

# 4) 抽样 (再次) - 大陆惯用译名
cur.execute("""SELECT name, name_zh, name_zh_source FROM institution WHERE name IN
    ('Tsinghua University','Peking University','Zhejiang University','Shanghai Jiao Tong University',
     'Stanford University','Massachusetts Inst. of Technology','KAIST','Nanyang Technological University',
     'National University of Singapore','The University of Hong Kong','Hong Kong Polytechnic University')
    ORDER BY name""")
print("\n=== 抽样 ===")
for n, z, s in cur.fetchall():
    print(f"  {n:45s} -> {z:20s} [{s}]")

cur.close()
conn.close()