-- Extend rank_display to accommodate trend arrow HTML stripped values
ALTER TABLE ranking_entry MODIFY COLUMN rank_display VARCHAR(64) NOT NULL;
