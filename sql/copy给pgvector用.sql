--
-- PostgreSQL database dump
-- Modified for Docker/Linux Compatibility
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: new-exam; Type: DATABASE; Schema: -; Owner: postgres
-- 
-- 修改说明：移除了 Windows 特定的 LOCALE 设置，仅保留 UTF8 编码
--

CREATE DATABASE "new-exam" WITH ENCODING = 'UTF8';

ALTER DATABASE "new-exam" OWNER TO postgres;

--
-- 连接到新创建的数据库
--
\connect "new-exam"

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: exam; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.exam (
    id bigint NOT NULL,
    title character varying(255) NOT NULL,
    introduce text,
    begin_time timestamp(6) without time zone NOT NULL,
    duration_time integer NOT NULL,
    security_setting jsonb DEFAULT '{}'::jsonb NOT NULL,
    status boolean DEFAULT true NOT NULL,
    create_time timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    latest_update_time timestamp(6) without time zone,
    max_user_num integer,
    rest_user_num integer,
    creator bigint NOT NULL
);


ALTER TABLE public.exam OWNER TO postgres;

--
-- Name: TABLE exam; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.exam IS '考试信息主表';


--
-- Name: COLUMN exam.duration_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.exam.duration_time IS '考试时长(分钟)';


--
-- Name: COLUMN exam.security_setting; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.exam.security_setting IS '安全配置(JSONB)：包含防作弊、监考限制等
 //是否允许提前提交
    private Boolean allowEarlySubmit;

    //是否需要开启摄像头
    private Boolean faceRecognition;

    //最大允许的重连次数
    private Integer maxReconnectCount;
';


--
-- Name: COLUMN exam.rest_user_num; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.exam.rest_user_num IS '剩余考试名额';


--
-- Name: exam_question_relation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.exam_question_relation (
    id bigint NOT NULL,
    exam_id bigint NOT NULL,
    question_id bigint NOT NULL,
    score numeric(5,1) DEFAULT 0.0 NOT NULL,
    seq integer DEFAULT 0 NOT NULL,
    override_props jsonb DEFAULT '{}'::jsonb
);


ALTER TABLE public.exam_question_relation OWNER TO postgres;

--
-- Name: TABLE exam_question_relation; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.exam_question_relation IS '考试题目关联表';


--
-- Name: COLUMN exam_question_relation.score; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.exam_question_relation.score IS '该题在本次考试中的分值';


--
-- Name: COLUMN exam_question_relation.seq; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.exam_question_relation.seq IS '题目在考试中的排列顺序';


--
-- Name: COLUMN exam_question_relation.override_props; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.exam_question_relation.override_props IS '覆盖配置(JSONB)：仅对本次考试生效的题目属性覆盖';


--
-- Name: question; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.question (
    id bigint NOT NULL,
    type character varying(50) NOT NULL,
    difficulty character varying(30),
    creator_id bigint NOT NULL,
    status boolean DEFAULT true NOT NULL,
    body jsonb NOT NULL,
    tags jsonb DEFAULT '[]'::jsonb,
    create_time timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    latest_update_time timestamp(6) without time zone,
    latest_update_id bigint
);


ALTER TABLE public.question OWNER TO postgres;

--
-- Name: TABLE question; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.question IS '题目表';


