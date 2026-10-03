-- Idempotency-Key fallback store (spec §7, ADR 0010). A row whose expires_at has passed is free: a claim takes it
-- over, and a purge job removes the rest (session 2, step 7). A NULL response_status means the request still runs.
CREATE TABLE idempotency_keys (
    account_id UUID NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    fingerprint CHAR(64) NOT NULL,
    response_status SMALLINT,
    response_headers TEXT,
    response_body BYTEA,
    expires_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (account_id, idempotency_key),
    CONSTRAINT ck_idempotency_keys_status CHECK (response_status IS NULL OR response_status BETWEEN 100 AND 599),
    CONSTRAINT ck_idempotency_keys_response CHECK (
        (response_status IS NULL AND response_headers IS NULL AND response_body IS NULL)
        OR (response_status IS NOT NULL AND response_headers IS NOT NULL AND response_body IS NOT NULL))
);

CREATE INDEX idx_idempotency_keys_expires_at ON idempotency_keys (expires_at);
