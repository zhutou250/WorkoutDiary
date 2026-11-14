# 健身计划与训练记录系统
## 数据库字段清单、统计公式与 AI 建议规则表

版本：V1.0  
用途：数据库设计、后端实现、论文第 4—6 章、答辩演示  
技术前提：Spring Boot 3 + MyBatis-Plus/JPA + MySQL 8 + Vue 3

---

## 0. 阅读说明

本文档把系统拆成三部分：

1. **数据库字段清单**：先保证“计划、训练、每一组”能被正确保存。
2. **统计公式**：明确每个数字怎么算，避免前端和后端各算一套。
3. **AI 建议规则表**：先由后端规则引擎判断，再让大模型只负责把结论写成自然语言。

### 0.1 优先级

- **P0**：中期检查前必须完成，支撑“计划—训练—记录—统计”闭环。
- **P1**：论文初稿前完成，作为智能建议和系统完整性的加分项。
- **P2**：不建议在本课题实现，例如社交、动作视频、移动端、数据导入。

### 0.2 公共字段约定

除特别说明外，每张业务表默认包含：

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键 |
| `created_at` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

是否需要 `updated_at` 按表是否会被修改决定；本文默认所有业务表都保留，只有纯追加表可以省略。

### 0.3 命名与类型约定

- 表名、字段名统一使用 `snake_case`。
- 重量统一存千克，字段名带 `_kg`；长度统一存厘米，字段名带 `_cm`；时间统一存秒，字段名带 `_sec`。
- 重量使用 `DECIMAL(6,2)`，次数使用 `INT`，RIR 使用 `TINYINT`，RPE 使用 `DECIMAL(3,1)`。
- 布尔值使用 `TINYINT(1)`，`0` 表示否，`1` 表示是。
- 状态、角色、类型使用 `VARCHAR(20)` 加应用层枚举，便于后续扩展，不使用 MySQL `ENUM` 锁死取值。
- 训练日期使用 `DATE`，具体时间使用 `DATETIME`；系统时区统一为 `Asia/Shanghai`，周起始日统一为周一。

---

## 1. 数据模型总览

### 1.1 核心链路

```text
sys_user
  └─ user_profile
  └─ body_metric
  └─ workout_plan
       └─ plan_day
            └─ plan_exercise

sys_user
  └─ workout_session
       └─ session_exercise
            └─ workout_set

exercise
  └─ exercise_muscle
       └─ muscle_group

workout_session
  └─ ai_advice
       └─ ai_advice_item
```

核心思想：

- **计划是模板**：`workout_plan → plan_day → plan_exercise`。
- **训练是事实**：`workout_session → session_exercise → workout_set`。
- **历史不能变**：训练开始时把计划中的动作名、计划组数、计划次数、计划重量复制到 `session_exercise` 的快照字段。
- **统计按组计算**：热身组、未完成组不进入训练量和部位统计。

### 1.2 表清单

| 表名 | 说明 | 优先级 |
|---|---|---|
| `sys_user` | 用户与角色 | P0 |
| `user_profile` | 用户资料与训练目标 | P0 |
| `body_metric` | 体重、体脂、围度记录 | P0 |
| `muscle_group` | 肌肉部位字典 | P0 |
| `equipment` | 器械字典 | P0 |
| `exercise` | 动作库 | P0 |
| `exercise_muscle` | 动作与肌肉部位的权重关系 | P0 |
| `workout_plan` | 训练计划 | P0 |
| `plan_day` | 计划中的训练日 | P0 |
| `plan_exercise` | 计划日中的动作安排 | P0 |
| `workout_session` | 一次实际训练 | P0 |
| `session_exercise` | 一次训练中的动作及计划快照 | P0 |
| `workout_set` | 每一组的实际数据 | P0 |
| `ai_advice` | 一次建议生成记录 | P1 |
| `ai_advice_item` | 单条规则发现与建议 | P1 |
| `ai_advice_feedback` | 用户对建议的评价 | P1 |
| `admin_audit_log` | 管理员操作日志 | P1 |

> AI 相关表属于 P1。V1 先实现规则引擎和模板文案，外部模型接口后续接入，不阻塞中期检查。

---

## 2. 数据库字段清单

### 2.1 用户与身体数据

#### 表：`sys_user`

| 字段 | 类型 | 约束/默认值 | 说明 |
|---|---|---|---|
| `username` | VARCHAR(50) | NOT NULL, UNIQUE | 登录名 |
| `password_hash` | VARCHAR(100) | NOT NULL | BCrypt 密码哈希 |
| `nickname` | VARCHAR(50) | NOT NULL | 显示名称 |
| `role` | VARCHAR(20) | NOT NULL, DEFAULT 'USER' | `USER` / `ADMIN` |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | `ACTIVE` / `DISABLED` |
| `last_login_at` | DATETIME | NULL | 最近登录时间 |

索引：`uk_username(username)`、`idx_role_status(role, status)`。

