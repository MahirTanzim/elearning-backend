CREATE TABLE qualification_documents (
    id               BIGSERIAL PRIMARY KEY,
    qualification_id BIGINT       NOT NULL REFERENCES instructor_qualifications(id) ON DELETE CASCADE,
    storage_key      VARCHAR(255) NOT NULL UNIQUE,
    original_name    VARCHAR(255) NOT NULL,
    content_type     VARCHAR(100) NOT NULL,
    size_bytes       BIGINT       NOT NULL,
    uploaded_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_documents_qualification ON qualification_documents(qualification_id);