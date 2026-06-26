-- 合并重复的 University of Pennsylvania
-- 保留 university-of-pennsylvania，合并 university-pennsylvania 的数据后软删除后者

START TRANSACTION;

-- 1. 删除冲突的排名记录（保留 university-of-pennsylvania 的版本）
DELETE re1 FROM ranking_entry re1
JOIN ranking_entry re2
  ON re1.source_id = re2.source_id
  AND re1.subject_id <=> re2.subject_id
  AND re1.year = re2.year
WHERE re1.university_id = 'university-pennsylvania'
  AND re2.university_id = 'university-of-pennsylvania'
  AND re1.deleted = 0
  AND re2.deleted = 0;

-- 2. 将 university-pennsylvania 独有的排名记录迁移到 university-of-pennsylvania
UPDATE ranking_entry
SET university_id = 'university-of-pennsylvania'
WHERE university_id = 'university-pennsylvania'
  AND deleted = 0;

-- 3. 清理重复院校的标签关联（保留的院校已拥有藤校标签）
DELETE FROM university_tag_relation
WHERE university_id = 'university-pennsylvania';

-- 4. 软删除重复院校
UPDATE university
SET deleted = 1, updated_at = CURRENT_TIMESTAMP
WHERE url_id = 'university-pennsylvania';

COMMIT;
