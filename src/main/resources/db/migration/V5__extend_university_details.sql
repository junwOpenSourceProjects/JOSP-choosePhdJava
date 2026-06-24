-- Extend university table with detail fields crawled from menggy
ALTER TABLE university
    ADD COLUMN website VARCHAR(512) NULL AFTER badge_url,
    ADD COLUMN motto VARCHAR(512) NULL AFTER website,
    ADD COLUMN founded_date VARCHAR(32) NULL AFTER motto,
    ADD COLUMN address VARCHAR(512) NULL AFTER founded_date;