#### 表：`user_profile`

| 字段 | 类型 | 约束/默认值 | 说明 |
|---|---|---|---|
| `user_id` | BIGINT UNSIGNED | NOT NULL, UNIQUE, FK | 关联 `sys_user.id` |
| `gender` | VARCHAR(10) | NULL | `MALE` / `FEMALE` / `OTHER` |
| `birth_date` | DATE | NULL | 出生日期 |
| `height_cm` | DECIMAL(5,1) | NULL | 身高 |
| `goal` | VARCHAR(30) | NULL | `HYPERTROPHY` / `STRENGTH` / `FAT_LOSS` / `GENERAL` |
| `training_years` | DECIMAL(4,1) | NULL | 训练年限 |
| `weekly_target` | TINYINT | DEFAULT 3 | 每周目标训练次数 |
| `preferred_unit` | VARCHAR(5) | DEFAULT 'KG' | `KG` / `LB` |

#### 表：`body_metric`

| 字段 | 类型 | 约束/默认值 | 说明 |
|---|---|---|---|
| `user_id` | BIGINT UNSIGNED | NOT NULL, FK | 关联用户 |
| `record_date` | DATE | NOT NULL | 记录日期 |
| `weight_kg` | DECIMAL(5,2) | NULL | 体重 |
| `body_fat_pct` | DECIMAL(4,1) | NULL | 体脂率，0—100 |
| `waist_cm` | DECIMAL(5,1) | NULL | 腰围 |
| `note` | VARCHAR(255) | NULL | 备注 |

唯一约束：`uk_user_date(user_id, record_date)`。  
索引：`idx_user_date(user_id, record_date)`。

### 2.2 动作库

#### 表：`muscle_group`

| 字段 | 类型 | 约束/默认值 | 说明 |
|---|---|---|---|
| `code` | VARCHAR(30) | NOT NULL, UNIQUE | 肌肉编码，如 `CHEST`、`QUADS` |
| `name` | VARCHAR(30) | NOT NULL | 中文名称 |
| `body_region` | VARCHAR(20) | NOT NULL | `UPPER` / `LOWER` / `CORE` |
| `sort_no` | INT | DEFAULT 0 | 展示顺序 |
| `status` | VARCHAR(20) | DEFAULT 'ACTIVE' | `ACTIVE` / `ARCHIVED` |

建议初始化 18 个部位：胸、上背、下背、肩、肱二头、肱三头、前臂、腹直肌、腹斜肌、臀、股四头、腘绳肌、内收肌、髋屈肌、小腿、胫骨前肌、斜方肌、前锯肌。

#### 表：`equipment`

| 字段 | 类型 | 约束/默认值 | 说明 |
|---|---|---|---|
| `code` | VARCHAR(30) | NOT NULL, UNIQUE | 器械编码 |
| `name` | VARCHAR(30) | NOT NULL | 器械名称 |
| `status` | VARCHAR(20) | DEFAULT 'ACTIVE' | 状态 |

#### 表：`exercise`

| 字段 | 类型 | 约束/默认值 | 说明 |
|---|---|---|---|
| `name` | VARCHAR(100) | NOT NULL | 动作名称 |
| `body_part` | VARCHAR(30) | NOT NULL | 展示用身体部位，如 `CHEST`、`BACK`、`LEGS` |
| `equipment_id` | BIGINT UNSIGNED | NULL, FK | 关联 `equipment.id` |
| `category` | VARCHAR(20) | NOT NULL | `STRENGTH` / `CARDIO` / `MOBILITY` |
| `mode` | VARCHAR(10) | NOT NULL | `REPS` / `TIME` / `CARDIO` |
| `description` | TEXT | NULL | 动作简介 |
| `instructions` | JSON | NULL | 动作步骤数组 |
| `media_url` | VARCHAR(255) | NULL | 图片或视频地址 |
| `is_custom` | TINYINT(1) | DEFAULT 0 | 是否为用户自建动作 |
| `created_by` | BIGINT UNSIGNED | NULL, FK | 创建者；系统动作为空 |
| `status` | VARCHAR(20) | DEFAULT 'ACTIVE' | `ACTIVE` / `ARCHIVED` |

索引：`idx_name(name)`、`idx_body_part(body_part)`、`idx_equipment(equipment_id)`、`idx_mode(mode)`。

#### 表：`exercise_muscle`

| 字段 | 类型 | 约束/默认值 | 说明 |
|---|---|---|---|
| `exercise_id` | BIGINT UNSIGNED | NOT NULL, FK | 关联动作 |
| `muscle_group_id` | BIGINT UNSIGNED | NOT NULL, FK | 关联肌肉部位 |
| `role` | VARCHAR(10) | NOT NULL | `PRIMARY` / `SECONDARY` |
| `weight` | DECIMAL(3,2) | NOT NULL, DEFAULT 1.00 | 统计权重，主动肌 1.00，协同肌 0.40 |

