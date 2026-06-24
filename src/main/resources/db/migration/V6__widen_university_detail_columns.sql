-- Widen university detail columns to accommodate longer crawled values
ALTER TABLE university
    MODIFY COLUMN website VARCHAR(1024) NULL,
    MODIFY COLUMN motto VARCHAR(1024) NULL,
    MODIFY COLUMN founded_date VARCHAR(256) NULL,
    MODIFY COLUMN address VARCHAR(1024) NULL;
