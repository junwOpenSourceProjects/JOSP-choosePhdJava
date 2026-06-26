"""
预测 2026-2030 院校潜力榜
基于 2020-2025 (6 完整年) 的 total_adjusted_count 线性外推
斜率 * (2026..2030) + 截距 = 预测 adjusted_count
潜力 score = 预测的 2028 年绝对量 * (1 + growth_pct)
"""
import pymysql

conn = pymysql.connect(host='127.0.0.1', user='root', password='', database='csrankings', charset='utf8mb4')
cur = conn.cursor(pymysql.cursors.DictCursor)

# 1) 读 2020-2025 全年的院校聚合
cur.execute("""
    SELECT f.institution_id, i.name AS inst, i.name_zh, i.country_alpha2,
           fpc.year,
           ROUND(SUM(fpc.adjusted_count), 2) AS adj
    FROM faculty_publication_count fpc
    JOIN faculty f ON f.id = fpc.faculty_id
    JOIN institution i ON i.id = f.institution_id
    WHERE fpc.year BETWEEN 2020 AND 2025
    GROUP BY f.institution_id, fpc.year
    ORDER BY f.institution_id, fpc.year
""")
rows = cur.fetchall()

# 2) 按院校 group
from collections import defaultdict
by_inst = defaultdict(list)
for r in rows:
    by_inst[r['inst']].append((r['year'], r['adj']))

# 3) 线性回归 + 预测 2026-2030
def linear_pred(pts, target_year):
    """pts = [(year, adj)] ; 返回 (predicted_adj, slope, intercept, growth_pct_2020_2025)"""
    n = len(pts)
    if n < 3:
        return None, 0, 0, 0
    sy = sum(y for _, y in pts)
    sx = sum(x for x, _ in pts)
    sxy = sum(x*y for x, y in pts)
    sxx = sum(x*x for x, _ in pts)
    denom = n*sxx - sx*sx
    if denom == 0:
        return None, 0, 0, 0
    slope = (n*sxy - sx*sy) / denom
    intercept = (sy - slope*sx) / n
    pred = slope * target_year + intercept
    # 2020-2025 平均增速
    first = next(y for x,y in sorted(pts))
    last = next(y for x,y in sorted(pts, reverse=True))
    growth_pct = ((last - first) / first * 100) if first > 0 else 0
    return pred, slope, intercept, growth_pct

results = []
for inst, pts in by_inst.items():
    # 必须 6 年都有数据 (2020-2025)
    years_have = {y for y, _ in pts}
    if not years_have >= {2020, 2021, 2022, 2023, 2024, 2025}:
        continue
    pts_sorted = sorted(pts)
    pred_2026, slope, intercept, growth = linear_pred(pts_sorted, 2026)
    pred_2027 = linear_pred(pts_sorted, 2027)[0]
    pred_2028 = linear_pred(pts_sorted, 2028)[0]
    pred_2030 = linear_pred(pts_sorted, 2030)[0]
    if any(p is None or p < 0 for p in [pred_2026, pred_2028]):
        continue
    # 2025 actual
    adj_2025 = next(y for x, y in pts_sorted if x == 2025)
    name_zh = pts_sorted and pts_sorted  # placeholder; we'll fetch
    results.append({
        'inst': inst,
        'name_zh': None,
        'country_alpha2': None,
        'adj_2025': adj_2025,
        'pred_2026': pred_2026,
        'pred_2028': pred_2028,
        'pred_2030': pred_2030,
        'slope': slope,
        'growth_pct': growth,
    })

# 取 name_zh 和 country
cur.execute("SELECT name, name_zh, country_alpha2 FROM institution")
m = {r['name']: (r['name_zh'], r['country_alpha2']) for r in cur.fetchall()}
for r in results:
    r['name_zh'], r['country_alpha2'] = m.get(r['inst'], (None, None))

