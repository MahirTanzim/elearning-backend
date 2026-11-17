ALTER TABLE courses ADD COLUMN thumbnail_key VARCHAR(255);

ALTER TABLE lessons
    ADD COLUMN file_key          VARCHAR(255),
    ADD COLUMN file_name         VARCHAR(255),
    ADD COLUMN file_content_type VARCHAR(100),
    ADD COLUMN file_size_bytes   BIGINT;