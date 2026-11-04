-- Baseline of the schema Hibernate used to create with ddl-auto=update. Idempotent, see a2srv-service.
CREATE TABLE IF NOT EXISTS tickets (
    id         BIGSERIAL PRIMARY KEY,
    event_id   BIGINT NOT NULL,
    image_data OID,
    status     VARCHAR(255) CHECK (status IN ('VALID', 'USED', 'CANCELLED')),
    user_id    BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tickets_user ON tickets (user_id);
CREATE INDEX IF NOT EXISTS idx_tickets_event ON tickets (event_id);