# 输出
print(f"样本: {len(results)} 所 2020-2025 6 年数据完整\n")
print(f"{'院校':<35}{'国家':<4}{'2025':>8}{'2026E':>8}{'2028E':>8}{'2030E':>9}{'growth':>8}{'slope':>7}")
print("-" * 95)

# 国内潜力 Top 5
cn_results = [r for r in results if r['country_alpha2'] == 'cn']
cn_results.sort(key=lambda r: r['pred_2028'] * (1 + r['growth_pct']/100), reverse=True)
print("\n=== 国内 (CN) 潜力 Top 5 (基于 2020-2025 线性外推) ===")
for r in cn_results[:5]:
    print(f"  {r['name_zh'] or r['inst'][:30]:<35}{r['country_alpha2']:<4}{r['adj_2025']:>8.1f}{r['pred_2026']:>8.1f}{r['pred_2028']:>8.1f}{r['pred_2030']:>9.1f}{r['growth_pct']:>7.1f}%{r['slope']:>7.2f}")

# 国外潜力 Top 5
non_cn = [r for r in results if r['country_alpha2'] != 'cn']
non_cn.sort(key=lambda r: r['pred_2028'] * (1 + r['growth_pct']/100), reverse=True)
print("\n=== 国外潜力 Top 5 ===")
for r in non_cn[:5]:
    print(f"  {r['name_zh'] or r['inst'][:30]:<35}{r['country_alpha2']:<4}{r['adj_2025']:>8.1f}{r['pred_2026']:>8.1f}{r['pred_2028']:>8.1f}{r['pred_2030']:>9.1f}{r['growth_pct']:>7.1f}%{r['slope']:>7.2f}")

# 保存预测到表
cur.execute("""
    CREATE TABLE IF NOT EXISTS institution_future_pred (
        institution_id INT NOT NULL,
        institution_name VARCHAR(200),
        institution_zh VARCHAR(200),
        country_alpha2 CHAR(2),
        adj_2025 DECIMAL(10,2),
        pred_2026 DECIMAL(10,2),
        pred_2027 DECIMAL(10,2),
        pred_2028 DECIMAL(10,2),
        pred_2029 DECIMAL(10,2),
        pred_2030 DECIMAL(10,2),
        growth_pct DECIMAL(8,2),
        slope DECIMAL(10,4),
        PRIMARY KEY (institution_id),
        INDEX idx_country (country_alpha2),
        INDEX idx_pred_2028 (pred_2028)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
""")
cur.execute("TRUNCATE institution_future_pred")
conn.commit()

# 新 cursor 避免之前 state 影响
cur2 = conn.cursor()
cur2.execute("SELECT id, name FROM institution")
inst_id_map = {n: i for i, n in cur2.fetchall()}
print(f"debug: inst_id_map size = {len(inst_id_map)}")
cur2.close()

upd = []
for r in results:
    iid = inst_id_map.get(r['inst'])
    if iid is None: continue
    pts_sorted = sorted(by_inst[r['inst']])
    upd.append((
        iid, r['inst'], r['name_zh'], r['country_alpha2'],
        r['adj_2025'], r['pred_2026'], linear_pred(pts_sorted, 2027)[0],
        r['pred_2028'], linear_pred(pts_sorted, 2029)[0],
        r['pred_2030'], r['growth_pct'], r['slope'],
    ))
print(f"\ndebug: upd 长度 {len(upd)}, inst_id_map 大小 {len(inst_id_map)}")
print(f"debug: results 前 3 个 inst: {[r['inst'] for r in results[:3]]}")
print(f"debug: 结果在 inst_id_map 中找到: {sum(1 for r in results if r['inst'] in inst_id_map)}/{len(results)}")
cur.executemany("""INSERT INTO institution_future_pred
    (institution_id, institution_name, institution_zh, country_alpha2, adj_2025, pred_2026, pred_2027, pred_2028, pred_2029, pred_2030, growth_pct, slope)
    VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)""", upd)
conn.commit()
print(f"\n✓ 写入 institution_future_pred: {len(upd)} 条")

cur.close()
conn.close()