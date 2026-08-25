import psycopg2
from psycopg2 import Error

connection = None
cursor = None

try:
    # 连接到PostgreSQL数据库
    connection = psycopg2.connect(
        user="postgres",
        password="123456",
        host="localhost",
        port="5433",
        database="new-exam"
    )
    cursor = connection.cursor()
    print("成功连接到 PostgreSQL 数据库 new-exam")

    # 定义建表 SQL
    create_tables_sql = """
    -- 1. 角色表
    CREATE TABLE IF NOT EXISTS sys_role (
        role_id BIGSERIAL PRIMARY KEY,
        role_name VARCHAR(50) NOT NULL,
        role_code VARCHAR(50) NOT NULL UNIQUE,
        description VARCHAR(255),
        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    -- 2. 权限表
    CREATE TABLE IF NOT EXISTS sys_permission (
        permission_id BIGSERIAL PRIMARY KEY,
        parent_id BIGINT DEFAULT 0,
        permission_name VARCHAR(50) NOT NULL,
        permission_code VARCHAR(100) NOT NULL UNIQUE,
        path VARCHAR(200),
        type INT DEFAULT 1, -- 1: 菜单 2: 按钮 3: 接口
        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    -- 3. 用户角色关联表
    CREATE TABLE IF NOT EXISTS sys_user_role (
        user_id BIGINT NOT NULL,
        role_id BIGINT NOT NULL,
        PRIMARY KEY (user_id, role_id)
    );

    -- 4. 角色权限关联表
    CREATE TABLE IF NOT EXISTS sys_role_permission (
        role_id BIGINT NOT NULL,
        permission_id BIGINT NOT NULL,
        PRIMARY KEY (role_id, permission_id)
    );

    -- 5. 考试记录表
    CREATE TABLE IF NOT EXISTS exam_record (
        record_id BIGSERIAL PRIMARY KEY,
        user_id BIGINT NOT NULL,
        exam_id BIGINT NOT NULL,
        total_score DECIMAL(5,2) DEFAULT 0.00,
        status INT DEFAULT 0, -- 0: 进行中, 1: 已交卷待批改, 2: 已批改
        start_time TIMESTAMP,
        end_time TIMESTAMP,
        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    -- 6. 用户答题明细表
    CREATE TABLE IF NOT EXISTS user_answer (
        answer_id BIGSERIAL PRIMARY KEY,
        record_id BIGINT NOT NULL,
        question_id BIGINT NOT NULL,
        user_answer TEXT,
        is_correct BOOLEAN,
        score DECIMAL(5,2) DEFAULT 0.00,
        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    -- 7. 审计日志表
    CREATE TABLE IF NOT EXISTS audit_log (
        log_id BIGSERIAL PRIMARY KEY,
        user_id BIGINT,
        action VARCHAR(100) NOT NULL,
        ip_address VARCHAR(50),
        request_params TEXT,
        duration BIGINT, -- 耗时(ms)
        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    -- 8. 错题本表
    CREATE TABLE IF NOT EXISTS error_book (
        error_id BIGSERIAL PRIMARY KEY,
        user_id BIGINT NOT NULL,
        question_id BIGINT NOT NULL,
        exam_id BIGINT,
        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    -- 9. 兼容考试及格分数字段
    ALTER TABLE IF EXISTS exam ADD COLUMN IF NOT EXISTS pass_score DECIMAL(5,2) DEFAULT 60.00;

    -- 关键业务幂等约束，防止并发交卷、保存答案或生成错题时出现重复数据。
    CREATE UNIQUE INDEX IF NOT EXISTS uk_exam_record_user_exam ON exam_record (user_id, exam_id);
    CREATE UNIQUE INDEX IF NOT EXISTS uk_user_answer_record_question ON user_answer (record_id, question_id);
    CREATE UNIQUE INDEX IF NOT EXISTS uk_error_book_user_question_exam ON error_book (user_id, question_id, exam_id);
    """

    # 执行建表
    cursor.execute(create_tables_sql)
    connection.commit()
    print("表结构创建完成！")

    # 插入测试数据
    insert_data_sql = """
    -- 插入测试角色
    INSERT INTO sys_role (role_name, role_code, description) VALUES
    ('超级管理员', 'admin', '拥有所有权限'),
    ('普通用户', 'student', '只能参加考试和查看自己记录'),
    ('教师', 'teacher', '可以管理考试和判卷')
    ON CONFLICT (role_code) DO NOTHING;

    -- 插入测试权限
    INSERT INTO sys_permission (permission_name, permission_code, path, type) VALUES
    ('考试管理', 'exam:manage', '/exam', 1),
    ('在线考试', 'exam:online', '/online', 1),
    ('判卷复改', 'exam:remark', '/remark', 2)
    ON CONFLICT (permission_code) DO NOTHING;
    """

    cursor.execute(insert_data_sql)
    connection.commit()
    print("测试数据插入完成！")

except (Exception, Error) as error:
    print("连接或执行时发生错误:", error)
finally:
    if connection:
        cursor.close()
        connection.close()
        print("PostgreSQL 连接已关闭")
