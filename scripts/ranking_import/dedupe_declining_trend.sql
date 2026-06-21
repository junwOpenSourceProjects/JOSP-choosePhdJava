-- declining_trend 表去重 + 加唯一索引
-- 问题: 10679 行 / 5629 uniq, usnews 2027 单条记录重复 46 次
-- 根因: 数据源 csv 本身就有重复条目 (推测爬虫/页面多次抓取)
-- 修复: 加 UNIQUE 索引前先备份重复条目, IGNORE 模式去重, 不丢信息(只是删除完全相同的行)

USE computer_rank;

-- 1. 备份重复条目 (审计留底)
CREATE TABLE IF NOT EXISTS _audit_declining_trend_dups_2026_06_21 AS
SELECT * FROM university_rankings_declining_trend
WHERE (university_name_chinese, ranking_year, ranking_category, current_rank_integer, current_rank_raw) IN (
  SELECT university_name_chinese, ranking_year, ranking_category, current_rank_integer, current_rank_raw
  FROM university_rankings_declining_trend
  GROUP BY university_name_chinese, ranking_year, ranking_category, current_rank_integer, current_rank_raw
  HAVING COUNT(*) > 1
);

-- 2. 统计
SELECT '重复行总数' AS metric, COUNT(*) AS cnt FROM _audit_declining_trend_dups_2026_06_21
UNION SELECT '去重后保留行数', 10679 - (SELECT COUNT(*) FROM _audit_declining_trend_dups_2026_06_21) + (
  SELECT COUNT(DISTINCT university_name_chinese, ranking_year, ranking_category, current_rank_integer, current_rank_raw)
  FROM _audit_declining_trend_dups_2026_06_21
);

-- 3. 删除重复条目, 保留 id 最小的 1 行
DELETE d1 FROM university_rankings_declining_trend d1
INNER JOIN university_rankings_declining_trend d2
WHERE d1.id > d2.id
  AND d1.university_name_chinese = d2.university_name_chinese
  AND d1.ranking_year = d2.ranking_year
  AND d1.ranking_category = d2.ranking_category
  AND d1.current_rank_integer = d2.current_rank_integer
  AND d1.current_rank_raw = d2.current_rank_raw;

-- 4. 加 UNIQUE 索引防再次重复
ALTER TABLE university_rankings_declining_trend
  ADD UNIQUE INDEX uniq_declining_dedup (university_name_chinese, ranking_year, ranking_category, current_rank_integer, current_rank_raw);

-- 5. 验证
SELECT '去重后行数' AS metric, COUNT(*) AS cnt FROM university_rankings_declining_trend;