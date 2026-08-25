-- 新增考试记录、答题明细、错题本、审计日志及及格分字段。
-- 在已有基础库上执行；重复执行不会破坏已有数据。
BEGIN;

ALTER TABLE IF EXISTS exam
    ADD COLUMN IF NOT EXISTS pass_score NUMERIC(5, 2) DEFAULT 60.00;

CREATE TABLE IF NOT EXISTS sys_role (
    role_id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_permission (
    permission_id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    permission_name VARCHAR(50) NOT NULL,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    path VARCHAR(200),
    type INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS exam_record (
    record_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    total_score NUMERIC(5, 2) DEFAULT 0.00,
    status INTEGER NOT NULL DEFAULT 0,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_exam_record_user_id ON exam_record (user_id);
CREATE INDEX IF NOT EXISTS idx_exam_record_exam_id ON exam_record (exam_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_exam_record_user_exam ON exam_record (user_id, exam_id);

CREATE TABLE IF NOT EXISTS user_answer (
    answer_id BIGSERIAL PRIMARY KEY,
    record_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    user_answer TEXT,
    is_correct BOOLEAN,
    score NUMERIC(5, 2) DEFAULT 0.00,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_answer_record_id ON user_answer (record_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_answer_record_question ON user_answer (record_id, question_id);

CREATE TABLE IF NOT EXISTS error_book (
    error_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    exam_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_error_book_user_id ON error_book (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_error_book_user_question_exam
    ON error_book (user_id, question_id, exam_id);

CREATE TABLE IF NOT EXISTS audit_log (
    log_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    ip_address VARCHAR(50),
    request_params TEXT,
    duration BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_log_create_time ON audit_log (create_time DESC);

COMMIT;
