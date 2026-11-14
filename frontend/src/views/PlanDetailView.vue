<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getExercises } from '../api/exercise'
import {
  addPlanDay,
  addPlanExercise,
  deletePlan,
  deletePlanDay,
  deletePlanExercise,
  getPlan,
  updatePlan,
  updatePlanDay,
  updatePlanExercise
} from '../api/plans'
import { startSession } from '../api/sessions'

const route = useRoute()
const router = useRouter()

const plan = ref(null)
const loading = ref(false)
const exerciseOptions = ref([])

const planDialogVisible = ref(false)
const planSaving = ref(false)
const planFormRef = ref()
const planForm = reactive({
  name: '',
  goal: 'HYPERTROPHY',
  startDate: '',
  weeks: 4,
  status: 'DRAFT'
})

const dayDialogVisible = ref(false)
const daySaving = ref(false)
const dayFormRef = ref()
const dayEditingId = ref(null)
const dayForm = reactive({
  weekNo: 1,
  dayOfWeek: 1,
  title: ''
})

const exerciseDialogVisible = ref(false)
const exerciseSaving = ref(false)
const exerciseFormRef = ref()
const exerciseEditingId = ref(null)
const exerciseDayId = ref(null)
const exerciseForm = reactive({
  exerciseId: null,
  targetSets: 3,
  targetRepsMin: 8,
  targetRepsMax: 12,
  targetWeightKg: null,
  targetDurationSec: null,
  restSeconds: 90,
  progressionType: 'NONE',
  incrementKg: 2.5,
  incrementReps: 1,
  note: '',
  sortNo: null
})

const goalOptions = [
  { value: 'HYPERTROPHY', label: '增肌' },
  { value: 'STRENGTH', label: '力量' },
  { value: 'FAT_LOSS', label: '减脂' },
  { value: 'GENERAL', label: '综合' }
]

const statusMeta = {
  DRAFT: { label: '草稿', type: 'info' },
  ACTIVE: { label: '进行中', type: 'success' },
  ARCHIVED: { label: '已归档', type: 'warning' }
}

const bodyPartLabels = {
  CHEST: '胸部',
  BACK: '背部',
  LEGS: '腿部',
  GLUTES: '臀部',
  SHOULDERS: '肩部',
  ARMS: '手臂',
  CORE: '核心',
  CARDIO: '有氧'
}

const selectedExercise = computed(() =>
  exerciseOptions.value.find((item) => item.id === exerciseForm.exerciseId)
)
const selectedMode = computed(() => selectedExercise.value?.mode || 'REPS')

const planRules = {
  name: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  weeks: [{ required: true, message: '请输入计划周数', trigger: 'change' }]
}

const dayRules = {
  weekNo: [{ required: true, message: '请选择计划周', trigger: 'change' }],
  dayOfWeek: [{ required: true, message: '请选择训练日', trigger: 'change' }],
  title: [{ required: true, message: '请输入训练日名称', trigger: 'blur' }]
}

const exerciseRules = {
  exerciseId: [{ required: true, message: '请选择动作', trigger: 'change' }],
  targetSets: [{ required: true, message: '请输入计划组数', trigger: 'change' }]
}

function dayName(dayOfWeek) {
  return ['周一', '周二', '周三', '周四', '周五', '周六', '周日'][dayOfWeek - 1] || '未知'
}

function goalLabel(value) {
  return goalOptions.find((item) => item.value === value)?.label || value || '未设置'
}

function daysForWeek(week) {
  return (plan.value?.days || [])
    .filter((item) => item.weekNo === week)
    .sort((a, b) => a.dayOfWeek - b.dayOfWeek || a.sortNo - b.sortNo)
}

function targetText(item) {
  if (item.mode === 'TIME' || item.mode === 'CARDIO') {
    return `${item.targetSets} 组 × ${item.targetDurationSec || 0} 秒`
  }
  const reps = item.targetRepsMin === item.targetRepsMax
    ? item.targetRepsMax
    : `${item.targetRepsMin ?? 0}—${item.targetRepsMax ?? 0}`
  const weight = item.targetWeightKg != null ? ` · ${item.targetWeightKg} kg` : ''
  return `${item.targetSets} 组 × ${reps} 次${weight}`
}

async function loadPlan() {
  loading.value = true
  try {
    plan.value = await getPlan(route.params.id)
  } catch {
    router.push('/plans')
  } finally {
    loading.value = false
  }
}

