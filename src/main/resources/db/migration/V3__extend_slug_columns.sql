-- Extend slug/name_zh columns to accommodate long subject ranking source slugs
ALTER TABLE ranking_source MODIFY COLUMN slug VARCHAR(120) NOT NULL UNIQUE;
ALTER TABLE ranking_source MODIFY COLUMN name_zh VARCHAR(120) NOT NULL;
ALTER TABLE subject MODIFY COLUMN slug VARCHAR(120) NOT NULL UNIQUE;
ALTER TABLE subject MODIFY COLUMN name_zh VARCHAR(120) NOT NULL;
