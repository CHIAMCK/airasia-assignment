CREATE SEQUENCE url_mapping_seq;

CREATE TABLE "user" (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(255),
    email      VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

CREATE TABLE url_mapping (
    short_code     VARCHAR(16)  PRIMARY KEY,
    original_url   TEXT         NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id        BIGINT       NOT NULL REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE INDEX idx_url_mapping_user_id ON url_mapping(user_id);
