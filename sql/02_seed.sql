-- 健身计划与训练记录系统：基础字典与动作库种子数据
-- 依赖 01_schema.sql

USE fitness_db;
SET NAMES utf8mb4;

-- ---------- 肌肉部位 ----------
INSERT INTO muscle_group (code, name, body_region, sort_no) VALUES
('CHEST',           '胸部',       'UPPER', 10),
('UPPER_BACK',      '上背',       'UPPER', 20),
('LOWER_BACK',      '下背',       'LOWER', 30),
('SHOULDERS',       '肩部',       'UPPER', 40),
('BICEPS',          '肱二头肌',   'UPPER', 50),
('TRICEPS',         '肱三头肌',   'UPPER', 60),
('FOREARMS',        '前臂',       'UPPER', 70),
('RECTUS_ABDOMINIS','腹直肌',     'CORE',  80),
('OBLIQUES',        '腹斜肌',     'CORE',  90),
('GLUTES',          '臀部',       'LOWER', 100),
('QUADS',           '股四头肌',   'LOWER', 110),
('HAMSTRINGS',      '腘绳肌',     'LOWER', 120),
('ADDUCTORS',       '内收肌',     'LOWER', 130),
('HIP_FLEXORS',     '髋屈肌',     'LOWER', 140),
('CALVES',          '小腿',       'LOWER', 150),
('TIBIALIS',        '胫骨前肌',   'LOWER', 160),
('TRAPS',           '斜方肌',     'UPPER', 170),
('SERRATUS',        '前锯肌',     'UPPER', 180)
ON DUPLICATE KEY UPDATE name = VALUES(name), body_region = VALUES(body_region), sort_no = VALUES(sort_no);

-- ---------- 器械 ----------
INSERT INTO equipment (code, name) VALUES
('BARBELL',   '杠铃'),
('DUMBBELL',  '哑铃'),
('MACHINE',   '固定器械'),
('CABLE',     '绳索'),
('BODYWEIGHT','自重'),
('KETTLEBELL','壶铃'),
('BAND',      '弹力带'),
('SMITH',     '史密斯机'),
('EZ_BAR',    '曲杠'),
('OTHER',     '其他')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ---------- 动作库 ----------
CREATE TEMPORARY TABLE tmp_exercise_seed (
  name         VARCHAR(100) NOT NULL,
  body_part    VARCHAR(30)  NOT NULL,
  equipment    VARCHAR(30)  NULL,
  category     VARCHAR(20)  NOT NULL,
  mode         VARCHAR(10)  NOT NULL,
  description  TEXT         NULL,
  instructions JSON         NULL
);

