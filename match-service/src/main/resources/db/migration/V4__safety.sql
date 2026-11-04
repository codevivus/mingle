-- Ending a match (unmatch or block) keeps the rows but marks them ended, and closes the conversation for good.
ALTER TABLE matches ADD COLUMN ended_at TIMESTAMPTZ;
ALTER TABLE matches ADD COLUMN ended_by BIGINT;
ALTER TABLE conversations ADD COLUMN closed_at TIMESTAMPTZ;

CREATE TABLE blocks (
    id         BIGSERIAL PRIMARY KEY,
    blocker    BIGINT      NOT NULL,
    blocked    BIGINT      NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_block UNIQUE (blocker, blocked),
    CONSTRAINT ck_block_self CHECK (blocker <> blocked)
);

CREATE INDEX idx_blocks_blocked ON blocks (blocked);

CREATE TABLE reports (
    id          BIGSERIAL PRIMARY KEY,
    reporter    BIGINT      NOT NULL,
    reported    BIGINT      NOT NULL,
    event_id    BIGINT,
    reason      VARCHAR(32) NOT NULL
        CHECK (reason IN ('SPAM', 'HARASSMENT', 'FAKE_PROFILE', 'INAPPROPRIATE_CONTENT', 'UNDERAGE', 'OTHER')),
    note        TEXT,
    status      VARCHAR(16) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ,
    resolved_by BIGINT
);

CREATE INDEX idx_reports_status ON reports (status, created_at);
CREATE INDEX idx_reports_reported ON reports (reported);
