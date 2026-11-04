-- Highest message id each participant has read (0 = nothing yet). user_one / user_two follow the match columns.
ALTER TABLE conversations ADD COLUMN last_read_one BIGINT NOT NULL DEFAULT 0;
ALTER TABLE conversations ADD COLUMN last_read_two BIGINT NOT NULL DEFAULT 0;
