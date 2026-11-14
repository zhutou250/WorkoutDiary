<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getExerciseMeta } from '../api/exercise'
import {
  changeExerciseStatus,
  createAdminExercise,
  getPlatform,
  listAdminExercises,
  listAuditLogs,
  listUsers,
  updateAdminExercise,
  updateUserStatus
} from '../api/admin'

const activeTab = ref('platform')
const loading = ref(false)

const platform = ref(null)
const users = ref([])
const userTotal = ref(0)
const userQuery = reactive({ keyword: '', page: 1, size: 10 })

const exercises = ref([])
const exerciseTotal = ref(0)
const exerciseQuery = reactive({ keyword: '', bodyPart: '', status: '', page: 1, size: 10 })
const bodyParts = ref([])
const equipmentOptions = ref([])
const exerciseDialogVisible = ref(false)
const exerciseSaving = ref(false)
const exerciseEditingId = ref(null)
const exerciseForm = reactive({
  name: '',
  bodyPart: 'CHEST',
  equipmentId: null,
  category: 'STRENGTH',
  mode: 'REPS',
  description: '',
  instructions: [],
  instructionsText: '',
  mediaUrl: ''
})

const auditLogs = ref([])

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

const categoryLabels = { STRENGTH: '力量', CARDIO: '有氧', MOBILITY: '灵活性' }
const modeLabels = { REPS: '次数', TIME: '计时', CARDIO: '有氧' }

async function loadPlatform() {
  platform.value = await getPlatform()
}

async function loadUsers() {
  const data = await listUsers({ ...userQuery })
  users.value = data.records
  userTotal.value = data.total
}

async function loadExercises() {
  const data = await listAdminExercises({ ...exerciseQuery })
  exercises.value = data.records
  exerciseTotal.value = data.total
}

async function loadMeta() {
  const data = await getExerciseMeta()
  bodyParts.value = data.bodyParts || []
  equipmentOptions.value = data.equipment || []
}

async function loadAuditLogs() {
  auditLogs.value = await listAuditLogs(50)
}

async function handleTabChange() {
  loading.value = true
  try {
    if (activeTab.value === 'platform') await loadPlatform()
    if (activeTab.value === 'users') await loadUsers()
    if (activeTab.value === 'exercises') await Promise.all([loadExercises(), loadMeta()])
    if (activeTab.value === 'logs') await loadAuditLogs()
  } catch {
    // 请求错误已统一提示
  } finally {
    loading.value = false
  }
}

async function toggleUserStatus(user) {
  const status = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await updateUserStatus(user.id, { status })
  ElMessage.success(status === 'ACTIVE' ? '账号已启用' : '账号已禁用')
  await loadUsers()
}

function searchUsers() {
  userQuery.page = 1
  loadUsers()
}

function searchExercises() {
  exerciseQuery.page = 1
  loadExercises()
}

function openCreateExercise() {
  exerciseEditingId.value = null
  Object.assign(exerciseForm, {
    name: '',
    bodyPart: 'CHEST',
    equipmentId: null,
    category: 'STRENGTH',
    mode: 'REPS',
    description: '',
    instructions: [],
    instructionsText: '',
    mediaUrl: ''
  })
  exerciseDialogVisible.value = true
}

function openEditExercise(item) {
  exerciseEditingId.value = item.id
  let instructions = []
  try {
    instructions = typeof item.instructions === 'string' ? JSON.parse(item.instructions) : (item.instructions || [])
  } catch {
    instructions = []
  }
  Object.assign(exerciseForm, {
    name: item.name,
    bodyPart: item.bodyPart,
    equipmentId: item.equipmentId,
    category: item.category,
    mode: item.mode,
    description: item.description || '',
    instructions,
    instructionsText: instructions.join('\n'),
    mediaUrl: item.mediaUrl || ''
  })
  exerciseDialogVisible.value = true
}

async function saveExercise() {
  if (!exerciseForm.name.trim()) {
    ElMessage.warning('请输入动作名称')
    return
  }
  const instructions = String(exerciseForm.instructionsText || '')
    .split('\n')
    .map((item) => item.trim())
    .filter(Boolean)
  const payload = { ...exerciseForm, name: exerciseForm.name.trim(), instructions }
  exerciseSaving.value = true
  try {
    if (exerciseEditingId.value) {
      await updateAdminExercise(exerciseEditingId.value, payload)
    } else {
      await createAdminExercise(payload)
    }
    ElMessage.success('动作已保存')
    exerciseDialogVisible.value = false
    await loadExercises()
  } catch {
    // 请求错误已统一提示
  } finally {
    exerciseSaving.value = false
  }
}

async function toggleExerciseStatus(item) {
  const status = item.status === 'ACTIVE' ? 'ARCHIVED' : 'ACTIVE'
  await changeExerciseStatus(item.id, status)
  ElMessage.success(status === 'ACTIVE' ? '动作已恢复' : '动作已归档')
  await loadExercises()
}

