CREATE SEQUENCE url_mapping_seq;

CREATE TABLE "user" (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255),
    email      VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

CREATE TABLE url_mapping (
    short_code   VARCHAR(16)  PRIMARY KEY,
    original_url CLOB         NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id      BIGINT       NOT NULL,
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE INDEX idx_url_mapping_user_id ON url_mapping(user_id);