--
-- Name: COLUMN question.body; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.question.body IS '题干、选项等复杂 JSON 结构
{
  "stem": "Java 中哪个关键字用于定义常量？",
  "stemImg": "https://cdn.example.com/images/q1.png",
  "options": [
    { "key": "A", "val": "var" },
    { "key": "B", "val": "final" },
    { "key": "C", "val": "static" },
    { "key": "D", "val": "const" }
  ],
  "correct": "B",
  "analysis": "final 关键字用于修饰变量，一旦赋值不可更改，即为常量。"
}
{
  "stem": "以下哪些是 Java 的基本数据类型？",
  "options": [
    { "key": "A", "val": "int" },
    { "key": "B", "val": "String" },
    { "key": "C", "val": "boolean" },
    { "key": "D", "val": "Double" }
  ],
  "correct": ["A", "C"],
  "analysis": "int 和 boolean 是基本类型；String 是引用类型；Double 是包装类。"
}
{
  "stem": "PostgreSQL 中的 jsonb 类型比 json 类型查询速度更快。",
  "options": [
    { "key": "T", "val": "正确" },
    { "key": "F", "val": "错误" }
  ],
  "correct": "T",
  "analysis": "jsonb 是二进制存储，支持索引，查询效率通常高于普通 json。"
}
{
  "stem": "请简述什么是依赖注入（DI）？",
  "options": [],
  "correct": "依赖注入是指对象不需要自行创建它所依赖的对象，而是由外部容器在运行时将这些依赖项注入其中。",
  "analysis": "考查对 Spring 核心概念 IoC/DI 的理解。"
';


--
-- Name: COLUMN question.tags; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.question.tags IS '标签列表，JSONB 数组格式';


--
-- Name: sys_oauth2_jwk; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sys_oauth2_jwk (
    id character varying(255) NOT NULL,
    jwk_json text NOT NULL,
    create_time timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.sys_oauth2_jwk OWNER TO postgres;

--
-- Name: TABLE sys_oauth2_jwk; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.sys_oauth2_jwk IS 'OAuth2 JWK 密钥持久化表';


--
-- Name: COLUMN sys_oauth2_jwk.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_oauth2_jwk.id IS '密钥 ID (kid)';


--
-- Name: COLUMN sys_oauth2_jwk.jwk_json; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_oauth2_jwk.jwk_json IS 'JWK 的标准 JSON 格式内容';


--
-- Name: COLUMN sys_oauth2_jwk.create_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_oauth2_jwk.create_time IS '密钥生成时间';


--
-- Name: sys_user; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sys_user (
    id bigint NOT NULL,
    username character varying(50) NOT NULL,
    password character varying(255) NOT NULL,
    nick_name character varying(100),
    role character varying(50),
    status boolean DEFAULT true NOT NULL
);


ALTER TABLE public.sys_user OWNER TO postgres;

--
-- Name: TABLE sys_user; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.sys_user IS '系统用户表';


--
-- Name: COLUMN sys_user.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_user.id IS '雪花算法生成的唯一标识';


--
-- Name: COLUMN sys_user.username; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_user.username IS '登录账号';


--
-- Name: COLUMN sys_user.password; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_user.password IS '加密后的密码';


--
-- Name: COLUMN sys_user.nick_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_user.nick_name IS '显示昵称';


--
-- Name: COLUMN sys_user.role; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_user.role IS '用户权限角色';


--
-- Name: COLUMN sys_user.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_user.status IS '启用状态: true-启用, false-禁用';


--
-- Name: user_apply_exam_relation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_apply_exam_relation (
    id bigint NOT NULL,
    exam_id bigint NOT NULL,
    user_id bigint NOT NULL,
    apply_time timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.user_apply_exam_relation OWNER TO postgres;

--
-- Name: user_online_exam_answer; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_online_exam_answer (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    exam_id bigint NOT NULL,
    question_id bigint NOT NULL,
    answer jsonb DEFAULT '{}'::jsonb NOT NULL,
    score real DEFAULT 0.0,
    is_correct boolean,
    option_time timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.user_online_exam_answer OWNER TO postgres;

--
-- Name: user_online_exam_options; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_online_exam_options (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    exam_id bigint NOT NULL,
    option_type character varying(50) NOT NULL,
    option_time timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.user_online_exam_options OWNER TO postgres;

--
-- Name: TABLE user_online_exam_options; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.user_online_exam_options IS '用户考试行为轨迹日志表';


--
-- Name: COLUMN user_online_exam_options.option_type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.user_online_exam_options.option_type IS '操作类型：如 ENTER(进入), LEAVE(离开), SUBMIT(交卷) 等';


--
-- Name: COLUMN user_online_exam_options.option_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.user_online_exam_options.option_time IS '行为发生的确切时间';


--
-- Data for Name: exam; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.exam (id, title, introduce, begin_time, duration_time, security_setting, status, create_time, latest_update_time, max_user_num, rest_user_num, creator) FROM stdin;
1768978001	2026年Q1 Java 后端核心技术月测	本次考试涵盖 Spring Boot 自动配置原理、Java 并发编程（JUC）以及 RocketMQ 消息可靠性投递等核心知识点。	2026-02-20 09:00:00	120	{"faceRecognition": false, "allowEarlySubmit": true, "maxReconnectCount": 3}	t	2026-01-21 15:15:56.206759	\N	100	100	20251021
1768978002	计算机视觉：高级车道线检测算法专题研究	重点考察 SDLane 与 CCMLane 模型在复杂交通场景下的鲁棒性分析，以及稀疏锚框（Sparse Anchors）的应用。	2026-03-05 14:30:00	90	{"faceRecognition": true, "allowEarlySubmit": false, "maxReconnectCount": 1}	t	2026-01-21 15:15:56.206759	2026-01-21 15:00:00	50	12	20251111
1768978003	Docker 容器化部署与 Linux 系统调优实战	包含 RocketMQ 集群搭建、Dashboard 故障排查以及内存超频引起的系统不稳定性诊断。	2026-03-15 10:00:00	60	{"faceRecognition": false, "allowEarlySubmit": true, "maxReconnectCount": 5}	t	2026-01-21 15:15:56.206759	\N	200	200	20260115
1768978004	LeetCode 算法专项：动态规划与深度优先搜索	侧重于单链表排序算法（归并排序实现）以及动态规划典型题目（如 Coin Change, Word Break）。	2026-01-25 19:00:00	150	{"faceRecognition": true, "allowEarlySubmit": true, "maxReconnectCount": 2}	t	2026-01-21 15:15:56.206759	\N	300	156	20260119
1768978005	2025年度计算机科学与信息学院研究生入学模拟考	针对重庆师范大学计算机学院考研大纲设计的模拟测试。	2025-12-20 08:30:00	180	{"faceRecognition": true, "allowEarlySubmit": false, "maxReconnectCount": 0}	f	2025-11-01 10:00:00	2025-12-21 12:00:00	500	0	20250529
\.


--
-- Data for Name: exam_question_relation; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.exam_question_relation (id, exam_id, question_id, score, seq, override_props) FROM stdin;
\.


--
-- Data for Name: question; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.question (id, type, difficulty, creator_id, status, body, tags, create_time, latest_update_time, latest_update_id) FROM stdin;
1001	SINGLE	MEDIUM	20251021	t	{"stem": "在 RocketMQ 5.x 架构中，负责存储消息并处理读写请求的核心组件是什么？", "correct": "B", "options": [{"key": "A", "val": "NameServer"}, {"key": "B", "val": "Broker"}, {"key": "C", "val": "Producer"}, {"key": "D", "val": "Dashboard"}], "analysis": "Broker 是 RocketMQ 的核心，负责消息的存储、传递和查询；NameServer 仅负责元数据管理。"}	["RocketMQ", "中间件"]	2026-01-21 15:18:19.978038	\N	\N
1002	MULTIPLE	HARD	20260115	t	{"stem": "关于 Java 中的 ReentrantLock，以下说法正确的是？", "correct": ["A", "B", "D"], "options": [{"key": "A", "val": "支持公平锁和非公平锁模式"}, {"key": "B", "val": "必须手动释放锁，通常在 finally 块中操作"}, {"key": "C", "val": "属于不可重入锁"}, {"key": "D", "val": "底层基于 AQS (AbstractQueuedSynchronizer) 实现"}], "analysis": "ReentrantLock 是可重入锁；选项 C 错误，其余均符合其特性。"}	["Java", "并发编程"]	2026-01-21 15:18:19.978038	\N	\N
1003	JUDGE	MEDIUM	20251111	t	{"stem": "在 SDLane 模型中，使用稀疏锚框（Sparse Anchors）的主要目的是为了降低计算复杂度并提高检测效率。", "correct": "T", "options": [{"key": "T", "val": "正确"}, {"key": "F", "val": "错误"}], "analysis": "SDLane 通过稀疏锚框和动态融合机制，在保证鲁棒性的同时显著提升了推理速度。"}	["CV", "车道线检测", "SDLane"]	2026-01-21 15:18:19.978038	2026-01-21 15:30:00	20251111
1004	SHORT_ANSWER	MEDIUM	20260119	t	{"stem": "请简述对单链表进行归并排序（Merge Sort）的基本步骤及平均时间复杂度。", "correct": "步骤：1. 快慢指针找中点拆分；2. 递归排序左右子链表；3. 合并有序链表。平均时间复杂度为 O(n log n)。", "options": [], "analysis": "考查对链表操作及归并算法分治思想的理解。"}	["算法", "链表", "排序"]	2026-01-21 15:18:19.978038	\N	\N
1005	SINGLE	EASY	20260121	t	{"stem": "在 PostgreSQL 中，若要对 jsonb 类型的字段（如 tags 数组）进行高效的包含（@>）查询，推荐使用哪种索引类型？", "correct": "C", "options": [{"key": "A", "val": "B-Tree"}, {"key": "B", "val": "Hash"}, {"key": "C", "val": "GIN"}, {"key": "D", "val": "GiST"}], "analysis": "GIN (Generalized Inverted Index) 索引专门用于处理多值类型（数组、JSONB），支持高效的成员包含查询。"}	["PostgreSQL", "数据库"]	2026-01-21 15:18:19.978038	\N	\N
\.


--
-- Data for Name: sys_oauth2_jwk; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_oauth2_jwk (id, jwk_json, create_time) FROM stdin;
oauth2-rsa-key	{"p":"18jQuq304GIagIeY68SRca9Nvpb9vQqvQLON97lFV6v_pbKa_iGYYRpsH6IZ0E8T6j9syQXnS5EWvpODeQqBxrjJM_IpfzR02woOSIGsQnWEAm77yCdu82vJWpkF_SxrYqxaqlT34wsrslJ6oFxGVR4usjVHcVlIQt4NkP6qiIE","kty":"RSA","q":"wEdbDLdNGYlZDHnzCY3DAUlqHChAMhjfGXU_jyzYEH1pr3MfbSJGLh02bZlc3NFr1cwXxwGUFpPt_Nh5EY-IP1IKv_efG5HfjcdZsApAgrwsOXBTTjHyhipPPe7Tb7DpFFxYybfcNiWmU15wCHwgb0iIOHxbORth-FamkQTVkQU","d":"AaVSHfkWdP_Jnt4eD7EG5mRc9zjPy9PSQ792qJLM3ZK2n-g17g9UKZi9znb737QIJzq5bzdTaTKl8O5fQy-j2tU-J5AoVe6YPry4aYkQTb2q17Hy1dWH3VMQBvSrKER-GgrtyfHQnNas9DC11bnw6UMmPkIyFjkXYFA76BvH3zVo7HF10V2FyRhMPdj9vwPA7UivVRaMQNTrOHK8UBYKsQmpNvX63POyaRVb46tX5zefydg7qx7xgIYKFXbHhHX8D92fPu-QcEshl0JTeD0G4-Sr9AWUmOpdW0h7uPtc6h8ZDVpEasTHYbmr_PPjEpBg93IMx5PbpSDN-irfscsjAQ","e":"AQAB","kid":"36cb1fba-44dd-415d-b8c0-6d37d1cfa53e","qi":"uZOzsDOgsWM69no4ns2WrNqio5bNTtsN-7BZN3GIV9Gjrgn07xhqK1A_4ZBgFC7rkZtymTVuLDWWOZDIXa7W3y3wpD_uSdvC5d1RJOt3flxpzzl6KLK3k_R-cyqEpSxGQBnzbvFVPWLSlTwJe2mRpQ7IvZj6rxIicqmOgnN2VS8","dp":"jHa_UNXOEoYp5ELgS5EgcFWCES-uqnEbFc4L-xbSSbi_6He0JvttesA9Y_YcQZpIR3LT-KNsJaejm5jgu1kYk2hUKQlY8-IMs0z_cTlgDb77mK3wHiF1OuWOI_h6ioxwtsGlHz3CQzxRTHoeMub-LYhBM2Y1RFavEEAFqr3r-oE","dq":"ksblrjNO26uJGokNCcH5lkXghlYDizqJM_nY5waoeQ4DWzdaSgjb4d_S6J8l0DrdQntluzhtPz71cQoegVU8AHo0Uo5IGtej-6qtwEBTNnlGmSIxtYNTSy-JBhIFvUEi67QC4xlDNOJQsVtp0lBOwiRwOA5V5KYOh3A9_n3eObE","n":"ohLB_QBbleyevMYUUG4-em1iglGOxGu-cJrLgZ52PQBEteMWOQf4qZqZ8tcpnd8_YRU4VOupQ-rlfPJrJ2Q5HRSTQVJ5NN2jzl0l_RsOvBgS_Qjb8w_ZF6Ee-Sa0AKJ5YMHPe4Uecr5VWIm5AOAgAY_esLvadoxAZWdekau_wWmy7wtf2fHUdZTDG8tF6bLdJ-I2mLqB2Gsst3_sYk7xtoegZdnk_OuV3VclMGXHTtzX4fs9RLAhgb2b4UJaOa3nNftsuQykuLNhFROfhP_cb6UE8D9cjOmPwwqOtUYOOp4yWcwtF3t9NRKJfVKH79kG4dD4A5-skOtIFOkqJ_q7hQ"}	2026-01-21 15:04:22.720202
\.


--
-- Data for Name: sys_user; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_user (id, username, password, nick_name, role, status) FROM stdin;
111	admin	$2a$10$LgiLAR.YuZTYL0QucSssgeol1/epzF60VIRZUyz1p2G.UjtxTAjBy	admin	admin	f
\.


--
-- Data for Name: user_apply_exam_relation; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_apply_exam_relation (id, exam_id, user_id, apply_time) FROM stdin;
\.


--
-- Data for Name: user_online_exam_answer; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_online_exam_answer (id, user_id, exam_id, question_id, answer, score, is_correct, option_time) FROM stdin;
\.


--
-- Data for Name: user_online_exam_options; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_online_exam_options (id, user_id, exam_id, option_type, option_time) FROM stdin;
\.


--
-- Name: exam exam_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exam
    ADD CONSTRAINT exam_pkey PRIMARY KEY (id);


--
-- Name: exam_question_relation exam_question_relation_exam_id_question_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exam_question_relation
    ADD CONSTRAINT exam_question_relation_exam_id_question_id_key UNIQUE (exam_id, question_id);


--
-- Name: exam_question_relation exam_question_relation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exam_question_relation
    ADD CONSTRAINT exam_question_relation_pkey PRIMARY KEY (id);


--
-- Name: question question_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.question
    ADD CONSTRAINT question_pkey PRIMARY KEY (id);


--
-- Name: sys_oauth2_jwk sys_oauth2_jwk_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_oauth2_jwk
    ADD CONSTRAINT sys_oauth2_jwk_pkey PRIMARY KEY (id);


--
-- Name: sys_user sys_user_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_user
    ADD CONSTRAINT sys_user_pkey PRIMARY KEY (id);


--
-- Name: sys_user sys_user_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_user
    ADD CONSTRAINT sys_user_username_key UNIQUE (username);


--
-- Name: user_apply_exam_relation user_apply_exam_relation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_apply_exam_relation
    ADD CONSTRAINT user_apply_exam_relation_pkey PRIMARY KEY (id);


--
-- Name: user_apply_exam_relation user_apply_exam_relation_user_id_exam_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_apply_exam_relation
    ADD CONSTRAINT user_apply_exam_relation_user_id_exam_id_key UNIQUE (user_id, exam_id);


--
-- Name: user_online_exam_answer user_online_exam_answer_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_online_exam_answer
    ADD CONSTRAINT user_online_exam_answer_pkey PRIMARY KEY (id);


--
-- Name: user_online_exam_answer user_online_exam_answer_user_id_exam_id_question_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_online_exam_answer
    ADD CONSTRAINT user_online_exam_answer_user_id_exam_id_question_id_key UNIQUE (user_id, exam_id, question_id);


--
-- Name: user_online_exam_options user_online_exam_options_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_online_exam_options
    ADD CONSTRAINT user_online_exam_options_pkey PRIMARY KEY (id);


--
-- Name: idx_apply_exam_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_apply_exam_id ON public.user_apply_exam_relation USING btree (exam_id);


--
-- Name: idx_exam_begin_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_exam_begin_time ON public.exam USING btree (begin_time);


--
-- Name: idx_exam_creator; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_exam_creator ON public.exam USING btree (creator);


--
-- Name: idx_exam_option_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_exam_option_type ON public.user_online_exam_options USING btree (exam_id, option_type);


--
-- Name: idx_exam_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_exam_status ON public.exam USING btree (status);


--
-- Name: idx_question_tags; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_question_tags ON public.question USING gin (tags);


--
-- Name: idx_relation_exam_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_relation_exam_id ON public.exam_question_relation USING btree (exam_id);


--
-- Name: idx_user_exam_option_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_exam_option_time ON public.user_online_exam_options USING btree (user_id, exam_id, option_time);


--
-- PostgreSQL database dump complete
--