INSERT INTO tmp_exercise_seed VALUES
('杠铃卧推', 'CHEST', 'BARBELL', 'STRENGTH', 'REPS', '胸部基础复合动作，主要发展胸大肌、肱三头肌和三角肌前束。', JSON_ARRAY('仰卧在卧推凳上，双脚踩稳地面，肩胛骨后缩下沉。', '双手略宽于肩握杠，杠铃下放至胸部中下沿。', '保持手腕中立，向上推起至手臂接近伸直。')),
('上斜哑铃卧推', 'CHEST', 'DUMBBELL', 'STRENGTH', 'REPS', '上斜角度更强调胸大肌上束。', JSON_ARRAY('调整凳面至30—45度，双脚踩稳。', '哑铃下放至胸部两侧，肘部约45度。', '向上推起并在顶端保持控制。')),
('哑铃飞鸟', 'CHEST', 'DUMBBELL', 'STRENGTH', 'REPS', '孤立胸大肌，强调拉伸与收缩。', JSON_ARRAY('仰卧持哑铃，手臂微屈。', '以肩关节为轴向外下放，感受胸部拉伸。', '沿弧线合拢哑铃，不要互相碰撞。')),
('引体向上', 'BACK', 'BODYWEIGHT', 'STRENGTH', 'REPS', '垂直拉动作，主要发展背阔肌和肱二头肌。', JSON_ARRAY('正握单杠，握距略宽于肩。', '肩胛骨下沉，拉起身体至下巴过杠。', '控制下放至手臂伸直。')),
('杠铃划船', 'BACK', 'BARBELL', 'STRENGTH', 'REPS', '水平拉复合动作，发展上背厚度。', JSON_ARRAY('屈髋俯身，背部保持中立。', '杠铃拉向下腹部，肘部贴近身体。', '控制下放，不要借助惯性。')),
('高位下拉', 'BACK', 'CABLE', 'STRENGTH', 'REPS', '适合作为引体向上的替代或辅助动作。', JSON_ARRAY('坐稳并固定大腿，正握横杆。', '肩胛骨下沉，将横杆拉至上胸。', '缓慢还原至手臂伸直。')),
('坐姿划船', 'BACK', 'CABLE', 'STRENGTH', 'REPS', '发展背部中部厚度。', JSON_ARRAY('坐直，双脚踩稳踏板。', '拉动手柄至腹部，肩胛骨向后收拢。', '控制还原，保持躯干稳定。')),
('杠铃深蹲', 'LEGS', 'BARBELL', 'STRENGTH', 'REPS', '下肢基础复合动作，主要发展股四头肌和臀部。', JSON_ARRAY('杠铃置于上背，双脚与肩同宽。', '屈髋屈膝下蹲，膝盖与脚尖方向一致。', '蹲至大腿接近平行，向上站起。')),
('腿举', 'LEGS', 'MACHINE', 'STRENGTH', 'REPS', '固定器械下肢动作，便于控制负荷。', JSON_ARRAY('背部贴紧靠垫，双脚与肩同宽。', '解锁后屈膝下放至舒适深度。', '蹬起至膝盖接近伸直，不要锁死。')),
('罗马尼亚硬拉', 'LEGS', 'BARBELL', 'STRENGTH', 'REPS', '髋主导动作，重点发展腘绳肌和臀部。', JSON_ARRAY('双手握杠，膝盖微屈。', '屈髋下放，杠铃贴近腿部。', '感受大腿后侧拉伸后伸髋站起。')),
('保加利亚分腿蹲', 'LEGS', 'DUMBBELL', 'STRENGTH', 'REPS', '单侧下肢动作，兼顾力量与稳定性。', JSON_ARRAY('后脚放在凳上，前脚站稳。', '垂直下蹲至前腿大腿接近平行。', '前脚发力站起，保持骨盆稳定。')),
('哑铃肩推', 'SHOULDERS', 'DUMBBELL', 'STRENGTH', 'REPS', '主要发展三角肌前束和中束。', JSON_ARRAY('坐姿或站姿持哑铃于肩侧。', '向上推起至手臂接近伸直。', '控制下放至肘部略低于肩。')),
('哑铃侧平举', 'SHOULDERS', 'DUMBBELL', 'STRENGTH', 'REPS', '孤立三角肌中束。', JSON_ARRAY('双手持哑铃自然下垂。', '肘部微屈，向两侧抬起至肩高。', '控制下放，避免耸肩。')),
('杠铃弯举', 'ARMS', 'EZ_BAR', 'STRENGTH', 'REPS', '发展肱二头肌。', JSON_ARRAY('双手握曲杠，肘部贴近身体。', '屈肘将杠铃举起。', '控制下放至手臂接近伸直。')),
('绳索下压', 'ARMS', 'CABLE', 'STRENGTH', 'REPS', '孤立肱三头肌。', JSON_ARRAY('面对绳索站立，肘部固定。', '向下压至手臂伸直。', '控制还原，保持上臂不动。')),
('平板支撑', 'CORE', 'BODYWEIGHT', 'STRENGTH', 'TIME', '等长核心动作，训练躯干稳定性。', JSON_ARRAY('前臂撑地，肘部位于肩下。', '身体从头到脚保持一条直线。', '收紧腹部和臀部，保持自然呼吸。')),
('卷腹', 'CORE', 'BODYWEIGHT', 'STRENGTH', 'REPS', '发展腹直肌。', JSON_ARRAY('仰卧屈膝，双脚踩地。', '收紧腹部抬起肩胛骨。', '缓慢下放，避免用手拉头。')),
('臀桥', 'GLUTES', 'BARBELL', 'STRENGTH', 'REPS', '髋伸动作，重点发展臀部。', JSON_ARRAY('仰卧，杠铃置于髋部。', '脚跟发力将髋部抬起。', '顶端收紧臀部后控制下放。')),
('跑步机慢跑', 'CARDIO', 'MACHINE', 'CARDIO', 'CARDIO', '低强度有氧，用于心肺训练和热身。', JSON_ARRAY('从低速开始，逐渐提高到目标心率区间。', '保持自然步幅和呼吸。', '结束前逐步降速。')),
('划船机', 'CARDIO', 'MACHINE', 'CARDIO', 'CARDIO', '全身有氧动作，兼顾心肺与背部耐力。', JSON_ARRAY('顺序为蹬腿、后仰、拉手。', '还原时顺序相反。', '保持节奏稳定。'));