唯一约束：`uk_exercise_muscle(exercise_id, muscle_group_id)`。  
索引：`idx_muscle(muscle_group_id)`。

> 一个动作可以有多个主动肌和多个协同肌。统计时使用 `weight`，不要只用 `role` 做字符串判断，这样以后可以把协同肌权重从 0.40 调整为 0.30 或 0.50。

### 2.3 训练计划

#### 表：`workout_plan`

| 字段 | 类型 | 约束/默认值 | 说明 |
|---|---|---|---|
| `user_id` | BIGINT UNSIGNED | NULL, FK | 用户计划；系统模板可为空 |
| `name` | VARCHAR(100) | NOT NULL | 计划名称 |
| `goal` | VARCHAR(30) | NULL | 计划目标 |
| `start_date` | DATE | NULL | 开始日期 |
| `weeks` | INT | DEFAULT 4 | 计划周数 |
| `status` | VARCHAR(20) | DEFAULT 'DRAFT' | `DRAFT` / `ACTIVE` / `ARCHIVED` |
| `is_template` | TINYINT(1) | DEFAULT 0 | 是否为系统模板 |
| `source_template_id` | BIGINT UNSIGNED | NULL, FK | 从哪个模板复制而来 |

索引：`idx_user_status(user_id, status)`、`idx_template(is_template)`。

#### 表：`plan_day`

| 字段 | 类型 | 约束/默认值 | 说明 |
|---|---|---|---|
| `plan_id` | BIGINT UNSIGNED | NOT NULL, FK | 关联计划 |
| `week_no` | INT | NOT NULL | 第几周，从 1 开始 |
| `day_of_week` | TINYINT | NOT NULL | 1=周一，7=周日 |
| `title` | VARCHAR(50) | NOT NULL | 如“上肢推”“下肢” |
| `sort_no` | INT | DEFAULT 0 | 展示顺序 |

唯一约束：`uk_plan_week_day(plan_id, week_no, day_of_week)`。  
索引：`idx_plan_week(plan_id, week_no)`。

#### 表：`plan_exercise`

| 字段 | 类型 | 约束/默认值 | 说明 |
|---|---|---|---|
| `plan_day_id` | BIGINT UNSIGNED | NOT NULL, FK | 关联计划日 |
| `exercise_id` | BIGINT UNSIGNED | NOT NULL, FK | 关联动作 |
| `sort_no` | INT | NOT NULL | 动作顺序 |
| `target_sets` | INT | NOT NULL | 计划组数 |
| `target_reps_min` | INT | NULL | 计划最少次数 |
| `target_reps_max` | INT | NULL | 计划最多次数 |
| `target_weight_kg` | DECIMAL(6,2) | NULL | 计划重量 |
| `target_duration_sec` | INT | NULL | 计时动作的目标秒数 |
| `rest_seconds` | INT | DEFAULT 90 | 组间休息 |
| `progression_type` | VARCHAR(20) | DEFAULT 'NONE' | `NONE` / `LINEAR` / `DOUBLE` |
| `increment_kg` | DECIMAL(4,2) | DEFAULT 2.50 | 渐进时每次加的重量 |
| `increment_reps` | TINYINT | DEFAULT 1 | 自重/计时动作每次加的次数或秒数 |
| `note` | VARCHAR(255) | NULL | 动作备注 |

检查条件：`target_sets > 0`；`target_reps_min <= target_reps_max`；计时动作 `target_duration_sec > 0`。

### 2.4 训练记录

#### 表：`workout_session`

| 字段 | 类型 | 约束/默认值 | 说明 |
|---|---|---|---|
| `user_id` | BIGINT UNSIGNED | NOT NULL, FK | 训练用户 |
| `plan_day_id` | BIGINT UNSIGNED | NULL, FK | 来源计划日；自由训练为空 |
| `plan_name_snapshot` | VARCHAR(100) | NULL | 训练开始时的计划名称 |
| `plan_day_title_snapshot` | VARCHAR(50) | NULL | 训练开始时的训练日名称 |
| `session_date` | DATE | NOT NULL | 训练日期 |
| `started_at` | DATETIME | NOT NULL | 开始时间 |
| `ended_at` | DATETIME | NULL | 结束时间 |
| `status` | VARCHAR(20) | DEFAULT 'IN_PROGRESS' | `IN_PROGRESS` / `COMPLETED` / `CANCELLED` |
| `overall_rpe` | DECIMAL(3,1) | NULL | 整次训练主观强度，1—10 |
| `feeling` | VARCHAR(20) | NULL | `GREAT` / `OK` / `TIRED` / `PAIN` |
| `note` | VARCHAR(500) | NULL | 训练备注 |

索引：`idx_user_date(user_id, session_date)`、`idx_plan_day(plan_day_id)`。

#### 表：`session_exercise`

