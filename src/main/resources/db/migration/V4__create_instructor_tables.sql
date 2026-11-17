CREATE TABLE instructor_profiles (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT       NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    headline           VARCHAR(150),
    bio                VARCHAR(2000),
    expertise          VARCHAR(255),
    application_status VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    rejection_reason   VARCHAR(1000),
    submitted_at       TIMESTAMP,
    reviewed_at        TIMESTAMP,
    reviewed_by        BIGINT REFERENCES users(id),
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE instructor_qualifications (
    id             BIGSERIAL PRIMARY KEY,
    instructor_id  BIGINT       NOT NULL REFERENCES instructor_profiles(id) ON DELETE CASCADE,
    title          VARCHAR(200) NOT NULL,
    institution    VARCHAR(200) NOT NULL,
    field_of_study VARCHAR(200),
    start_year     INTEGER,
    end_year       INTEGER,
    type           VARCHAR(20)  NOT NULL,
    verified       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE instructor_social_links (
    id            BIGSERIAL PRIMARY KEY,
    instructor_id BIGINT       NOT NULL REFERENCES instructor_profiles(id) ON DELETE CASCADE,
    platform      VARCHAR(20)  NOT NULL,
    url           VARCHAR(500) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_qualifications_instructor ON instructor_qualifications(instructor_id);
CREATE INDEX idx_social_links_instructor ON instructor_social_links(instructor_id);