INSERT INTO exercise (name, body_part, equipment_id, category, mode, description, instructions, is_custom, status)
SELECT s.name, s.body_part, e.id, s.category, s.mode, s.description, s.instructions, 0, 'ACTIVE'
FROM tmp_exercise_seed s
LEFT JOIN equipment e ON e.code = s.equipment
WHERE NOT EXISTS (SELECT 1 FROM exercise x WHERE x.name = s.name);

DROP TEMPORARY TABLE tmp_exercise_seed;

-- ---------- 动作与肌肉权重 ----------
CREATE TEMPORARY TABLE tmp_exercise_muscle (
  exercise_name VARCHAR(100) NOT NULL,
  muscle_code   VARCHAR(30)  NOT NULL,
  role          VARCHAR(10)  NOT NULL,
  weight        DECIMAL(3,2) NOT NULL
);

INSERT INTO tmp_exercise_muscle VALUES
('杠铃卧推', 'CHEST', 'PRIMARY', 1.00),
('杠铃卧推', 'TRICEPS', 'SECONDARY', 0.40),
('杠铃卧推', 'SHOULDERS', 'SECONDARY', 0.40),
('上斜哑铃卧推', 'CHEST', 'PRIMARY', 1.00),
('上斜哑铃卧推', 'SHOULDERS', 'SECONDARY', 0.40),
('上斜哑铃卧推', 'TRICEPS', 'SECONDARY', 0.40),
('哑铃飞鸟', 'CHEST', 'PRIMARY', 1.00),
('哑铃飞鸟', 'SHOULDERS', 'SECONDARY', 0.40),
('引体向上', 'UPPER_BACK', 'PRIMARY', 1.00),
('引体向上', 'BICEPS', 'SECONDARY', 0.40),
('杠铃划船', 'UPPER_BACK', 'PRIMARY', 1.00),
('杠铃划船', 'LOWER_BACK', 'SECONDARY', 0.40),
('杠铃划船', 'BICEPS', 'SECONDARY', 0.40),
('高位下拉', 'UPPER_BACK', 'PRIMARY', 1.00),
('高位下拉', 'BICEPS', 'SECONDARY', 0.40),
('坐姿划船', 'UPPER_BACK', 'PRIMARY', 1.00),
('坐姿划船', 'BICEPS', 'SECONDARY', 0.40),
('杠铃深蹲', 'QUADS', 'PRIMARY', 1.00),
('杠铃深蹲', 'GLUTES', 'SECONDARY', 0.40),
('杠铃深蹲', 'HAMSTRINGS', 'SECONDARY', 0.40),
('腿举', 'QUADS', 'PRIMARY', 1.00),
('腿举', 'GLUTES', 'SECONDARY', 0.40),
('罗马尼亚硬拉', 'HAMSTRINGS', 'PRIMARY', 1.00),
('罗马尼亚硬拉', 'GLUTES', 'SECONDARY', 0.40),
('罗马尼亚硬拉', 'LOWER_BACK', 'SECONDARY', 0.40),
('保加利亚分腿蹲', 'QUADS', 'PRIMARY', 1.00),
('保加利亚分腿蹲', 'GLUTES', 'SECONDARY', 0.40),
('哑铃肩推', 'SHOULDERS', 'PRIMARY', 1.00),
('哑铃肩推', 'TRICEPS', 'SECONDARY', 0.40),
('哑铃侧平举', 'SHOULDERS', 'PRIMARY', 1.00),
('杠铃弯举', 'BICEPS', 'PRIMARY', 1.00),
('杠铃弯举', 'FOREARMS', 'SECONDARY', 0.40),
('绳索下压', 'TRICEPS', 'PRIMARY', 1.00),
('平板支撑', 'RECTUS_ABDOMINIS', 'PRIMARY', 1.00),
('平板支撑', 'OBLIQUES', 'SECONDARY', 0.40),
('卷腹', 'RECTUS_ABDOMINIS', 'PRIMARY', 1.00),
('臀桥', 'GLUTES', 'PRIMARY', 1.00),
('臀桥', 'HAMSTRINGS', 'SECONDARY', 0.40);

INSERT INTO exercise_muscle (exercise_id, muscle_group_id, role, weight)
SELECT e.id, m.id, t.role, t.weight
FROM tmp_exercise_muscle t
JOIN exercise e ON e.name = t.exercise_name
JOIN muscle_group m ON m.code = t.muscle_code
ON DUPLICATE KEY UPDATE role = VALUES(role), weight = VALUES(weight);

DROP TEMPORARY TABLE tmp_exercise_muscle;
