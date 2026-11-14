<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getExercises } from '../api/exercise'
import {
  addSessionExercise,
  addWorkoutSet,
  cancelSession,
  deleteWorkoutSet,
  finishSession,
  getSession,
  replaceSessionExercise,
  skipSessionExercise,
  updateWorkoutSet
} from '../api/sessions'

const route = useRoute()
const router = useRouter()

const session = ref(null)
const loading = ref(false)
const exerciseOptions = ref([])

const setDialogVisible = ref(false)
const setSaving = ref(false)
const setEditingId = ref(null)
const setExerciseId = ref(null)
const setForm = reactive({
  setNo: 1,
  phase: 'WORK',
  setType: 'STRAIGHT',
  side: 'BOTH',
  weightKg: null,
  reps: null,
  durationSec: null,
  rir: null,
  rpe: null,
  isFailure: false,
  isCompleted: true,
  note: ''
})

const addExerciseVisible = ref(false)
const addExerciseSaving = ref(false)
const addExerciseId = ref(null)

const replaceVisible = ref(false)
const replaceSaving = ref(false)
const replaceTarget = ref(null)
const replaceExerciseId = ref(null)

const finishVisible = ref(false)
const finishSaving = ref(false)
const finishForm = reactive({
  overallRpe: 8,
  feeling: 'OK',
  note: ''
})

const statusMeta = {
  IN_PROGRESS: { label: '进行中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' }
}

const deviationMeta = {
  NONE: { label: '按计划', type: 'success' },
  ADDED: { label: '临时新增', type: 'primary' },
  SWAPPED: { label: '已替换', type: 'warning' },
  SKIPPED: { label: '已跳过', type: 'info' }
}

const feelingOptions = [
  { value: 'GREAT', label: '状态很好' },
  { value: 'OK', label: '正常' },
  { value: 'TIRED', label: '比较疲劳' },
  { value: 'PAIN', label: '有疼痛' }
]

const isActive = computed(() => session.value?.status === 'IN_PROGRESS')
const canFinish = computed(() => (session.value?.completedSets || 0) > 0)

function numberText(value, fallback = '—') {
  return value == null || value === '' ? fallback : Number(value).toLocaleString('zh-CN')
}

function plannedText(exercise) {
  if (exercise.mode === 'TIME' || exercise.mode === 'CARDIO') {
    return `${exercise.plannedSets || 0} 组 × ${exercise.plannedDurationSec || 0} 秒`
  }
  const reps = exercise.plannedRepsMin === exercise.plannedRepsMax
    ? exercise.plannedRepsMax
    : `${exercise.plannedRepsMin ?? 0}—${exercise.plannedRepsMax ?? 0}`
  const weight = exercise.plannedWeightKg != null ? ` · ${exercise.plannedWeightKg} kg` : ''
  return `${exercise.plannedSets || 0} 组 × ${reps} 次${weight}`
}

function setTypeText(item) {
  const phase = item.phase === 'WARMUP' ? '热身' : '正式'
  const type = { STRAIGHT: '常规', DROP: '递减', REST_PAUSE: '休息暂停' }[item.setType] || ''
  return `${phase} · ${type}`
}

function nextSetNo(exercise) {
  return (exercise.sets || []).reduce((max, item) => Math.max(max, item.setNo || 0), 0) + 1
}