| 字段 | 类型 | 约束/默认值 | 说明 |
|---|---|---|---|
| `session_id` | BIGINT UNSIGNED | NOT NULL, FK | 关联训练 |
| `exercise_id` | BIGINT UNSIGNED | NOT NULL, FK | 实际动作 |
| `plan_exercise_id` | BIGINT UNSIGNED | NULL, FK | 来源计划动作；临时新增为空 |
| `exercise_name_snapshot` | VARCHAR(100) | NOT NULL | 训练当时的动作名称 |
| `sort_no` | INT | NOT NULL | 动作顺序 |
| `planned_sets` | INT | NULL | 计划组数快照 |
| `planned_reps_min` | INT | NULL | 计划最少次数快照 |
| `planned_reps_max` | INT | NULL | 计划最多次数快照 |
| `planned_weight_kg` | DECIMAL(6,2) | NULL | 计划重量快照 |
| `planned_duration_sec` | INT | NULL | 计划时长快照 |
| `deviation_type` | VARCHAR(20) | DEFAULT 'NONE' | `NONE` / `SWAPPED` / `ADDED` / `SKIPPED` |
| `actual_note` | VARCHAR(255) | NULL | 本次动作备注 |

索引：`idx_session(session_id)`、`idx_exercise(exercise_id)`。

> 计划中的动作被替换时，原计划动作不删除，实际动作通过 `deviation_type='SWAPPED'` 记录；计划动作完全没做时，可以生成一条 `SKIPPED` 记录，`planned_sets` 保留，`workout_set` 为空。这样“计划完成度”和“计划偏差率”都能计算。

#### 表：`workout_set`

| 字段 | 类型 | 约束/默认值 | 说明 |
|---|---|---|---|
| `session_exercise_id` | BIGINT UNSIGNED | NOT NULL, FK | 关联本次训练动作 |
| `set_no` | INT | NOT NULL | 第几组，从 1 开始 |
| `phase` | VARCHAR(10) | DEFAULT 'WORK' | `WARMUP` / `WORK` |
| `set_type` | VARCHAR(15) | DEFAULT 'STRAIGHT' | `STRAIGHT` / `DROP` / `REST_PAUSE` |
| `side` | VARCHAR(5) | DEFAULT 'BOTH' | `BOTH` / `L` / `R` |
| `weight_kg` | DECIMAL(6,2) | NULL | 实际重量 |
| `reps` | INT | NULL | 实际次数 |
| `duration_sec` | INT | NULL | 计时动作实际秒数 |
| `distance_m` | DECIMAL(8,2) | NULL | 有氧距离，可选 |
| `rir` | TINYINT | NULL | 余力次数，0—10 |
| `rpe` | DECIMAL(3,1) | NULL | 主观用力程度，1—10 |
| `is_failure` | TINYINT(1) | DEFAULT 0 | 是否力竭 |
| `is_completed` | TINYINT(1) | DEFAULT 1 | 是否完成 |
| `completed_at` | DATETIME | NULL | 完成时间 |
| `detail_json` | JSON | NULL | 递减组、休息暂停等扩展数据，P1 |
| `note` | VARCHAR(255) | NULL | 本组备注 |

唯一约束：`uk_session_exercise_set(session_exercise_id, set_no, side)`。  
索引：`idx_session_exercise(session_exercise_id)`、`idx_completed_at(completed_at)`。

检查条件：`weight_kg >= 0`；`reps >= 0`；`duration_sec >= 0`；`rir BETWEEN 0 AND 10`；`rpe BETWEEN 1 AND 10`。

### 2.5 AI 建议

#### 表：`ai_advice`

| 字段 | 类型 | 约束/默认值 | 说明 |
|---|---|---|---|
| `user_id` | BIGINT UNSIGNED | NOT NULL, FK | 建议所属用户 |
| `period_start` | DATE | NOT NULL | 分析窗口开始 |
| `period_end` | DATE | NOT NULL | 分析窗口结束 |
| `status` | VARCHAR(20) | DEFAULT 'PENDING' | `PENDING` / `SUCCESS` / `FALLBACK` / `FAILED` |
| `model_name` | VARCHAR(50) | NULL | 模型名称 |
| `prompt_version` | VARCHAR(20) | NULL | 提示词版本 |
| `request_digest` | CHAR(64) | NULL | 请求摘要，用于审计，不保存敏感原文 |
| `response_text` | TEXT | NULL | 模型返回的完整文本 |
| `fallback_used` | TINYINT(1) | DEFAULT 0 | 是否使用了规则模板兜底 |
| `error_message` | VARCHAR(500) | NULL | 失败原因 |

索引：`idx_user_created(user_id, created_at)`。

#### 表：`ai_advice_item`

