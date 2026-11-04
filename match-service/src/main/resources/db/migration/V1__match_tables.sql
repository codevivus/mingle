CREATE TABLE match_swipes (
    id          BIGSERIAL PRIMARY KEY,
    event_id    BIGINT      NOT NULL,
    from_user   BIGINT      NOT NULL,
    to_user     BIGINT      NOT NULL,
    action      VARCHAR(8)  NOT NULL CHECK (action IN ('LIKE', 'PASS')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_match_swipe UNIQUE (event_id, from_user, to_user)
);

CREATE INDEX idx_match_swipes_to ON match_swipes (event_id, to_user, action);

CREATE TABLE matches (
    id          BIGSERIAL PRIMARY KEY,
    event_id    BIGINT      NOT NULL,
    user_one    BIGINT      NOT NULL,
    user_two    BIGINT      NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_match_pair UNIQUE (event_id, user_one, user_two),
    CONSTRAINT ck_match_order CHECK (user_one < user_two)
);

CREATE INDEX idx_matches_user_one ON matches (user_one);
CREATE INDEX idx_matches_user_two ON matches (user_two);
