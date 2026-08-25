import psycopg2
from psycopg2 import Error

connection = None
cursor = None

try:
    connection = psycopg2.connect(
        user="postgres",
        password="123456",
        host="localhost",
        port="5433",
        database="new-exam"
    )
    cursor = connection.cursor()
    print("成功连接到 PostgreSQL 数据库 new-exam，准备添加注释...")

    # 包含所有表和字段注释的 SQL 语句
    comment_sql = """
    -- 1. sys_role 角色表
    COMMENT ON TABLE sys_role IS '系统角色表';
    COMMENT ON COLUMN sys_role.role_id IS '角色ID, 主键自增';
    COMMENT ON COLUMN sys_role.role_name IS '角色名称 (如: 超级管理员)';
    COMMENT ON COLUMN sys_role.role_code IS '角色编码 (如: ROLE_ADMIN)';
    COMMENT ON COLUMN sys_role.description IS '角色描述';
    COMMENT ON COLUMN sys_role.create_time IS '创建时间';
    COMMENT ON COLUMN sys_role.update_time IS '更新时间';

    -- 2. sys_permission 权限/菜单表
    COMMENT ON TABLE sys_permission IS '系统权限与菜单表';
    COMMENT ON COLUMN sys_permission.permission_id IS '权限ID, 主键自增';
    COMMENT ON COLUMN sys_permission.parent_id IS '父级权限ID';
    COMMENT ON COLUMN sys_permission.permission_name IS '权限或菜单名称';
    COMMENT ON COLUMN sys_permission.permission_code IS '权限标识码';
    COMMENT ON COLUMN sys_permission.path IS '前端路由路径或后台接口路径';
    COMMENT ON COLUMN sys_permission.type IS '类型: 1-菜单, 2-按钮, 3-接口';
    COMMENT ON COLUMN sys_permission.create_time IS '创建时间';
    COMMENT ON COLUMN sys_permission.update_time IS '更新时间';

    -- 3. sys_user_role 用户角色关联表
    COMMENT ON TABLE sys_user_role IS '用户角色关联表';
    COMMENT ON COLUMN sys_user_role.user_id IS '用户ID';
    COMMENT ON COLUMN sys_user_role.role_id IS '角色ID';

    -- 4. sys_role_permission 角色权限关联表
    COMMENT ON TABLE sys_role_permission IS '角色权限关联表';
    COMMENT ON COLUMN sys_role_permission.role_id IS '角色ID';
    COMMENT ON COLUMN sys_role_permission.permission_id IS '权限ID';

    -- 5. exam_record 考试记录表
    COMMENT ON TABLE exam_record IS '用户考试记录表';
    COMMENT ON COLUMN exam_record.record_id IS '记录ID, 主键自增';
    COMMENT ON COLUMN exam_record.user_id IS '考生ID';
    COMMENT ON COLUMN exam_record.exam_id IS '考试ID';
    COMMENT ON COLUMN exam_record.total_score IS '考试总得分';
    COMMENT ON COLUMN exam_record.status IS '考试状态: 0-进行中, 1-已交卷待批改, 2-已批改';
    COMMENT ON COLUMN exam_record.start_time IS '开始考试时间';
    COMMENT ON COLUMN exam_record.end_time IS '交卷时间';
    COMMENT ON COLUMN exam_record.create_time IS '创建时间';
    COMMENT ON COLUMN exam_record.update_time IS '更新时间';

    -- 6. user_answer 用户答题明细表
    COMMENT ON TABLE user_answer IS '用户答题明细表';
    COMMENT ON COLUMN user_answer.answer_id IS '答题记录ID, 主键自增';
    COMMENT ON COLUMN user_answer.record_id IS '所属考试记录ID';
    COMMENT ON COLUMN user_answer.question_id IS '题目ID';
    COMMENT ON COLUMN user_answer.user_answer IS '考生提交的答案';
    COMMENT ON COLUMN user_answer.is_correct IS '是否正确: true/false';
    COMMENT ON COLUMN user_answer.score IS '该题得分';
    COMMENT ON COLUMN user_answer.create_time IS '创建时间';
    COMMENT ON COLUMN user_answer.update_time IS '更新时间';

    -- 7. audit_log 审计日志表
    COMMENT ON TABLE audit_log IS '系统操作审计日志表';
    COMMENT ON COLUMN audit_log.log_id IS '日志ID, 主键自增';
    COMMENT ON COLUMN audit_log.user_id IS '操作用户ID';
    COMMENT ON COLUMN audit_log.action IS '操作内容描述';
    COMMENT ON COLUMN audit_log.ip_address IS '操作人IP地址';
    COMMENT ON COLUMN audit_log.request_params IS '请求参数(截断存储)';
    COMMENT ON COLUMN audit_log.duration IS '接口执行耗时(毫秒)';
    COMMENT ON COLUMN audit_log.create_time IS '记录时间';

    -- 8. error_book 错题本表
    COMMENT ON TABLE error_book IS '用户错题本表';
    COMMENT ON COLUMN error_book.error_id IS '错题ID, 主键自增';
    COMMENT ON COLUMN error_book.user_id IS '所属用户ID';
    COMMENT ON COLUMN error_book.question_id IS '错题关联的题目ID';
    COMMENT ON COLUMN error_book.exam_id IS '错题产生的考试ID';
    COMMENT ON COLUMN error_book.create_time IS '创建时间';
    COMMENT ON COLUMN error_book.update_time IS '更新时间';

    -- 9. exam 考试主表 (修改之前的)
    ALTER TABLE IF EXISTS exam ADD COLUMN IF NOT EXISTS pass_score DECIMAL(5,2) DEFAULT 60.00;
    DO $$
    BEGIN
        IF to_regclass('public.exam') IS NOT NULL THEN
            EXECUTE 'COMMENT ON COLUMN public.exam.pass_score IS ''考试及格分数''';
        END IF;
    END $$;
    """

    cursor.execute(comment_sql)
    connection.commit()
    print("数据库表及字段注释补充完成！")

except (Exception, Error) as error:
    print("执行时发生错误:", error)
finally:
    if connection:
        cursor.close()
        connection.close()
        print("PostgreSQL 连接已关闭")
