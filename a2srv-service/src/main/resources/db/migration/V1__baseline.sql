-- Baseline of the schema Hibernate used to create with ddl-auto=update. Every statement is idempotent so
-- the same file works on an empty database and on one that already has these tables.
CREATE TABLE IF NOT EXISTS users (
    id                BIGSERIAL PRIMARY KEY,
    age               INTEGER,
    bio               VARCHAR(255),
    birth_date        DATE,
    city              VARCHAR(255),
    created_at        TIMESTAMP(6),
    date_preference   VARCHAR(255),
    email             VARCHAR(255) UNIQUE,
    gender            VARCHAR(255),
    is_online         BOOLEAN,
    last_active_at    TIMESTAMP(6),
    location_lat      REAL,
    location_lon      REAL,
    name              VARCHAR(255),
    password_hash     VARCHAR(255),
    photo_url         VARCHAR(255),
    relationship_goal VARCHAR(255),
    role              VARCHAR(255) CHECK (role IN ('USER', 'ADMIN')),
    surname           VARCHAR(255),
    updated_at        TIMESTAMP(6),
    username          VARCHAR(255) UNIQUE,
    verified          BOOLEAN
);

CREATE TABLE IF NOT EXISTS user_interests (
    user_id  BIGINT NOT NULL REFERENCES users (id),
    interest VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id         BIGSERIAL PRIMARY KEY,
    expires_at TIMESTAMPTZ  NOT NULL,
    token_hash VARCHAR(64)  NOT NULL UNIQUE,
    used_at    TIMESTAMPTZ,
    user_id    BIGINT       NOT NULL
);