| 字段 | 类型 | 约束/默认值 | 说明 |
|---|---|---|---|
| `advice_id` | BIGINT UNSIGNED | NOT NULL, FK | 关联一次建议 |
| `finding_key` | VARCHAR(64) | NOT NULL | 规则发现唯一键，如 `WEIGHT_STALL:12` |
| `rule_code` | VARCHAR(50) | NOT NULL | 规则编号 |
| `severity` | VARCHAR(10) | NOT NULL | `INFO` / `WARN` / `ALERT` |
| `title` | VARCHAR(100) | NOT NULL | 发现标题 |
| `evidence_json` | JSON | NOT NULL | 支撑该结论的数据 |
| `suggestion_text` | TEXT | NOT NULL | 最终展示给用户的建议 |
| `source` | VARCHAR(10) | DEFAULT 'RULE' | `RULE` / `AI` |

唯一约束：`uk_advice_finding(advice_id, finding_key)`。  
索引：`idx_advice(advice_id)`、`idx_rule(rule_code)`。

#### 表：`ai_advice_feedback`（P1）

| 字段 | 类型 | 约束/默认值 | 说明 |
|---|---|---|---|
| `advice_id` | BIGINT UNSIGNED | NOT NULL, FK | 关联建议 |
| `user_id` | BIGINT UNSIGNED | NOT NULL, FK | 评价用户 |
| `rating` | TINYINT | NULL | 1—5 分 |
| `helpful` | TINYINT(1) | NULL | 是否有帮助 |
| `comment` | VARCHAR(255) | NULL | 评价内容 |

唯一约束：`uk_advice_user(advice_id, user_id)`。

#### 表：`admin_audit_log`（P1）

| 字段 | 类型 | 约束/默认值 | 说明 |
|---|---|---|---|
| `admin_user_id` | BIGINT UNSIGNED | NOT NULL, FK | 管理员 |
| `action` | VARCHAR(50) | NOT NULL | 操作类型 |
| `target_type` | VARCHAR(30) | NULL | 目标类型 |
| `target_id` | BIGINT UNSIGNED | NULL | 目标 ID |
| `detail_json` | JSON | NULL | 操作详情 |

索引：`idx_admin_created(admin_user_id, created_at)`。

---

## 3. 关键约束与事务边界

### 3.1 唯一约束

| 表 | 唯一约束 | 目的 |
|---|---|---|
| `sys_user` | `username` | 登录名唯一 |
| `user_profile` | `user_id` | 一个用户一份资料 |
| `body_metric` | `(user_id, record_date)` | 一天一条身体数据 |
| `exercise_muscle` | `(exercise_id, muscle_group_id)` | 同一动作同一肌肉只出现一次 |
| `plan_day` | `(plan_id, week_no, day_of_week)` | 一周同一天只安排一个训练日 |
| `workout_set` | `(session_exercise_id, set_no, side)` | 同一动作同一组不重复 |
| `ai_advice_item` | `(advice_id, finding_key)` | 一次建议中同一发现不重复 |

### 3.2 建议索引

| 表 | 索引 | 用途 |
|---|---|---|
| `body_metric` | `(user_id, record_date)` | 体重趋势 |
| `exercise` | `(body_part, equipment_id, mode)` | 动作筛选 |
| `workout_plan` | `(user_id, status)` | 我的计划 |
| `plan_day` | `(plan_id, week_no)` | 按周加载计划 |
| `workout_session` | `(user_id, session_date)` | 训练历史和日历 |
| `session_exercise` | `(session_id)` | 训练详情 |
| `workout_set` | `(session_exercise_id)` | 组数据加载 |
| `ai_advice` | `(user_id, created_at)` | 建议历史 |

### 3.3 事务边界

1. **开始训练**：创建 `workout_session`，同时从 `plan_exercise` 复制生成多条 `session_exercise`；任一步失败则整体回滚。
2. **保存组记录**：`workout_set` 的插入或更新与 `session_exercise` 的完成状态在同一事务中完成。
3. **结束训练**：更新 `workout_session.status='COMPLETED'` 和 `ended_at`，校验至少有一组已完成；失败则回滚。
4. **生成建议**：先写入 `ai_advice` 和 `ai_advice_item`，再调用模型；模型失败时保留 `FALLBACK` 记录，不影响训练模块。

### 3.4 快照规则

- 训练开始后，`session_exercise` 中的 `exercise_name_snapshot`、`planned_sets`、`planned_reps_min`、`planned_reps_max`、`planned_weight_kg` 不再随计划修改。
- `workout_session` 保存 `plan_name_snapshot` 和 `plan_day_title_snapshot`，避免计划改名后历史记录无法辨认。
- 动作被归档或改名后，历史记录仍然通过快照显示当时的名称。
- 统计一律使用实际记录 `workout_set`，计划快照只用于计算完成度和偏差，不直接参与训练量。

---

## 4. 统计公式

### 4.1 符号说明

- \(s\)：一条 `workout_set` 记录。
- \(w_s\)：该组重量，单位 kg。
- \(r_s\)：该组次数。
- \(phase_s\)：`WARMUP` 或 `WORK`。
- \(completed_s\)：是否完成，1 或 0。
- \(E\)：动作集合。
- \(M\)：肌肉部位集合。
- \(S_e\)：动作 \(e\) 的有效组数。
- \(\alpha_{e,m}\)：动作 \(e\) 对肌肉 \(m\) 的权重，主动肌 1.00，协同肌 0.40。