async function loadExercises() {
  try {
    const data = await getExercises({ page: 1, size: 100 })
    exerciseOptions.value = data.records
  } catch {
    // 请求错误已统一提示
  }
}

function openPlanEdit() {
  Object.assign(planForm, {
    name: plan.value.name,
    goal: plan.value.goal || 'GENERAL',
    startDate: plan.value.startDate || '',
    weeks: plan.value.weeks,
    status: plan.value.status
  })
  planDialogVisible.value = true
}

async function savePlan() {
  const valid = await planFormRef.value.validate().catch(() => false)
  if (!valid) return

  planSaving.value = true
  try {
    plan.value = await updatePlan(plan.value.id, { ...planForm })
    planDialogVisible.value = false
    ElMessage.success('计划信息已更新')
  } catch {
    // 请求错误已统一提示
  } finally {
    planSaving.value = false
  }
}

async function togglePlanStatus() {
  const next = plan.value.status === 'ACTIVE' ? 'DRAFT' : 'ACTIVE'
  try {
    plan.value = await updatePlan(plan.value.id, { status: next })
    ElMessage.success(next === 'ACTIVE' ? '计划已启用' : '计划已转为草稿')
  } catch {
    // 请求错误已统一提示
  }
}

async function removePlan() {
  try {
    await ElMessageBox.confirm(
      '计划下的训练日会一并删除，已经完成的训练记录会保留。确定删除吗？',
      '删除计划',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deletePlan(plan.value.id)
    ElMessage.success('计划已删除')
    router.push('/plans')
  } catch {
    // 用户取消或请求失败
  }
}

function openAddDay(weekNo) {
  dayEditingId.value = null
  const used = new Set(daysForWeek(weekNo).map((item) => item.dayOfWeek))
  let dayOfWeek = 1
  while (used.has(dayOfWeek) && dayOfWeek <= 7) dayOfWeek++
  Object.assign(dayForm, {
    weekNo,
    dayOfWeek: Math.min(dayOfWeek, 7),
    title: ''
  })
  dayDialogVisible.value = true
}

function openEditDay(day) {
  dayEditingId.value = day.id
  Object.assign(dayForm, {
    weekNo: day.weekNo,
    dayOfWeek: day.dayOfWeek,
    title: day.title
  })
  dayDialogVisible.value = true
}

async function saveDay() {
  const valid = await dayFormRef.value.validate().catch(() => false)
  if (!valid) return

  daySaving.value = true
  try {
    plan.value = dayEditingId.value
      ? await updatePlanDay(dayEditingId.value, { ...dayForm })
      : await addPlanDay(plan.value.id, { ...dayForm })
    dayDialogVisible.value = false
    ElMessage.success(dayEditingId.value ? '训练日已更新' : '训练日已添加')
  } catch {
    // 请求错误已统一提示
  } finally {
    daySaving.value = false
  }
}

async function removeDay(day) {
  try {
    await ElMessageBox.confirm(`删除「${day.title}」及其动作安排吗？`, '删除训练日', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    plan.value = await deletePlanDay(day.id)
    ElMessage.success('训练日已删除')
  } catch {
    // 用户取消或请求失败
  }
}

function resetExerciseForm() {
  Object.assign(exerciseForm, {
    exerciseId: null,
    targetSets: 3,
    targetRepsMin: 8,
    targetRepsMax: 12,
    targetWeightKg: null,
    targetDurationSec: null,
    restSeconds: 90,
    progressionType: 'NONE',
    incrementKg: 2.5,
    incrementReps: 1,
    note: '',
    sortNo: null
  })
}

function openAddExercise(day) {
  exerciseEditingId.value = null
  exerciseDayId.value = day.id
  resetExerciseForm()
  exerciseDialogVisible.value = true
}

function openEditExercise(day, item) {
  exerciseEditingId.value = item.id
  exerciseDayId.value = day.id
  Object.assign(exerciseForm, {
    exerciseId: item.exerciseId,
    targetSets: item.targetSets,
    targetRepsMin: item.targetRepsMin,
    targetRepsMax: item.targetRepsMax,
    targetWeightKg: item.targetWeightKg,
    targetDurationSec: item.targetDurationSec,
    restSeconds: item.restSeconds,
    progressionType: item.progressionType || 'NONE',
    incrementKg: item.incrementKg ?? 2.5,
    incrementReps: item.incrementReps ?? 1,
    note: item.note || '',
    sortNo: item.sortNo
  })
  handleExerciseChange(item.exerciseId, false)
  exerciseDialogVisible.value = true
}

function handleExerciseChange(value, resetTargets = true) {
  const exercise = exerciseOptions.value.find((item) => item.id === value)
  if (!exercise || !resetTargets) return
  if (exercise.mode === 'REPS') {
    exerciseForm.targetRepsMin = 8
    exerciseForm.targetRepsMax = 12
    exerciseForm.targetDurationSec = null
  } else {
    exerciseForm.targetRepsMin = null
    exerciseForm.targetRepsMax = null
    exerciseForm.targetDurationSec = 60
    exerciseForm.targetWeightKg = null
  }
}

async function saveExercise() {
  const valid = await exerciseFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (selectedMode.value === 'REPS' && exerciseForm.targetRepsMin == null
    && exerciseForm.targetRepsMax == null) {
    ElMessage.warning('次数类动作请填写目标次数')
    return
  }
  if (selectedMode.value !== 'REPS' && !exerciseForm.targetDurationSec) {
    ElMessage.warning('计时或有氧动作请填写目标时长')
    return
  }
  if (exerciseForm.targetRepsMin != null && exerciseForm.targetRepsMax != null
    && exerciseForm.targetRepsMin > exerciseForm.targetRepsMax) {
    ElMessage.warning('目标次数下限不能大于上限')
    return
  }

  const payload = {
    exerciseId: exerciseForm.exerciseId,
    sortNo: exerciseForm.sortNo,
    targetSets: exerciseForm.targetSets,
    targetRepsMin: selectedMode.value === 'REPS' ? exerciseForm.targetRepsMin : null,
    targetRepsMax: selectedMode.value === 'REPS' ? exerciseForm.targetRepsMax : null,
    targetWeightKg: selectedMode.value === 'CARDIO' ? null : exerciseForm.targetWeightKg,
    targetDurationSec: selectedMode.value === 'REPS' ? null : exerciseForm.targetDurationSec,
    restSeconds: exerciseForm.restSeconds,
    progressionType: exerciseForm.progressionType,
    incrementKg: exerciseForm.incrementKg,
    incrementReps: exerciseForm.incrementReps,
    note: exerciseForm.note || null
  }

  exerciseSaving.value = true
  try {
    plan.value = exerciseEditingId.value
      ? await updatePlanExercise(exerciseEditingId.value, payload)
      : await addPlanExercise(exerciseDayId.value, payload)
    exerciseDialogVisible.value = false
    ElMessage.success(exerciseEditingId.value ? '动作安排已更新' : '动作已加入计划')
  } catch {
    // 请求错误已统一提示
  } finally {
    exerciseSaving.value = false
  }
}

async function removeExercise(item) {
  try {
    await ElMessageBox.confirm(`从计划中移除「${item.exerciseName}」吗？`, '移除动作', {
      confirmButtonText: '移除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    plan.value = await deletePlanExercise(item.id)
    ElMessage.success('动作已移除')
  } catch {
    // 用户取消或请求失败
  }
}

async function startTraining(day) {
  try {
    const session = await startSession(day.id)
    router.push(`/sessions/${session.id}`)
  } catch {
    // 请求错误已统一提示
  }
}

onMounted(async () => {
  await Promise.all([loadPlan(), loadExercises()])
})
</script>

<template>
  <div v-loading="loading" class="page">
    <template v-if="plan">
      <div class="detail-head">
        <div>
          <el-button text class="back-btn" @click="router.push('/plans')">
            <el-icon><ArrowLeft /></el-icon>
            返回计划列表
          </el-button>
          <div class="title-row">
            <h1>{{ plan.name }}</h1>
            <el-tag :type="statusMeta[plan.status]?.type || 'info'">
              {{ statusMeta[plan.status]?.label || plan.status }}
            </el-tag>
          </div>
          <p>{{ goalLabel(plan.goal) }} · {{ plan.weeks }} 周 · 开始于 {{ plan.startDate || '未设置' }}</p>
        </div>
        <div class="head-actions">
          <el-button @click="togglePlanStatus">
            {{ plan.status === 'ACTIVE' ? '转为草稿' : '启用计划' }}
          </el-button>
          <el-button @click="openPlanEdit">编辑信息</el-button>
          <el-button type="danger" plain @click="removePlan">删除</el-button>
        </div>
      </div>

      <section v-for="week in plan.weeks" :key="week" class="week-card card">
        <div class="week-head">
          <div>
            <span class="week-index">W{{ week }}</span>
            <h2>第 {{ week }} 周</h2>
          </div>
          <el-button type="primary" plain @click="openAddDay(week)">
            <el-icon><Plus /></el-icon>
            添加训练日
          </el-button>
        </div>

        <div v-if="daysForWeek(week).length" class="day-grid">
          <article v-for="day in daysForWeek(week)" :key="day.id" class="day-card">
            <header class="day-head">
              <div>
                <span class="day-label">{{ dayName(day.dayOfWeek) }}</span>
                <h3>{{ day.title }}</h3>
              </div>
              <el-dropdown trigger="click">
                <el-button text><el-icon><MoreFilled /></el-icon></el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="openEditDay(day)">编辑训练日</el-dropdown-item>
                    <el-dropdown-item divided @click="removeDay(day)">删除训练日</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </header>

            <div v-if="day.exercises.length" class="exercise-list">
              <div v-for="item in day.exercises" :key="item.id" class="exercise-row">
                <div class="exercise-main">
                  <strong>{{ item.exerciseName }}</strong>
                  <span>
                    {{ bodyPartLabels[item.bodyPart] || item.bodyPart }}
                    <template v-if="item.equipmentName"> · {{ item.equipmentName }}</template>
                  </span>
                </div>
                <div class="target">{{ targetText(item) }}</div>
                <el-dropdown trigger="click">
                  <el-button text size="small"><el-icon><MoreFilled /></el-icon></el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="openEditExercise(day, item)">编辑动作</el-dropdown-item>
                      <el-dropdown-item divided @click="removeExercise(item)">移除动作</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
            <div v-else class="day-empty">还没有动作安排</div>

            <footer class="day-actions">
              <el-button type="primary" size="small" :disabled="!day.exercises.length" @click="startTraining(day)">
                <el-icon><VideoPlay /></el-icon>
                开始训练
              </el-button>
              <el-button size="small" @click="openAddExercise(day)">
                <el-icon><Plus /></el-icon>
                添加动作
              </el-button>
            </footer>
          </article>
        </div>
        <div v-else class="week-empty">这一周还没有训练安排，先添加一个训练日。</div>
      </section>
    </template>

    <el-dialog v-model="planDialogVisible" title="编辑计划信息" width="480px">
      <el-form ref="planFormRef" :model="planForm" :rules="planRules" label-position="top">
        <el-form-item label="计划名称" prop="name">
          <el-input v-model="planForm.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="训练目标">
          <el-select v-model="planForm.goal" style="width: 100%">
            <el-option v-for="item in goalOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="planForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计划周数" prop="weeks">
          <el-input-number v-model="planForm.weeks" :min="1" :max="52" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计划状态">
          <el-select v-model="planForm.status" style="width: 100%">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="进行中" value="ACTIVE" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="planSaving" @click="savePlan">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dayDialogVisible" :title="dayEditingId ? '编辑训练日' : '添加训练日'" width="460px">
      <el-form ref="dayFormRef" :model="dayForm" :rules="dayRules" label-position="top">
        <el-form-item label="计划周" prop="weekNo">
          <el-select v-model="dayForm.weekNo" style="width: 100%">
            <el-option v-for="week in plan?.weeks || 0" :key="week" :label="`第 ${week} 周`" :value="week" />
          </el-select>
        </el-form-item>
        <el-form-item label="训练日" prop="dayOfWeek">
          <el-select v-model="dayForm.dayOfWeek" style="width: 100%">
            <el-option v-for="day in 7" :key="day" :label="dayName(day)" :value="day" />
          </el-select>
        </el-form-item>
        <el-form-item label="训练日名称" prop="title">
          <el-input v-model="dayForm.title" maxlength="50" placeholder="例如：上肢推、下肢" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dayDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="daySaving" @click="saveDay">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="exerciseDialogVisible"
      :title="exerciseEditingId ? '编辑动作安排' : '添加动作'"
      width="560px"
    >
      <el-form ref="exerciseFormRef" :model="exerciseForm" :rules="exerciseRules" label-position="top">
        <el-form-item label="动作" prop="exerciseId">
          <el-select
            v-model="exerciseForm.exerciseId"
            filterable
            placeholder="搜索动作名称"
            style="width: 100%"
            @change="handleExerciseChange"
          >
            <el-option
              v-for="item in exerciseOptions"
              :key="item.id"
              :label="`${item.name} · ${bodyPartLabels[item.bodyPart] || item.bodyPart}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="计划组数" prop="targetSets">
            <el-input-number v-model="exerciseForm.targetSets" :min="1" :max="30" style="width: 100%" />
          </el-form-item>
          <el-form-item label="组间休息（秒）">
            <el-input-number v-model="exerciseForm.restSeconds" :min="0" :max="600" :step="15" style="width: 100%" />
          </el-form-item>
        </div>

        <div v-if="selectedMode === 'REPS'" class="form-grid">
          <el-form-item label="最少次数">
            <el-input-number v-model="exerciseForm.targetRepsMin" :min="0" :max="100" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最多次数">
            <el-input-number v-model="exerciseForm.targetRepsMax" :min="0" :max="100" style="width: 100%" />
          </el-form-item>
        </div>

        <div v-else class="form-grid">
          <el-form-item label="目标时长（秒）">
            <el-input-number v-model="exerciseForm.targetDurationSec" :min="0" :max="7200" :step="10" style="width: 100%" />
          </el-form-item>
          <el-form-item v-if="selectedMode !== 'CARDIO'" label="目标重量（kg）">
            <el-input-number v-model="exerciseForm.targetWeightKg" :min="0" :max="1000" :step="2.5" style="width: 100%" />
          </el-form-item>
        </div>

        <div v-if="selectedMode === 'REPS'" class="form-grid">
          <el-form-item label="目标重量（kg）">
            <el-input-number v-model="exerciseForm.targetWeightKg" :min="0" :max="1000" :step="2.5" style="width: 100%" />
          </el-form-item>
          <el-form-item label="渐进方式">
            <el-select v-model="exerciseForm.progressionType" style="width: 100%">
              <el-option label="不自动渐进" value="NONE" />
              <el-option label="线性加重" value="LINEAR" />
              <el-option label="双渐进" value="DOUBLE" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="动作备注">
          <el-input v-model="exerciseForm.note" maxlength="255" placeholder="可选，例如：最后一组做到接近力竭" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exerciseDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="exerciseSaving" @click="saveExercise">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.detail-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
}

.back-btn {
  margin-left: -12px;
  color: #7a8496;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
}

.title-row h1 {
  margin: 0;
  font-size: 28px;
}

.detail-head p {
  margin: 8px 0 0;
  color: #7a8496;
  font-size: 13px;
}

.head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.week-card {
  padding: 22px 24px;
  margin-bottom: 16px;
}

.week-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.week-head > div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.week-head h2 {
  margin: 0;
  font-size: 17px;
}

.week-index {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 10px;
  background: #eef4ff;
  color: #3f7cff;
  font-size: 11px;
  font-weight: 800;
}

.day-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.day-card {
  display: flex;
  min-height: 230px;
  flex-direction: column;
  padding: 16px;
  border: 1px solid #e8ebf2;
  border-radius: 13px;
  background: #fbfcfe;
}

.day-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.day-label {
  color: #3f7cff;
  font-size: 11px;
  font-weight: 700;
}

.day-head h3 {
  margin: 4px 0 0;
  font-size: 16px;
}

.exercise-list {
  flex: 1;
  margin-top: 14px;
}

.exercise-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 0;
  border-bottom: 1px dashed #e8ebf2;
}

.exercise-row:last-child {
  border-bottom: none;
}

.exercise-main {
  min-width: 0;
  flex: 1;
}

.exercise-main strong {
  display: block;
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.exercise-main span {
  display: block;
  margin-top: 3px;
  color: #8b95a7;
  font-size: 10px;
}

.target {
  flex-shrink: 0;
  color: #5b6b85;
  font-size: 11px;
}

.day-empty {
  display: grid;
  flex: 1;
  min-height: 90px;
  place-items: center;
  color: #9aa4b5;
  font-size: 12px;
}

.day-actions {
  display: flex;
  gap: 8px;
  padding-top: 14px;
  border-top: 1px solid #eef1f6;
}

.week-empty {
  padding: 26px;
  border: 1px dashed #dce3ef;
  border-radius: 12px;
  color: #8b95a7;
  font-size: 13px;
  text-align: center;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

@media (max-width: 1150px) {
  .day-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 780px) {
  .detail-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .day-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
