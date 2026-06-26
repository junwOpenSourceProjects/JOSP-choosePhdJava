ALTER TABLE ranking_entry
    ADD COLUMN score DECIMAL(10, 3) NULL AFTER rank_value;