### 4.2 总训练量

```text
V = Σ (w_s × r_s)
    其中 s 满足：phase_s = WORK 且 completed_s = 1
```

说明：

- 热身组不计入。
- 未完成组不计入。
- 自重动作若 `weight_kg = 0`，训练量为 0，但次数和组数仍然计入部位统计。
- 有氧动作不参与 kg 训练量，单独统计时长、距离或次数。

### 4.3 训练频率

```text
F_week = 该周内存在已完成组的不同的 session_date 数量
```

周起始日为周一。一天训练两次只计 1 次训练频率，但训练量分别累加。

### 4.4 计划完成度

单次训练：

```text
C_session = 已完成正式组数 / 计划正式组数
```

单个动作：

```text
C_exercise = 已完成正式组数 / planned_sets
```

展示时建议：

- 完成度超过 100% 时，百分比显示封顶 100%，同时单独显示“额外组数”。
- 计划动作为 `SKIPPED` 时，分母保留，分子为 0。
- 临时新增动作不计入原计划完成度，但计入实际训练量。

### 4.5 部位有效组数

```text
E_m = Σ (S_e × α_e,m)
```

其中：

- 双侧动作：一组计 1.0 组。
- 单侧动作：左侧和右侧各一条 `workout_set`，每条计 0.5 组；两侧都完成合计 1.0 组。
- 主动肌：`α = 1.00`。
- 协同肌：`α = 0.40`。
- 热身组、未完成组不进入计算。

### 4.6 部位均衡

先计算某周各肌肉的有效组数 \(E_m\)，再计算已训练肌肉的中位数 \(median(E)\)。

建议的提示规则：

```text
偏少：E_m < max(4, 0.5 × median(E))，且连续 2 周
偏多：E_m > 1.5 × median(E) 且 E_m > 20，且连续 2 周
覆盖率：E_m > 0 的肌肉数量 / 18
```

这些阈值是产品规则，不是医学标准，应该放在配置中，允许调整。论文中要明确说明“用于训练记录提示，不构成医学诊断”。

### 4.7 预计 1RM

采用 Epley 公式：

```text
estimated_1RM = w × (1 + r / 30)
```

使用条件：

- 只对 `mode = REPS` 的动作计算。
- 建议只使用 \(1 \le r \le 10\) 的正式组；次数过高时公式会明显高估。
- 单侧动作使用单侧重量和次数，不把两侧次数相加。
- 自重动作使用“体重 + 附加重量”；助力器械使用“体重 - 助力”；体重未知时不计算。
- 热身组不参与。

示例：60 kg 完成 8 次：

```text
estimated_1RM = 60 × (1 + 8 / 30) = 76.0 kg
```

### 4.8 个人纪录 PR

```text
PR_e = max(estimated_1RM_e,t)
```

只标记“第一次达到该最大值”的日期，后续追平不重复标记。若动作没有重量记录，则使用“最多次数”或“最长时长”作为该动作的 PR 指标。

### 4.9 体重趋势

```text
7 日移动平均 = 最近 7 天体重记录的平均值
周变化率 = (本周 7 日均值 - 上周 7 日均值) / 上周 7 日均值 × 100%
```

体重记录不足 7 天时，不做趋势判断。

### 4.10 统计边界

| 场景 | 处理方式 |
|---|---|
| 热身组 | 不计入训练量、部位组数、1RM、PR |
| 未完成组 | 不计入任何统计 |
| 自重动作 | 不计 kg 训练量，计次数和有效组数 |
| 助力器械 | 有效负荷 = 体重 - 助力；体重未知则跳过 1RM |
| 单侧动作 | 每侧 0.5 组；1RM 取较强一侧 |
| 重量为 0 | 训练量为 0，但仍可统计次数和组数 |
| 计划被修改 | 历史统计使用 `session_exercise` 快照 |
| 周起始日 | 周一 |
| 缺失 RIR/RPE | 保留空值，不做插值 |
| 时区 | 统一 `Asia/Shanghai`，按用户本地训练日期归档 |

### 4.11 手工验算示例

某次训练：

| 动作 | 组数 | 重量 | 次数 | 是否热身 |
|---|---:|---:|---:|---|
| 深蹲 | 1 | 20 kg | 10 | 是 |
| 深蹲 | 3 | 60 kg | 8 | 否 |
| 卧推 | 3 | 40 kg | 10 | 否 |

计算：

```text
训练量 = 3 × 60 × 8 + 3 × 40 × 10 = 1440 + 1200 = 2640 kg
深蹲 e1RM = 60 × (1 + 8 / 30) = 76.0 kg
部位有效组数：
  股四头肌 = 3 × 1.00 = 3.0
  臀肌     = 3 × 0.40 = 1.2
  腘绳肌   = 3 × 0.40 = 1.2
  胸肌     = 3 × 1.00 = 3.0
  肱三头肌 = 3 × 0.40 = 1.2
  肩部     = 3 × 0.40 = 1.2
```

