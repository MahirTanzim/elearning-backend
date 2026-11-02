CREATE TABLE categories (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(120) NOT NULL UNIQUE
);

INSERT INTO categories (name, slug) VALUES
    ('Programming', 'programming'),
    ('Data Science', 'data-science'),
    ('Design', 'design'),
    ('Business', 'business'),
    ('Marketing', 'marketing'),
    ('Languages', 'languages'),
    ('Personal Development', 'personal-development'),
    ('Photography and Video', 'photography-and-video'),
    ('Other', 'other');

CREATE TABLE courses (
    id               BIGSERIAL PRIMARY KEY,
    instructor_id    BIGINT        NOT NULL REFERENCES instructor_profiles(id),
    category_id      BIGINT        NOT NULL REFERENCES categories(id),
    title            VARCHAR(120)  NOT NULL,
    slug             VARCHAR(150)  NOT NULL UNIQUE,
    subtitle         VARCHAR(200),
    description      VARCHAR(5000),
    level            VARCHAR(20)   NOT NULL,
    language         VARCHAR(30)   NOT NULL,
    price            NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (price >= 0),
    currency         VARCHAR(3)    NOT NULL DEFAULT 'USD',
    what_you_learn   VARCHAR(2000),
    requirements     VARCHAR(2000),
    status           VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    rejection_reason VARCHAR(1000),
    submitted_at     TIMESTAMP,
    published_at     TIMESTAMP,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_courses_instructor ON courses(instructor_id);
CREATE INDEX idx_courses_status ON courses(status);

CREATE TABLE sections (
    id         BIGSERIAL PRIMARY KEY,
    course_id  BIGINT       NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title      VARCHAR(150) NOT NULL,
    position   INTEGER      NOT NULL
);

CREATE INDEX idx_sections_course ON sections(course_id);

CREATE TABLE lessons (
    id               BIGSERIAL PRIMARY KEY,
    section_id       BIGINT       NOT NULL REFERENCES sections(id) ON DELETE CASCADE,
    title            VARCHAR(150) NOT NULL,
    type             VARCHAR(20)  NOT NULL,
    position         INTEGER      NOT NULL,
    free_preview     BOOLEAN      NOT NULL DEFAULT FALSE,
    description      VARCHAR(2000),
    text_content     VARCHAR(20000),
    duration_seconds INTEGER
);

CREATE INDEX idx_lessons_section ON lessons(section_id);