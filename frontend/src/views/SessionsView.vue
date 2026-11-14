<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getActiveSession, listSessions, startFreeSession } from '../api/sessions'

const router = useRouter()
const loading = ref(false)
const active = ref(null)
const records = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })

const statusMeta = {
  IN_PROGRESS: { label: '进行中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' }
}

function formatDate(value) {
  return value || '—'
}

function formatTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(5, 16)
}

function durationText(item) {
  if (!item.startedAt || !item.endedAt) return item.status === 'IN_PROGRESS' ? '训练中' : '—'
  const start = new Date(item.startedAt)
  const end = new Date(item.endedAt)
  const minutes = Math.max(0, Math.round((end - start) / 60000))
  return minutes >= 60 ? `${Math.floor(minutes / 60)} 小时 ${minutes % 60} 分` : `${minutes} 分钟`
}

function volumeText(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

async function loadActive() {
  try {
    active.value = await getActiveSession()
  } catch {
    active.value = null
  }
}

async function loadRecords() {
  loading.value = true
  try {
    const data = await listSessions({ page: query.page, size: query.size })
    records.value = data.records
    total.value = data.total
  } catch {
    // 请求错误已统一提示
  } finally {
    loading.value = false
  }
}

function changePage(page) {
  query.page = page
  loadRecords()
}

async function startFree() {
  try {
    const session = await startFreeSession()
    router.push(`/sessions/${session.id}`)
  } catch {
    // 请求错误已统一提示
  }
}

onMounted(async () => {
  await Promise.all([loadActive(), loadRecords()])
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">训练记录</h1>
        <p class="page-subtitle">逐组保存真实训练数据，训练结束后可以回看训练量和动作表现。</p>
      </div>
      <div class="head-actions">
        <el-button size="large" @click="startFree">
          <el-icon><EditPen /></el-icon>
          自由训练
        </el-button>
        <el-button type="primary" size="large" @click="router.push('/plans')">
          <el-icon><VideoPlay /></el-icon>
          从计划开始训练
        </el-button>
      </div>
    </div>

    <section v-if="active" class="active-card card">
      <div class="active-pulse"></div>
      <div>
        <span class="active-label">进行中的训练</span>
        <h2>{{ active.planNameSnapshot }} · {{ active.planDayTitleSnapshot }}</h2>
        <p>
          已记录 {{ active.completedSets }} 组，训练量 {{ volumeText(active.volumeKg) }} kg
        </p>
      </div>
      <el-button type="primary" size="large" @click="router.push(`/sessions/${active.id}`)">
        继续训练
        <el-icon><ArrowRight /></el-icon>
      </el-button>
    </section>

    <div class="history-card card">
      <div class="history-head">
        <div>
          <h2>历史训练</h2>
          <span>共 {{ total }} 次训练</span>
        </div>
      </div>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column label="训练日期" width="130">
          <template #default="{ row }">{{ formatDate(row.sessionDate) }}</template>
        </el-table-column>
        <el-table-column label="计划 / 训练日" min-width="210">
          <template #default="{ row }">
            <div class="plan-name">{{ row.planNameSnapshot || '自由训练' }}</div>
            <div class="plan-day">{{ row.planDayTitleSnapshot || '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMeta[row.status]?.type || 'info'" size="small">
              {{ statusMeta[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="完成组数" width="110">
          <template #default="{ row }">{{ row.completedSets }} / {{ row.totalSets }}</template>
        </el-table-column>
        <el-table-column label="训练量" width="130">
          <template #default="{ row }">
            <strong>{{ volumeText(row.volumeKg) }}</strong> <span class="unit">kg</span>
          </template>
        </el-table-column>
        <el-table-column label="用时" width="130">
          <template #default="{ row }">{{ durationText(row) }}</template>
        </el-table-column>
        <el-table-column label="开始时间" width="125">
          <template #default="{ row }">{{ formatTime(row.startedAt) }}</template>
        </el-table-column>
        <el-table-column label="" width="90" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="router.push(`/sessions/${row.id}`)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!loading && records.length === 0"
        description="还没有训练记录，从训练计划选择一个训练日开始"
      >
        <el-button type="primary" @click="router.push('/plans')">去训练计划</el-button>
      </el-empty>

      <div v-if="total > query.size" class="pager">
        <el-pagination
          layout="prev, pager, next"
          :total="total"
          :page-size="query.size"
          :current-page="query.page"
          @current-change="changePage"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.active-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 22px 26px;
  margin-bottom: 16px;
  background:
    radial-gradient(circle at 90% 20%, rgba(79, 140, 255, 0.15), transparent 36%),
    linear-gradient(120deg, #f8fbff, #ffffff);
}

.head-actions {
  display: flex;
  gap: 8px;
}

.active-pulse {
  width: 12px;
  height: 12px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #3f7cff;
  box-shadow: 0 0 0 7px rgba(63, 124, 255, 0.12);
}

.active-card > div:nth-child(2) {
  flex: 1;
}

.active-label {
  color: #3f7cff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
}

.active-card h2 {
  margin: 5px 0;
  font-size: 19px;
}

.active-card p {
  margin: 0;
  color: #7a8496;
  font-size: 13px;
}

.history-card {
  padding: 22px 22px 16px;
}

.history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.history-head h2 {
  margin: 0;
  font-size: 17px;
}

.history-head span {
  display: block;
  margin-top: 5px;
  color: #8b95a7;
  font-size: 12px;
}

.plan-name {
  font-weight: 600;
}

.plan-day {
  margin-top: 3px;
  color: #8b95a7;
  font-size: 12px;
}

.unit {
  color: #8b95a7;
  font-size: 11px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

@media (max-width: 800px) {
  .active-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