async function loadSession() {
  loading.value = true
  try {
    session.value = await getSession(route.params.id)
  } catch {
    router.push('/sessions')
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

function openAddSet(exercise) {
  setEditingId.value = null
  setExerciseId.value = exercise.id
  Object.assign(setForm, {
    setNo: nextSetNo(exercise),
    phase: 'WORK',
    setType: 'STRAIGHT',
    side: 'BOTH',
    weightKg: exercise.plannedWeightKg,
    reps: exercise.plannedRepsMax ?? exercise.plannedRepsMin,
    durationSec: exercise.plannedDurationSec,
    rir: null,
    rpe: null,
    isFailure: false,
    isCompleted: true,
    note: ''
  })
  setDialogVisible.value = true
}

function openEditSet(exercise, item) {
  setEditingId.value = item.id
  setExerciseId.value = exercise.id
  Object.assign(setForm, {
    setNo: item.setNo,
    phase: item.phase,
    setType: item.setType,
    side: item.side,
    weightKg: item.weightKg,
    reps: item.reps,
    durationSec: item.durationSec,
    rir: item.rir,
    rpe: item.rpe,
    isFailure: Boolean(item.isFailure),
    isCompleted: item.isCompleted !== false,
    note: item.note || ''
  })
  setDialogVisible.value = true
}

async function saveSet() {
  if (setForm.weightKg == null && setForm.reps == null && setForm.durationSec == null) {
    ElMessage.warning('请至少填写重量、次数或时长中的一项')
    return
  }

  setSaving.value = true
  try {
    const payload = { ...setForm }
    if (setEditingId.value) {
      await updateWorkoutSet(setEditingId.value, payload)
    } else {
      await addWorkoutSet(session.value.id, setExerciseId.value, payload)
    }
    setDialogVisible.value = false
    ElMessage.success(setEditingId.value ? '这一组已更新' : '已添加一组')
    await loadSession()
  } catch {
    // 请求错误已统一提示
  } finally {
    setSaving.value = false
  }
}

async function removeSet(item) {
  try {
    await ElMessageBox.confirm(`删除第 ${item.setNo} 组记录吗？`, '删除训练组', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteWorkoutSet(item.id)
    ElMessage.success('训练组已删除')
    await loadSession()
  } catch {
    // 用户取消或请求失败
  }
}

function openAddExercise() {
  addExerciseId.value = null
  addExerciseVisible.value = true
}

async function saveAddExercise() {
  if (!addExerciseId.value) {
    ElMessage.warning('请选择动作')
    return
  }
  addExerciseSaving.value = true
  try {
    session.value = await addSessionExercise(session.value.id, { exerciseId: addExerciseId.value })
    addExerciseVisible.value = false
    ElMessage.success('动作已加入本次训练')
  } catch {
    // 请求错误已统一提示
  } finally {
    addExerciseSaving.value = false
  }
}

function openReplace(exercise) {
  replaceTarget.value = exercise
  replaceExerciseId.value = null
  replaceVisible.value = true
}

async function saveReplace() {
  if (!replaceExerciseId.value) {
    ElMessage.warning('请选择替换动作')
    return
  }
  replaceSaving.value = true
  try {
    session.value = await replaceSessionExercise(session.value.id, replaceTarget.value.id, {
      exerciseId: replaceExerciseId.value
    })
    replaceVisible.value = false
    ElMessage.success('动作已替换，计划快照仍然保留')
  } catch {
    // 请求错误已统一提示
  } finally {
    replaceSaving.value = false
  }
}

async function skipExercise(exercise) {
  try {
    await ElMessageBox.confirm(`将「${exercise.exerciseName}」标记为跳过吗？`, '跳过动作', {
      confirmButtonText: '跳过',
      cancelButtonText: '取消',
      type: 'warning'
    })
    session.value = await skipSessionExercise(session.value.id, exercise.id)
    ElMessage.success('已标记为跳过')
  } catch {
    // 用户取消或请求失败
  }
}

function openFinish() {
  Object.assign(finishForm, {
    overallRpe: session.value.overallRpe || 8,
    feeling: session.value.feeling || 'OK',
    note: session.value.note || ''
  })
  finishVisible.value = true
}

async function saveFinish() {
  finishSaving.value = true
  try {
    session.value = await finishSession(session.value.id, { ...finishForm })
    finishVisible.value = false
    ElMessage.success('训练已完成')
  } catch {
    // 请求错误已统一提示
  } finally {
    finishSaving.value = false
  }
}

async function cancelTraining() {
  try {
    await ElMessageBox.confirm('取消后本次训练不能继续记录，确定取消吗？', '取消训练', {
      confirmButtonText: '取消训练',
      cancelButtonText: '继续训练',
      type: 'warning'
    })
    session.value = await cancelSession(session.value.id)
    ElMessage.success('训练已取消')
  } catch {
    // 用户取消或请求失败
  }
}

onMounted(async () => {
  await Promise.all([loadSession(), loadExercises()])
})
</script>

<template>
  <div v-loading="loading" class="page">
    <template v-if="session">
      <div class="detail-head">
        <div>
          <el-button text class="back-btn" @click="router.push('/sessions')">
            <el-icon><ArrowLeft /></el-icon>
            返回训练记录
          </el-button>
          <div class="title-row">
            <h1>{{ session.planDayTitleSnapshot }}</h1>
            <el-tag :type="statusMeta[session.status]?.type || 'info'">
              {{ statusMeta[session.status]?.label || session.status }}
            </el-tag>
          </div>
          <p>{{ session.planNameSnapshot }} · {{ session.sessionDate }}</p>
        </div>
        <div v-if="isActive" class="head-actions">
          <el-button @click="openAddExercise">
            <el-icon><Plus /></el-icon>
            临时加动作
          </el-button>
          <el-button type="danger" plain @click="cancelTraining">取消训练</el-button>
          <el-button type="primary" :disabled="!canFinish" @click="openFinish">结束训练</el-button>
        </div>
      </div>

      <section class="summary-grid">
        <div class="summary-card card">
          <span>完成组数</span>
          <strong>{{ session.completedSets }}</strong>
          <small>共记录 {{ session.totalSets }} 组</small>
        </div>
        <div class="summary-card card">
          <span>训练量</span>
          <strong>{{ numberText(session.volumeKg) }}</strong>
          <small>kg · 正式组重量 × 次数</small>
        </div>
        <div class="summary-card card">
          <span>动作数量</span>
          <strong>{{ session.exerciseCount }}</strong>
          <small>计划动作与临时动作</small>
        </div>
        <div class="summary-card card">
          <span>整体强度</span>
          <strong>{{ session.overallRpe || '—' }}</strong>
          <small>{{ session.feeling ? feelingOptions.find((item) => item.value === session.feeling)?.label : '训练结束后填写' }}</small>
        </div>
      </section>

      <section v-for="exercise in session.exercises" :key="exercise.id" class="exercise-card card">
        <header class="exercise-head">
          <div>
            <div class="exercise-title">
              <h2>{{ exercise.exerciseName }}</h2>
              <el-tag
                v-if="exercise.deviationType && exercise.deviationType !== 'NONE'"
                :type="deviationMeta[exercise.deviationType]?.type || 'info'"
                size="small"
              >
                {{ deviationMeta[exercise.deviationType]?.label || exercise.deviationType }}
              </el-tag>
            </div>
            <p>计划：{{ plannedText(exercise) }}</p>
          </div>
          <div class="exercise-stats">
            <span><strong>{{ exercise.completedSets }}</strong> 完成组</span>
            <span><strong>{{ numberText(exercise.volumeKg) }}</strong> kg</span>
            <span v-if="exercise.estimated1RmKg"><strong>{{ exercise.estimated1RmKg }}</strong> kg e1RM</span>
          </div>
        </header>

        <div v-if="isActive && exercise.deviationType !== 'SKIPPED'" class="exercise-actions">
          <el-button type="primary" size="small" @click="openAddSet(exercise)">
            <el-icon><Plus /></el-icon>
            添加一组
          </el-button>
          <el-button size="small" @click="openReplace(exercise)">替换动作</el-button>
          <el-button
            size="small"
            :disabled="exercise.sets.length > 0"
            @click="skipExercise(exercise)"
          >
            跳过
          </el-button>
        </div>

        <el-table v-if="exercise.sets.length" :data="exercise.sets" size="small" class="set-table">
          <el-table-column label="组" prop="setNo" width="55" />
          <el-table-column label="类型" width="130">
            <template #default="{ row }">{{ setTypeText(row) }}</template>
          </el-table-column>
          <el-table-column label="重量" width="90">
            <template #default="{ row }">{{ row.weightKg != null ? `${row.weightKg} kg` : '—' }}</template>
          </el-table-column>
          <el-table-column label="次数" width="75">
            <template #default="{ row }">{{ row.reps ?? '—' }}</template>
          </el-table-column>
          <el-table-column label="时长" width="80">
            <template #default="{ row }">{{ row.durationSec != null ? `${row.durationSec}s` : '—' }}</template>
          </el-table-column>
          <el-table-column label="RIR / RPE" width="110">
            <template #default="{ row }">{{ row.rir ?? '—' }} / {{ row.rpe ?? '—' }}</template>
          </el-table-column>
          <el-table-column label="完成" width="80">
            <template #default="{ row }">
              <el-tag :type="row.isCompleted ? 'success' : 'info'" size="small">
                {{ row.isCompleted ? '已完成' : '未完成' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="note" label="备注" min-width="120" show-overflow-tooltip />
          <el-table-column v-if="isActive" label="" width="110" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" size="small" @click="openEditSet(exercise, row)">编辑</el-button>
              <el-button text type="danger" size="small" @click="removeSet(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="set-empty">
          {{ exercise.deviationType === 'SKIPPED' ? '本次训练跳过了这个动作' : '还没有组记录，点击“添加一组”开始记录' }}
        </div>
      </section>

      <div v-if="isActive && session.completedSets === 0" class="finish-hint">
        至少记录一组已完成的正式组后，才能结束本次训练。
      </div>
    </template>

    <el-dialog v-model="setDialogVisible" :title="setEditingId ? '编辑训练组' : '添加训练组'" width="540px">
      <el-form :model="setForm" label-position="top">
        <div class="form-grid-3">
          <el-form-item label="组号">
            <el-input-number v-model="setForm.setNo" :min="1" :max="99" style="width: 100%" />
          </el-form-item>
          <el-form-item label="阶段">
            <el-select v-model="setForm.phase" style="width: 100%">
              <el-option label="正式组" value="WORK" />
              <el-option label="热身组" value="WARMUP" />
            </el-select>
          </el-form-item>
          <el-form-item label="支撑侧">
            <el-select v-model="setForm.side" style="width: 100%">
              <el-option label="双侧" value="BOTH" />
              <el-option label="左侧" value="L" />
              <el-option label="右侧" value="R" />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-grid-3">
          <el-form-item label="重量（kg）">
            <el-input-number v-model="setForm.weightKg" :min="0" :max="1000" :step="0.5" style="width: 100%" />
          </el-form-item>
          <el-form-item label="次数">
            <el-input-number v-model="setForm.reps" :min="0" :max="500" style="width: 100%" />
          </el-form-item>
          <el-form-item label="时长（秒）">
            <el-input-number v-model="setForm.durationSec" :min="0" :max="7200" :step="5" style="width: 100%" />
          </el-form-item>
        </div>

        <div class="form-grid-3">
          <el-form-item label="RIR（余力次数）">
            <el-input-number v-model="setForm.rir" :min="0" :max="10" style="width: 100%" />
          </el-form-item>
          <el-form-item label="RPE（主观强度）">
            <el-input-number v-model="setForm.rpe" :min="1" :max="10" :step="0.5" style="width: 100%" />
          </el-form-item>
          <el-form-item label="组方式">
            <el-select v-model="setForm.setType" style="width: 100%">
              <el-option label="常规组" value="STRAIGHT" />
              <el-option label="递减组" value="DROP" />
              <el-option label="休息暂停" value="REST_PAUSE" />
            </el-select>
          </el-form-item>
        </div>

        <div class="switch-row">
          <el-switch v-model="setForm.isCompleted" active-text="已完成" inactive-text="未完成" />
          <el-switch v-model="setForm.isFailure" active-text="力竭组" />
        </div>
        <el-form-item label="本组备注">
          <el-input v-model="setForm.note" maxlength="255" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="setDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="setSaving" @click="saveSet">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="addExerciseVisible" title="临时添加动作" width="460px">
      <el-select v-model="addExerciseId" filterable placeholder="搜索动作" style="width: 100%">
        <el-option
          v-for="item in exerciseOptions"
          :key="item.id"
          :label="`${item.name} · ${item.equipmentName || '自重'}`"
          :value="item.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="addExerciseVisible = false">取消</el-button>
        <el-button type="primary" :loading="addExerciseSaving" @click="saveAddExercise">添加</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="replaceVisible" title="替换训练动作" width="460px">
      <p class="dialog-tip">
        当前动作：{{ replaceTarget?.exerciseName }}。替换后仍保留原计划组数快照，并在记录中标记为“已替换”。
      </p>
      <el-select v-model="replaceExerciseId" filterable placeholder="搜索替换动作" style="width: 100%">
        <el-option
          v-for="item in exerciseOptions"
          :key="item.id"
          :label="`${item.name} · ${item.equipmentName || '自重'}`"
          :value="item.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="replaceVisible = false">取消</el-button>
        <el-button type="primary" :loading="replaceSaving" @click="saveReplace">替换</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="finishVisible" title="结束本次训练" width="480px">
      <el-form :model="finishForm" label-position="top">
        <div class="form-grid">
          <el-form-item label="整体 RPE">
            <el-input-number v-model="finishForm.overallRpe" :min="1" :max="10" :step="0.5" style="width: 100%" />
          </el-form-item>
          <el-form-item label="训练感受">
            <el-select v-model="finishForm.feeling" style="width: 100%">
              <el-option v-for="item in feelingOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="训练备注">
          <el-input v-model="finishForm.note" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="finishVisible = false">继续训练</el-button>
        <el-button type="primary" :loading="finishSaving" @click="saveFinish">完成训练</el-button>
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
  margin-bottom: 18px;
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
  margin: 7px 0 0;
  color: #7a8496;
  font-size: 13px;
}

.head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.summary-card {
  display: flex;
  flex-direction: column;
  padding: 18px 20px;
}

.summary-card span {
  color: #8b95a7;
  font-size: 12px;
}

.summary-card strong {
  margin-top: 7px;
  font-size: 27px;
  line-height: 1;
}

.summary-card small {
  margin-top: 7px;
  color: #9aa4b5;
  font-size: 10px;
}

.exercise-card {
  padding: 20px 22px;
  margin-bottom: 14px;
}

.exercise-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
}

.exercise-title {
  display: flex;
  align-items: center;
  gap: 9px;
}

.exercise-title h2 {
  margin: 0;
  font-size: 18px;
}

.exercise-head p {
  margin: 6px 0 0;
  color: #8b95a7;
  font-size: 12px;
}

.exercise-stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 14px;
  color: #8b95a7;
  font-size: 11px;
}

.exercise-stats strong {
  color: #374151;
  font-size: 14px;
}

.exercise-actions {
  display: flex;
  gap: 8px;
  padding: 10px 0 14px;
  border-top: 1px solid #eef1f6;
}

.set-table {
  border-radius: 10px;
}

.set-empty {
  padding: 28px;
  border: 1px dashed #dce3ef;
  border-radius: 10px;
  color: #8b95a7;
  font-size: 12px;
  text-align: center;
}

.finish-hint {
  color: #8b95a7;
  font-size: 12px;
  text-align: right;
}

.form-grid,
.form-grid-3 {
  display: grid;
  gap: 14px;
}

.form-grid {
  grid-template-columns: 1fr 1fr;
}

.form-grid-3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.switch-row {
  display: flex;
  gap: 24px;
  margin: 4px 0 18px;
}

.dialog-tip {
  margin: 0 0 18px;
  color: #7a8496;
  font-size: 12px;
  line-height: 1.7;
}

@media (max-width: 1050px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 780px) {
  .detail-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .form-grid,
  .form-grid-3 {
    grid-template-columns: 1fr;
  }
}
</style>
