-- A chat now has two deadlines: lock_at (sending stops, history stays readable, status READ_ONLY)
-- and read_until (history is locked too, status LOCKED). Unlocking brings it back to ACTIVE.
ALTER TABLE conversations ADD COLUMN read_until TIMESTAMPTZ;
UPDATE conversations SET read_until = lock_at + INTERVAL '30 days';
ALTER TABLE conversations ALTER COLUMN read_until SET NOT NULL;

ALTER TABLE conversations DROP CONSTRAINT conversations_status_check;
ALTER TABLE conversations ADD CONSTRAINT conversations_status_check
    CHECK (status IN ('ACTIVE', 'READ_ONLY', 'LOCKED'));
ALTER TABLE conversations ALTER COLUMN status TYPE VARCHAR(16);
