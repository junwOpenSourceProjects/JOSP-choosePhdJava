# csrankings.org 全量数据爬虫

爬取 csrankings.org 网站的所有排名信息(教员、院校、领域、年份、论文计数、图灵奖、ACM Fellow)到 MySQL。

## 数据源调研结论

**不爬 csrankings.org 网站本身**(网站是 SPA,论文数据是浏览器打开时实时从 DBLP 抓的,反爬门槛极高)。

**爬 csrankings 官方 GitHub 仓库的静态数据文件**(emeryberger/CSrankings),这是 csrankings.org 网站的 source of truth:

| CSV 文件 | 用途 | 行数 |
|---|---|---|
| `csrankings.csv` | 教员主档 (name/affiliation/homepage/scholarid/orcid) | 34280 |
| `institutions.csv` | 院校 (name/region/countryabbrv/homepage) | 741 |
| `countries.csv` | 国家 ISO 3166 (含洲/子洲) | 250 |
| `turing.csv` | 图灵奖 | 93 |
| `acm-fellows.csv` | ACM Fellow | 1619 |
| `generated-author-info.csv` | **教员 × 领域 × 年份 × 论文数(adjust_count)** | 42896 |

`csrankings.js` 解析出 27 个一级领域 + 77 个子领域会议 + 区域定义。

## 数据库

库名: `csrankings`  (建表脚本 `schema.sql`)

```
country                          = 248
institution                      = 744   (含 4 个 alias 补)
faculty                          = 34280 (100% 院校匹配)
research_area                    = 104   (27 一级 + 77 子领域)
faculty_publication_count        = 287203 (教员 × 领域 × 年份 三元组)
turing_award                     = 93
acm_fellow                       = 1619
```

+ 2 视图:
- `v_institution_area_ranking` 院校 × 领域 × 年份 排名
- `v_faculty_ranking` 教员个人累计排名

## 跑法

```bash
mysql -uroot < schema.sql          # 建库
python3 crawler.py                 # 全量跑, 22 秒
```

环境变量 (可选):
```
CS_DB_HOST=127.0.0.1
CS_DB_USER=root
CS_DB_PASS=
CS_DB=csrankings
CS_THREADS=8
```

## 设计要点

1. **防封**: 数据源是 GitHub raw + jsdelivr CDN,不用 DBLP(会 ban)。请求带 UA,5 次重试(429/5xx)
2. **限速**: GitHub raw 匿名 60 req/h 限制(够用,本项目只 5 个 GET)
3. **多线程**: `requests.Session` 池化(8 连接) + 批量 INSERT 5000 一批
4. **容错**: 坏行(Michig 拼错 1 行)跳过,fallback 到 jsdelivr 镜像
5. **真源 + 备源**: GitHub 失败自动切 jsdelivr CDN
6. **增量**: 全用 `INSERT IGNORE` + UNIQUE 约束,二次跑不会重复

## 已知限制

- 论文"全文"(DBLP XML)未爬 — csrankings 网站本身也只在用户打开页面时实时拉。**如需论文明细,后续用 DBLP 公开 API `https://dblp.org/pid/<hash>.xml` 增量拉**,每个教员 1 次,加 rate-limit 2 req/s。
- 4 个 institution 是 csrankings.csv 出现但 institutions.csv 缺失的(Delhi/Queen's/SLU/UMass Dartmouth),已在 crawler.py 里硬编码 alias 补全。

## 文件

- `schema.sql` - 建库 + 7 表 + 2 视图
- `crawler.py` - 主爬虫 (22 秒全量入库)
- `README.md` - 本文件
