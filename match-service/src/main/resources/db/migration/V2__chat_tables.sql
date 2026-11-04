CREATE TABLE conversations (
    id           BIGSERIAL PRIMARY KEY,
    match_id     BIGINT      NOT NULL UNIQUE REFERENCES matches (id) ON DELETE CASCADE,
    event_id     BIGINT      NOT NULL,
    user_one     BIGINT      NOT NULL,
    user_two     BIGINT      NOT NULL,
    status       VARCHAR(8)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'LOCKED')),
    lock_at      TIMESTAMPTZ NOT NULL,
    unlocked_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversations_user_one ON conversations (user_one);
CREATE INDEX idx_conversations_user_two ON conversations (user_two);

CREATE TABLE messages (
    id               BIGSERIAL PRIMARY KEY,
    conversation_id  BIGINT      NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    sender_id        BIGINT      NOT NULL,
    content          TEXT        NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_conversation ON messages (conversation_id, id);