如果计划就是深蹲 3 组、卧推 3 组，则计划完成度为 `6 / 6 = 100%`。

---

## 5. AI 建议规则表

### 5.1 生成流程

```text
训练数据
  → 统计服务计算指标
  → 规则引擎生成 finding
  → 后端组装 allowlist 数据
  → 大模型只改写建议文案
  → 校验 findingKey、文案长度、数字来源
  → 成功则保存 AI 文本；失败则使用规则模板
  → 前端展示，用户可选择是否采纳计划调整
```

关键原则：

- **数字由后端算**，大模型不负责计算。
- **模型不能新增 finding**，只能改写已有 finding 的建议。
- **模型不能直接修改训练日志或计划**；计划调整必须由用户确认。
- **超时、格式错误、内容不合规时**，直接展示规则模板，不影响页面。

实施阶段：

- **V1**：只实现规则引擎和 `suggestionTemplate`，不调用外部模型。
- **V1.1**：接入 OpenAI 兼容接口，模型只负责改写文案。
- **V1.2**：增加建议反馈和效果统计。

### 5.2 规则表

| rule_code | 触发条件（默认值） | 证据字段 | 建议模板 | 级别 | 分析窗口 |
|---|---|---|---|---|---|
| `DATA_INSUFFICIENT` | 已完成训练少于 3 次，或数据不足 14 天 | `sessionCount`, `daySpan` | 继续记录 2—3 次训练后再分析；当前数据不足以判断趋势。 | INFO | 14 天 |
| `WEIGHT_STALL` | 同一动作连续 3 次或 4 周，最佳 e1RM 变化 ≤ 2%，且重量未提高 | `exerciseId`, `exerciseName`, `sessions`, `bestE1rmKg`, `changePct`, `lastWeightKg` | 保持当前重量，尝试每组增加 1 次；若连续 2 次仍未完成，下周减载约 10% 后重建。 | WARN | 4 周 |
| `REP_STALL` | 自重或计时动作连续 3 次，最佳次数/时长没有提高 | `exerciseId`, `exerciseName`, `bestReps`, `bestDurationSec`, `sessions` | 先增加 1—2 次或 1 组，再考虑更换动作；不要同时增加重量和组数。 | WARN | 3 次训练 |
| `MUSCLE_UNDERTRAINED` | 某肌肉周有效组数 < max(4, 0.5×中位数)，连续 2 周 | `muscleCode`, `weeklyEffectiveSets`, `medianEffectiveSets` | 下周为该部位增加 2—4 组，优先安排复合动作，并观察恢复情况。 | WARN | 2 周 |
| `MUSCLE_OVERTRAINED` | 某肌肉周有效组数 > 1.5×中位数且 > 20，连续 2 周 | `muscleCode`, `weeklyEffectiveSets`, `medianEffectiveSets` | 保持或减少 2—4 组，优先保证动作质量和恢复，不继续加量。 | WARN | 2 周 |
| `LOW_PLAN_COMPLETION` | 周计划完成度 < 60%，连续 2 周 | `plannedSets`, `completedSets`, `completionPct`, `weeks` | 先把计划组数减少约 20%，或把训练日调整到更容易坚持的时间。 | WARN | 2 周 |
| `LOW_FREQUENCY` | 每周训练次数 < 2，连续 2 周 | `sessionsPerWeek`, `weeklyTarget` | 先固定 2—3 个训练日，优先保证频率，再逐步增加训练量。 | WARN | 2 周 |
| `VOLUME_SPIKE` | 本周训练量 > 过去 4 周平均值的 1.3 倍 | `weeklyVolumeKg`, `fourWeekAverageKg`, `increasePct` | 本周不继续加量，优先完成原计划；下周再根据恢复情况调整。 | WARN | 5 周 |
| `PLAN_DEVIATION` | 4 周内换动作或跳过的训练占比 > 30% | `deviationSessions`, `totalSessions`, `deviationPct`, `topSwappedExercises` | 把经常替换的动作直接写进计划，减少“计划一套、实际一套”的偏差。 | INFO | 4 周 |
| `PAIN_FLAG` | `feeling = PAIN`，或备注命中疼痛关键词 | `sessionDate`, `exerciseName`, `note` | 停止会加重疼痛的动作，记录出现时间和部位，并咨询医生或康复专业人士。 | ALERT | 单次训练 |
| `BODYWEIGHT_TREND` | 4 周内周体重变化率绝对值 > 1%，且与目标方向不一致 | `weeklyChangePct`, `goal`, `weeks` | 先确认记录是否稳定；体重趋势仅作参考，不提供饮食或医疗处方。 | INFO | 4 周 |

### 5.3 规则实现说明

