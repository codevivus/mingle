-- Baseline of the schema Hibernate used to create with ddl-auto=update. Idempotent, see a2srv-service.
CREATE TABLE IF NOT EXISTS events (
    id          BIGSERIAL PRIMARY KEY,
    city        VARCHAR(255),
    description VARCHAR(255),
    end_time    TIMESTAMP(6),
    external_id VARCHAR(255),
    image_url   VARCHAR(255),
    imported    BOOLEAN NOT NULL,
    start_time  TIMESTAMP(6),
    title       VARCHAR(255),
    venue       VARCHAR(255)
);
