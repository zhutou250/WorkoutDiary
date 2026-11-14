-- 健身计划与训练记录系统：建库建表脚本
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS fitness_db
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE fitness_db;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS admin_audit_log;
DROP TABLE IF EXISTS ai_advice_feedback;
DROP TABLE IF EXISTS ai_advice_item;
DROP TABLE IF EXISTS ai_advice;
DROP TABLE IF EXISTS workout_set;
DROP TABLE IF EXISTS session_exercise;
DROP TABLE IF EXISTS workout_session;
DROP TABLE IF EXISTS plan_exercise;
DROP TABLE IF EXISTS plan_day;
DROP TABLE IF EXISTS workout_plan;
DROP TABLE IF EXISTS exercise_muscle;
DROP TABLE IF EXISTS exercise;
DROP TABLE IF EXISTS equipment;
DROP TABLE IF EXISTS muscle_group;
DROP TABLE IF EXISTS body_metric;
DROP TABLE IF EXISTS user_profile;
DROP TABLE IF EXISTS sys_user;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 1. 用户与身体数据
-- ============================================================

CREATE TABLE sys_user (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  username      VARCHAR(50)     NOT NULL COMMENT '登录名',
  password_hash VARCHAR(100)    NOT NULL COMMENT 'BCrypt 密码哈希',
  nickname      VARCHAR(50)     NOT NULL COMMENT '显示名称',
  role          VARCHAR(20)     NOT NULL DEFAULT 'USER' COMMENT 'USER / ADMIN',
  status        VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / DISABLED',
  last_login_at DATETIME        NULL COMMENT '最近登录时间',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  KEY idx_role_status (role, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户与角色';

CREATE TABLE user_profile (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id        BIGINT UNSIGNED NOT NULL COMMENT '关联 sys_user.id',
  gender         VARCHAR(10)     NULL COMMENT 'MALE / FEMALE / OTHER',
  birth_date     DATE            NULL,
  height_cm      DECIMAL(5,1)    NULL,
  goal           VARCHAR(30)     NULL COMMENT 'HYPERTROPHY / STRENGTH / FAT_LOSS / GENERAL',
  training_years DECIMAL(4,1)    NULL,
  weekly_target  TINYINT         NOT NULL DEFAULT 3 COMMENT '每周目标训练次数',
  preferred_unit VARCHAR(5)      NOT NULL DEFAULT 'KG' COMMENT 'KG / LB',
  created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_profile_user (user_id),
  CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资料与训练目标';

CREATE TABLE body_metric (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id      BIGINT UNSIGNED NOT NULL,
  record_date  DATE            NOT NULL,
  weight_kg    DECIMAL(5,2)    NULL,
  body_fat_pct DECIMAL(4,1)    NULL,
  waist_cm     DECIMAL(5,1)    NULL,
  note         VARCHAR(255)    NULL,
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_date (user_id, record_date),
  KEY idx_user_date (user_id, record_date),
  CONSTRAINT fk_metric_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
  CONSTRAINT ck_body_fat CHECK (body_fat_pct IS NULL OR body_fat_pct BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体重、体脂、围度记录';

-- ============================================================
-- 2. 动作库
-- ============================================================

CREATE TABLE muscle_group (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code        VARCHAR(30)     NOT NULL,
  name        VARCHAR(30)     NOT NULL,
  body_region VARCHAR(20)     NOT NULL COMMENT 'UPPER / LOWER / CORE',
  sort_no     INT             NOT NULL DEFAULT 0,
  status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / ARCHIVED',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_muscle_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='肌肉部位字典';

CREATE TABLE equipment (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code       VARCHAR(30)     NOT NULL,
  name       VARCHAR(30)     NOT NULL,
  status     VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_equipment_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='器械字典';

CREATE TABLE exercise (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name         VARCHAR(100)    NOT NULL,
  body_part    VARCHAR(30)     NOT NULL COMMENT 'CHEST / BACK / LEGS / SHOULDERS / ARMS / CORE / GLUTES / CARDIO',
  equipment_id BIGINT UNSIGNED NULL,
  category     VARCHAR(20)     NOT NULL COMMENT 'STRENGTH / CARDIO / MOBILITY',
  mode         VARCHAR(10)     NOT NULL COMMENT 'REPS / TIME / CARDIO',
  description  TEXT            NULL,
  instructions JSON            NULL COMMENT '动作步骤数组',
  media_url    VARCHAR(255)    NULL,
  is_custom    TINYINT(1)      NOT NULL DEFAULT 0,
  created_by   BIGINT UNSIGNED NULL,
  status       VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / ARCHIVED',
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_name (name),
  KEY idx_body_part (body_part),
  KEY idx_equipment (equipment_id),
  KEY idx_mode (mode),
  KEY idx_body_equipment_mode (body_part, equipment_id, mode),
  CONSTRAINT fk_exercise_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id) ON DELETE SET NULL,
  CONSTRAINT fk_exercise_creator FOREIGN KEY (created_by) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动作库';

CREATE TABLE exercise_muscle (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  exercise_id     BIGINT UNSIGNED NOT NULL,
  muscle_group_id BIGINT UNSIGNED NOT NULL,
  role            VARCHAR(10)     NOT NULL COMMENT 'PRIMARY / SECONDARY',
  weight          DECIMAL(3,2)    NOT NULL DEFAULT 1.00 COMMENT '统计权重',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_exercise_muscle (exercise_id, muscle_group_id),
  KEY idx_muscle (muscle_group_id),
  CONSTRAINT fk_em_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id) ON DELETE CASCADE,
  CONSTRAINT fk_em_muscle FOREIGN KEY (muscle_group_id) REFERENCES muscle_group (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动作与肌肉部位权重';

-- ============================================================
-- 3. 训练计划
-- ============================================================

CREATE TABLE workout_plan (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id            BIGINT UNSIGNED NULL COMMENT '系统模板为空',
  name               VARCHAR(100)    NOT NULL,
  goal               VARCHAR(30)     NULL,
  start_date         DATE            NULL,
  weeks              INT             NOT NULL DEFAULT 4,
  status             VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / ACTIVE / ARCHIVED',
  is_template        TINYINT(1)      NOT NULL DEFAULT 0,
  source_template_id BIGINT UNSIGNED NULL,
  created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_status (user_id, status),
  KEY idx_template (is_template),
  CONSTRAINT fk_plan_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
  CONSTRAINT fk_plan_template FOREIGN KEY (source_template_id) REFERENCES workout_plan (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练计划';

CREATE TABLE plan_day (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  plan_id     BIGINT UNSIGNED NOT NULL,
  week_no     INT             NOT NULL COMMENT '第几周，从 1 开始',
  day_of_week TINYINT         NOT NULL COMMENT '1=周一，7=周日',
  title       VARCHAR(50)     NOT NULL,
  sort_no     INT             NOT NULL DEFAULT 0,
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_plan_week_day (plan_id, week_no, day_of_week),
  KEY idx_plan_week (plan_id, week_no),
  CONSTRAINT fk_day_plan FOREIGN KEY (plan_id) REFERENCES workout_plan (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划中的训练日';

CREATE TABLE plan_exercise (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  plan_day_id         BIGINT UNSIGNED NOT NULL,
  exercise_id         BIGINT UNSIGNED NOT NULL,
  sort_no             INT             NOT NULL,
  target_sets         INT             NOT NULL,
  target_reps_min     INT             NULL,
  target_reps_max     INT             NULL,
  target_weight_kg    DECIMAL(6,2)    NULL,
  target_duration_sec INT             NULL,
  rest_seconds        INT             NOT NULL DEFAULT 90,
  progression_type    VARCHAR(20)     NOT NULL DEFAULT 'NONE' COMMENT 'NONE / LINEAR / DOUBLE',
  increment_kg        DECIMAL(4,2)    NOT NULL DEFAULT 2.50,
  increment_reps      TINYINT         NOT NULL DEFAULT 1,
  note                VARCHAR(255)    NULL,
  created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_plan_day_sort (plan_day_id, sort_no),
  KEY idx_plan_exercise (exercise_id),
  CONSTRAINT fk_pe_day FOREIGN KEY (plan_day_id) REFERENCES plan_day (id) ON DELETE CASCADE,
  CONSTRAINT fk_pe_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id) ON DELETE RESTRICT,
  CONSTRAINT ck_pe_sets CHECK (target_sets > 0),
  CONSTRAINT ck_pe_reps CHECK (target_reps_min IS NULL OR target_reps_max IS NULL OR target_reps_min <= target_reps_max)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划日中的动作安排';

-- ============================================================
-- 4. 训练记录
-- ============================================================

CREATE TABLE workout_session (
  id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id                 BIGINT UNSIGNED NOT NULL,
  plan_day_id             BIGINT UNSIGNED NULL COMMENT '自由训练为空',
  plan_name_snapshot      VARCHAR(100)    NULL,
  plan_day_title_snapshot VARCHAR(50)     NULL,
  session_date            DATE            NOT NULL,
  started_at              DATETIME        NOT NULL,
  ended_at                DATETIME        NULL,
  status                  VARCHAR(20)     NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS / COMPLETED / CANCELLED',
  overall_rpe             DECIMAL(3,1)    NULL,
  feeling                 VARCHAR(20)     NULL COMMENT 'GREAT / OK / TIRED / PAIN',
  note                    VARCHAR(500)    NULL,
  created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_session_user_date (user_id, session_date),
  KEY idx_plan_day (plan_day_id),
  CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
  CONSTRAINT fk_session_plan_day FOREIGN KEY (plan_day_id) REFERENCES plan_day (id) ON DELETE SET NULL,
  CONSTRAINT ck_session_rpe CHECK (overall_rpe IS NULL OR overall_rpe BETWEEN 1 AND 10)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一次实际训练';

CREATE TABLE session_exercise (
  id                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  session_id             BIGINT UNSIGNED NOT NULL,
  exercise_id            BIGINT UNSIGNED NOT NULL,
  plan_exercise_id       BIGINT UNSIGNED NULL,
  exercise_name_snapshot VARCHAR(100)    NOT NULL,
  sort_no                INT             NOT NULL,
  planned_sets           INT             NULL,
  planned_reps_min       INT             NULL,
  planned_reps_max       INT             NULL,
  planned_weight_kg      DECIMAL(6,2)    NULL,
  planned_duration_sec   INT             NULL,
  deviation_type         VARCHAR(20)     NOT NULL DEFAULT 'NONE' COMMENT 'NONE / SWAPPED / ADDED / SKIPPED',
  actual_note            VARCHAR(255)    NULL,
  created_at             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_session (session_id),
  KEY idx_exercise (exercise_id),
  CONSTRAINT fk_se_session FOREIGN KEY (session_id) REFERENCES workout_session (id) ON DELETE CASCADE,
  CONSTRAINT fk_se_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id) ON DELETE RESTRICT,
  CONSTRAINT fk_se_plan_exercise FOREIGN KEY (plan_exercise_id) REFERENCES plan_exercise (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练中的动作及计划快照';

CREATE TABLE workout_set (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  session_exercise_id BIGINT UNSIGNED NOT NULL,
  set_no              INT             NOT NULL COMMENT '第几组，从 1 开始',
  phase               VARCHAR(10)     NOT NULL DEFAULT 'WORK' COMMENT 'WARMUP / WORK',
  set_type            VARCHAR(15)     NOT NULL DEFAULT 'STRAIGHT' COMMENT 'STRAIGHT / DROP / REST_PAUSE',
  side                VARCHAR(5)      NOT NULL DEFAULT 'BOTH' COMMENT 'BOTH / L / R',
  weight_kg           DECIMAL(6,2)    NULL,
  reps                INT             NULL,
  duration_sec        INT             NULL,
  distance_m          DECIMAL(8,2)    NULL,
  rir                 TINYINT         NULL,
  rpe                 DECIMAL(3,1)    NULL,
  is_failure          TINYINT(1)      NOT NULL DEFAULT 0,
  is_completed        TINYINT(1)      NOT NULL DEFAULT 1,
  completed_at        DATETIME        NULL,
  detail_json         JSON            NULL,
  note                VARCHAR(255)    NULL,
  created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_session_exercise_set (session_exercise_id, set_no, side),
  KEY idx_session_exercise (session_exercise_id),
  KEY idx_completed_at (completed_at),
  CONSTRAINT fk_set_session_exercise FOREIGN KEY (session_exercise_id) REFERENCES session_exercise (id) ON DELETE CASCADE,
  CONSTRAINT ck_set_weight CHECK (weight_kg IS NULL OR weight_kg >= 0),
  CONSTRAINT ck_set_reps CHECK (reps IS NULL OR reps >= 0),
  CONSTRAINT ck_set_duration CHECK (duration_sec IS NULL OR duration_sec >= 0),
  CONSTRAINT ck_set_rir CHECK (rir IS NULL OR rir BETWEEN 0 AND 10),
  CONSTRAINT ck_set_rpe CHECK (rpe IS NULL OR rpe BETWEEN 1 AND 10)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每一组的实际数据';

-- ============================================================
-- 5. AI 建议
-- ============================================================

CREATE TABLE ai_advice (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id        BIGINT UNSIGNED NOT NULL,
  period_start   DATE            NOT NULL,
  period_end     DATE            NOT NULL,
  status         VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / SUCCESS / FALLBACK / FAILED',
  model_name     VARCHAR(50)     NULL,
  prompt_version VARCHAR(20)     NULL,
  request_digest CHAR(64)        NULL,
  response_text  TEXT            NULL,
  fallback_used  TINYINT(1)      NOT NULL DEFAULT 0,
  error_message  VARCHAR(500)    NULL,
  created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_created (user_id, created_at),
  CONSTRAINT fk_advice_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一次建议生成记录';

CREATE TABLE ai_advice_item (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  advice_id       BIGINT UNSIGNED NOT NULL,
  finding_key     VARCHAR(64)     NOT NULL,
  rule_code       VARCHAR(50)     NOT NULL,
  severity        VARCHAR(10)     NOT NULL COMMENT 'INFO / WARN / ALERT',
  title           VARCHAR(100)    NOT NULL,
  evidence_json   JSON            NOT NULL,
  suggestion_text TEXT            NOT NULL,
  source          VARCHAR(10)     NOT NULL DEFAULT 'RULE' COMMENT 'RULE / AI',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_advice_finding (advice_id, finding_key),
  KEY idx_advice (advice_id),
  KEY idx_rule (rule_code),
  CONSTRAINT fk_item_advice FOREIGN KEY (advice_id) REFERENCES ai_advice (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单条规则发现与建议';

CREATE TABLE ai_advice_feedback (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  advice_id  BIGINT UNSIGNED NOT NULL,
  user_id    BIGINT UNSIGNED NOT NULL,
  rating     TINYINT         NULL,
  helpful    TINYINT(1)      NULL,
  comment    VARCHAR(255)    NULL,
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_advice_user (advice_id, user_id),
  CONSTRAINT fk_feedback_advice FOREIGN KEY (advice_id) REFERENCES ai_advice (id) ON DELETE CASCADE,
  CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
  CONSTRAINT ck_feedback_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户对建议的评价';

CREATE TABLE admin_audit_log (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  admin_user_id BIGINT UNSIGNED NOT NULL,
  action        VARCHAR(50)     NOT NULL,
  target_type   VARCHAR(30)     NULL,
  target_id     BIGINT UNSIGNED NULL,
  detail_json   JSON            NULL,
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_admin_created (admin_user_id, created_at),
  CONSTRAINT fk_audit_admin FOREIGN KEY (admin_user_id) REFERENCES sys_user (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作日志';
