"""
第三轮: 只跑 wiki 还 pending 的, 限速 2 req/s (Wiki 友好)
"""
import csv, json, time, requests, pymysql, sys
from concurrent.futures import ThreadPoolExecutor, as_completed
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

def make_sess():
    s = requests.Session()
    s.mount('https://', HTTPAdapter(max_retries=Retry(total=2, backoff_factor=0.5,
        status_forcelist=[429,500,502,503,504]),
        pool_connections=8, pool_maxsize=16))
    s.headers['User-Agent'] = 'csrankings-zh-lookup/1.0 (research; mailto:test@test.com)'
    return s

SESS = make_sess()

def query_wiki(name):
    try:
        r = SESS.get('https://zh.wikipedia.org/w/api.php',
            params={'action':'query','titles':name,'prop':'langlinks','lllang':'zh','format':'json','redirects':1},
            timeout=12)
        data = r.json()
        pages = data.get('query', {}).get('pages', {})
        for pid, p in pages.items():
            ll = p.get('langlinks')
            if ll and ll[0].get('*'):
                return ll[0]['*']
        r2 = SESS.get('https://zh.wikipedia.org/w/api.php',
            params={'action':'opensearch','search':name,'limit':1,'format':'json'},
            timeout=12)
        d2 = r2.json()
        if len(d2) >= 4 and d2[1]:
            return d2[1][0]
    except Exception as e:
        return f"__ERR__:{type(e).__name__}"
    return None

# 读 DB 当前仍未翻译的
conn = pymysql.connect(host='127.0.0.1', user='root', password='', database='csrankings', charset='utf8mb4')
cur = conn.cursor()
cur.execute("SELECT id, name FROM institution WHERE name_zh IS NULL ORDER BY id")
jobs = cur.fetchall()
cur.close()
conn.close()
print(f"DB 仍未翻译: {len(jobs)}", flush=True)

# 3 线程 + 限速 (Wiki 友好)
results = {}
t0 = time.time()
done = 0
with ThreadPoolExecutor(max_workers=3) as ex:
    futs = {ex.submit(query_wiki, name): (iid, name) for iid, name in jobs}
    for f in as_completed(futs):
        iid, name = futs[f]
        zh = f.result()
        if zh and not zh.startswith('__ERR__'):
            results[name] = (iid, zh)
        done += 1
        if done % 10 == 0 or done == len(jobs):
            elapsed = time.time() - t0
            rate = done / max(elapsed, 1)
            print(f"  {done}/{len(jobs)} ({elapsed:.0f}s, {rate:.2f} req/s, hit={len(results)})", flush=True)

print(f"wiki 命中 {len(results)} / {len(jobs)}", flush=True)

# 写回
conn = pymysql.connect(host='127.0.0.1', user='root', password='', database='csrankings', charset='utf8mb4')
cur = conn.cursor()
upd = [(zh, 'wikipedia_zh', iid) for name, (iid, zh) in results.items()]
if upd:
    cur.executemany("UPDATE institution SET name_zh=%s, name_zh_source=%s WHERE id=%s", upd)
    conn.commit()
print(f"已更新 {len(upd)} 条", flush=True)

cur.execute("SELECT name_zh_source, COUNT(*) FROM institution GROUP BY name_zh_source")
for src, n in cur.fetchall():
    print(f"  {src or 'NULL'}: {n}")

cur.execute("SELECT COUNT(*) FROM institution WHERE name_zh IS NULL")
print(f"DB 仍未翻译: {cur.fetchone()[0]}")

if cur.fetchone()[0] > 0:
    cur.execute("SELECT id, name, country_alpha2 FROM institution WHERE name_zh IS NULL ORDER BY id")
    print("\n=== 仍未翻译的院校 ===")
    for iid, name, c in cur.fetchall():
        print(f"  [{iid}] {name} ({c})")

cur.close()
conn.close()