function actionLabel(action) {
  const map = {
    CREATE_EXERCISE: '新增动作',
    UPDATE_EXERCISE: '修改动作',
    CHANGE_EXERCISE_STATUS: '归档/恢复动作',
    UPDATE_USER_STATUS: '修改用户状态'
  }
  return map[action] || action
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '—'
}

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([loadPlatform(), loadUsers(), loadExercises(), loadMeta(), loadAuditLogs()])
  } catch {
    // 请求错误已统一提示
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">管理后台</h1>
        <p class="page-subtitle">用户管理、动作库维护和平台数据概览。</p>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="平台概览" name="platform">
        <section v-if="platform" class="platform-grid">
          <div class="platform-card card">
            <span>注册用户</span>
            <strong>{{ platform.userCount }}</strong>
          </div>
          <div class="platform-card card">
            <span>动作库动作</span>
            <strong>{{ platform.exerciseCount }}</strong>
          </div>
          <div class="platform-card card">
            <span>训练计划</span>
            <strong>{{ platform.planCount }}</strong>
          </div>
          <div class="platform-card card">
            <span>训练记录</span>
            <strong>{{ platform.sessionCount }}</strong>
          </div>
          <div class="platform-card card">
            <span>生成建议</span>
            <strong>{{ platform.adviceCount }}</strong>
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="用户管理" name="users">
        <div class="filter-card card">
          <el-input v-model="userQuery.keyword" placeholder="搜索用户名或昵称" clearable style="width: 240px" @keyup.enter="searchUsers" />
          <el-button type="primary" @click="searchUsers">查询</el-button>
        </div>
        <div class="table-card card">
          <el-table v-loading="loading" :data="users" stripe>
            <el-table-column prop="username" label="用户名" min-width="140" />
            <el-table-column prop="nickname" label="昵称" min-width="120" />
            <el-table-column label="角色" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.role === 'ADMIN' ? 'danger' : 'info'">
                  {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'">
                  {{ row.status === 'ACTIVE' ? '正常' : '已禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="注册时间" width="150">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="" width="110" fixed="right">
              <template #default="{ row }">
                <el-button text :type="row.status === 'ACTIVE' ? 'danger' : 'primary'" @click="toggleUserStatus(row)">
                  {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="动作库管理" name="exercises">
        <div class="filter-card card">
          <el-input v-model="exerciseQuery.keyword" placeholder="搜索动作名称" clearable style="width: 220px" @keyup.enter="searchExercises" />
          <el-select v-model="exerciseQuery.bodyPart" placeholder="全部部位" clearable style="width: 140px">
            <el-option v-for="item in bodyParts" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="exerciseQuery.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
          <el-button type="primary" @click="searchExercises">查询</el-button>
          <el-button @click="openCreateExercise">新增动作</el-button>
        </div>
        <div class="table-card card">
          <el-table v-loading="loading" :data="exercises" stripe>
            <el-table-column prop="name" label="动作名称" min-width="150" />
            <el-table-column label="部位" width="90">
              <template #default="{ row }">{{ bodyPartLabels[row.bodyPart] || row.bodyPart }}</template>
            </el-table-column>
            <el-table-column label="器械" width="110">
              <template #default="{ row }">{{ row.equipmentName || '—' }}</template>
            </el-table-column>
            <el-table-column label="类型" width="90">
              <template #default="{ row }">{{ categoryLabels[row.category] || row.category }}</template>
            </el-table-column>
            <el-table-column label="记录方式" width="100">
              <template #default="{ row }">{{ modeLabels[row.mode] || row.mode }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'">
                  {{ row.status === 'ACTIVE' ? '正常' : '已归档' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="" width="150" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click="openEditExercise(row)">编辑</el-button>
                <el-button text :type="row.status === 'ACTIVE' ? 'warning' : 'primary'" @click="toggleExerciseStatus(row)">
                  {{ row.status === 'ACTIVE' ? '归档' : '恢复' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="操作日志" name="logs">
        <div class="table-card card">
          <el-table :data="auditLogs" stripe>
            <el-table-column label="时间" width="180">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">{{ actionLabel(row.action) }}</template>
            </el-table-column>
            <el-table-column prop="targetType" label="目标类型" width="120" />
            <el-table-column prop="targetId" label="目标 ID" width="100" />
            <el-table-column label="详情" min-width="240">
              <template #default="{ row }">{{ row.detailJson || '—' }}</template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="exerciseDialogVisible" :title="exerciseEditingId ? '编辑动作' : '新增动作'" width="600px">
      <el-form :model="exerciseForm" label-position="top">
        <div class="form-grid">
          <el-form-item label="动作名称">
            <el-input v-model="exerciseForm.name" maxlength="100" />
          </el-form-item>
          <el-form-item label="身体部位">
            <el-select v-model="exerciseForm.bodyPart" style="width: 100%">
              <el-option v-for="item in bodyParts" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="器械">
            <el-select v-model="exerciseForm.equipmentId" clearable placeholder="可选" style="width: 100%">
              <el-option v-for="item in equipmentOptions" :key="item.value" :label="item.label" :value="Number(item.value)" />
            </el-select>
          </el-form-item>
          <el-form-item label="动作类型">
            <el-select v-model="exerciseForm.category" style="width: 100%">
              <el-option label="力量" value="STRENGTH" />
              <el-option label="有氧" value="CARDIO" />
              <el-option label="灵活性" value="MOBILITY" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="记录方式">
          <el-radio-group v-model="exerciseForm.mode">
            <el-radio-button value="REPS">次数</el-radio-button>
            <el-radio-button value="TIME">计时</el-radio-button>
            <el-radio-button value="CARDIO">有氧</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="动作说明">
          <el-input v-model="exerciseForm.description" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="动作步骤（每行一条）">
          <el-input v-model="exerciseForm.instructionsText" type="textarea" :rows="4" placeholder="每行一条动作步骤" />
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
.platform-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.platform-card {
  display: flex;
  flex-direction: column;
  padding: 22px;
}

.platform-card span {
  color: #8b95a7;
  font-size: 12px;
}

.platform-card strong {
  margin-top: 8px;
  font-size: 30px;
}

.filter-card {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px 18px;
  margin-bottom: 14px;
}

.table-card {
  padding: 8px 16px 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

@media (max-width: 1100px) {
  .platform-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
