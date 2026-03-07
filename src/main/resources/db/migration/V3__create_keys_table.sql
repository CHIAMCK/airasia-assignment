CREATE TABLE keys (
    id         BIGSERIAL    PRIMARY KEY,
    key_value  VARCHAR(16)  NOT NULL UNIQUE,
    is_used    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_keys_key_value ON keys(key_value);
CREATE INDEX idx_keys_is_used ON keys(is_used) WHERE is_used = FALSE;
