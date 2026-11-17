CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    admin_id    BIGINT       NOT NULL REFERENCES users(id),
    action      VARCHAR(50)  NOT NULL,
    target_type VARCHAR(50)  NOT NULL,
    target_id   BIGINT       NOT NULL,
    details     VARCHAR(1000),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);