1. 每个规则输出一个 `Finding` 对象，包含 `findingKey`、`ruleCode`、`severity`、`title`、`evidence`、`suggestionTemplate`。
2. `findingKey` 建议格式：`规则编号:对象ID`，例如 `WEIGHT_STALL:12`、`MUSCLE_UNDERTRAINED:CHEST`。
3. 同一规则同一对象在一个分析窗口内只生成一条，避免重复提示。
4. 规则按严重级别排序：`ALERT` > `WARN` > `INFO`；同级别按证据强度排序。
5. 每次最多展示 5 条，避免建议过多让用户无法执行。

### 5.4 建议输出结构

后端给模型的输入只包含 `finding` 和必要的上下文，模型输出只包含 `findingKey` 与改写后的建议：

```json
{
  "summary": "最近四周下肢训练稳定，但胸部有效组数偏少。",
  "items": [
    {
      "findingKey": "WEIGHT_STALL:12",
      "suggestion": "卧推近四周预计 1RM 没有明显变化。本周保持 60 kg，尝试每组多做 1 次；若连续两次未完成，再考虑减载 10%。"
    },
    {
      "findingKey": "MUSCLE_UNDERTRAINED:CHEST",
      "suggestion": "胸部本周有效组数低于下肢。下周增加 2—4 组卧推或上斜推，优先保证动作质量。"
    }
  ],
  "disclaimer": "以上建议仅基于训练记录，不构成医疗建议。"
}
```

### 5.5 校验与兜底

模型返回后必须校验：

1. `findingKey` 必须存在于后端生成的 finding 集合中。
2. 不允许模型新增规则或新增 finding。
3. 每条建议长度建议为 20—200 个汉字。
4. 不允许出现诊断、用药、治疗方案等医疗表述。
5. 建议中的数字必须来自 `evidence`，不能由模型自行编造。
6. 校验失败只允许修复一次；仍失败则使用 `suggestionTemplate`。
7. 外部接口超时建议 10 秒；超时直接走兜底，不阻塞训练页面。

### 5.6 隐私与安全

- 发送给模型的数据使用白名单字段，不发送用户名、密码哈希、令牌、登录记录。
- 只发送分析窗口内的训练摘要、目标、训练年限和必要的身体数据。
- 身体数据能不用就不用；自重动作需要体重时，可四舍五入后发送。
- 模型密钥只放在后端环境变量中，不能出现在 Vue 代码或浏览器请求里。
- `ai_advice` 保存请求摘要和返回文本，便于审计；不保存完整敏感提示词。

---

## 6. 与论文的对应关系

| 论文章节 | 使用本文档的内容 |
|---|---|
| 需求分析 | 表清单、角色、计划/训练/统计/建议四类用例 |
| 数据库设计 | 第 2 章字段清单、E-R 图、唯一约束和索引 |
| 系统实现 | 第 3 章事务边界、第 4 章统计公式、第 5 章规则表 |
| 系统测试 | 4.11 手工验算、4.10 边界场景、5.5 兜底测试 |
| 结论与不足 | 体重/1RM 估算误差、规则阈值可配置、AI 仅作辅助 |

写作建议：

- 论文中不要写“系统实现了 openGym 的全部功能”，而是写“参考了组级记录、计划快照和有效组数统计等设计思想，并在 Spring Boot + Vue + MySQL 下实现”。
- 每个统计数字都要能追溯到 `workout_set` 的原始记录。
- 每个 AI 建议都要能追溯到一条规则和一组证据，避免“模型随便生成”。

---

## 7. 建议实施顺序

### P0：先做闭环

1. `sys_user`、`user_profile`、`body_metric`
2. `muscle_group`、`equipment`、`exercise`、`exercise_muscle`
3. `workout_plan`、`plan_day`、`plan_exercise`
4. `workout_session`、`session_exercise`、`workout_set`
5. 统计查询与手工验算

### P1：再做增强

1. 规则引擎、`ai_advice`、`ai_advice_item` 与模板文案
2. 双重渐进和减载规则
3. 接入外部 AI 服务，完成文案润色、校验与兜底
4. `ai_advice_feedback`
5. `admin_audit_log`
6. 递减组/休息暂停的 `detail_json`

### P2：不建议现在做

- 社交、排行榜、好友
- 动作视频和 3D 模型
- 移动端 App
- 多设备实时同步
- 从其他健身 App 导入数据

---

## 8. 已确认的 4 个口径（2026-10-19）

1. **自重动作**：不计入 kg 训练量，单独按次数和组数统计。
2. **单侧动作**：左右两侧各计 0.5 组，两侧完成合计 1 组。
3. **周起始日**：周一。
4. **AI 服务**：V1 先不接外部模型，只做规则引擎和模板文案；后续再接入 OpenAI 兼容接口。

以上口径已经确定，后续 MySQL 建表脚本、Flyway 迁移文件和 JUnit 测试用例都按此执行。
