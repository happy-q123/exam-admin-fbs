-- 在线考试平台升级迁移脚本
-- 在已有基础库上执行，可重复执行。生产环境请先备份并由迁移工具执行。
BEGIN;

CREATE INDEX IF NOT EXISTS idx_online_exam_answer_user_exam
    ON user_online_exam_answer (user_id, exam_id);
CREATE INDEX IF NOT EXISTS idx_online_exam_options_user_exam_time
    ON user_online_exam_options (user_id, exam_id, option_time DESC);

CREATE TABLE IF NOT EXISTS ai_agent_run (
    run_id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    conversation_id BIGINT,
    question_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'RUNNING',
    attempts INTEGER NOT NULL DEFAULT 0,
    quality_passed BOOLEAN NOT NULL DEFAULT FALSE,
    quality_note TEXT,
    model_name VARCHAR(128),
    prompt_version VARCHAR(64),
    latency_ms BIGINT,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_agent_run_user_time
    ON ai_agent_run (user_id, created_time DESC);
CREATE INDEX IF NOT EXISTS idx_ai_agent_run_conversation
    ON ai_agent_run (conversation_id, created_time DESC);

CREATE TABLE IF NOT EXISTS ai_agent_step (
    id BIGSERIAL PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    agent_name VARCHAR(64) NOT NULL,
    step_order INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    input_summary TEXT,
    output_summary TEXT,
    latency_ms BIGINT,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_agent_step_run_order
    ON ai_agent_step (run_id, step_order);

CREATE TABLE IF NOT EXISTS ai_answer_feedback (
    id BIGSERIAL PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    rating VARCHAR(16) NOT NULL,
    reason VARCHAR(255),
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_feedback_run ON ai_answer_feedback (run_id);
CREATE INDEX IF NOT EXISTS idx_ai_feedback_user_time ON ai_answer_feedback (user_id, created_time DESC);

CREATE TABLE IF NOT EXISTS ai_knowledge_document (
    id BIGSERIAL PRIMARY KEY,
    document_key VARCHAR(128) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_uri VARCHAR(500),
    course_id BIGINT,
    version VARCHAR(64) NOT NULL,
    content_hash VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_by BIGINT,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_knowledge_document_course
    ON ai_knowledge_document (course_id, status, review_status);

CREATE TABLE IF NOT EXISTS ai_knowledge_chunk (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    chunk_key VARCHAR(160) NOT NULL UNIQUE,
    chunk_order INTEGER NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    course_id BIGINT,
    knowledge_point_id BIGINT,
    question_id BIGINT,
    content_hash VARCHAR(128) NOT NULL,
    embedding_model VARCHAR(128),
    review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_knowledge_chunk_document
    ON ai_knowledge_chunk (document_id, chunk_order);
CREATE INDEX IF NOT EXISTS idx_ai_knowledge_chunk_scope
    ON ai_knowledge_chunk (course_id, knowledge_point_id, review_status);
CREATE INDEX IF NOT EXISTS idx_ai_knowledge_chunk_metadata
    ON ai_knowledge_chunk USING GIN (metadata);

-- PGVector 的正式向量表。embedding 维度必须与实际 EmbeddingModel 一致；当前本地模型为 768。
CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE IF NOT EXISTS ai_knowledge_vector (
    id VARCHAR(128) PRIMARY KEY,
    content TEXT NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    embedding vector(768) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ai_knowledge_vector_embedding
    ON ai_knowledge_vector USING hnsw (embedding vector_cosine_ops);

CREATE TABLE IF NOT EXISTS proctor_event (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_proctor_event_exam_time
    ON proctor_event (exam_id, created_time DESC);
CREATE INDEX IF NOT EXISTS idx_proctor_event_student_time
    ON proctor_event (student_id, created_time DESC);

COMMIT;
