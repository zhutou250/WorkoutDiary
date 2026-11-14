<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Calendar, CircleCheck, DataLine, Timer } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { listPlans } from '../api/plans'
import { getActiveSession, listSessions } from '../api/sessions'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const activeSession = ref(null)
const recentSessions = ref([])
const totalSessions = ref(0)
const planCount = ref(0)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

function dateString(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const today = dateString(new Date())
const monday = new Date()
const weekdayOffset = (monday.getDay() + 6) % 7
monday.setDate(monday.getDate() - weekdayOffset)
const weekStart = dateString(monday)

const weeklyCompleted = computed(() =>
  recentSessions.value.filter(
    (item) => item.status === 'COMPLETED' && item.sessionDate >= weekStart
  )
)
const weekCount = computed(() =>
  new Set(weeklyCompleted.value.map((item) => item.sessionDate)).size
)
const weekVolume = computed(() =>
  weeklyCompleted.value.reduce((sum, item) => sum + Number(item.volumeKg || 0), 0)
)
const trainedToday = computed(() =>
  weeklyCompleted.value.some((item) => item.sessionDate === today)
)
const heroText = computed(() => {
  if (activeSession.value) {
    return `当前正在训练：${activeSession.value.planDayTitleSnapshot}，上次记录到第 ${activeSession.value.completedSets} 组。`
  }
  if (trainedToday.value) {
    return '今天已经完成训练，可以回看本次记录或开始恢复。'
  }
  return '今天还没有训练记录。从计划选择一个训练日，开始逐组记录。'
})
const stats = computed(() => [
  { label: '本周训练', value: String(weekCount.value), unit: '次', icon: Calendar, color: '#3f7cff' },
  { label: '本周训练量', value: weekVolume.value.toLocaleString('zh-CN'), unit: 'kg', icon: DataLine, color: '#7c5cff' },
  { label: '训练计划', value: String(planCount.value), unit: '份', icon: CircleCheck, color: '#18a058' },
  { label: '累计训练', value: String(totalSessions.value), unit: '次', icon: Timer, color: '#f0a020' }
])

async function loadDashboard() {
  loading.value = true
  try {
    const [sessionsResult, plansResult, activeResult] = await Promise.allSettled([
      listSessions({ page: 1, size: 100 }),
      listPlans(),
      getActiveSession()
    ])
    if (sessionsResult.status === 'fulfilled') {
      recentSessions.value = sessionsResult.value.records
      totalSessions.value = sessionsResult.value.total
    }
    if (plansResult.status === 'fulfilled') {
      planCount.value = plansResult.value.length
    }
    if (activeResult.status === 'fulfilled') {
      activeSession.value = activeResult.value || null
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <div v-loading="loading" class="page">
    <section class="hero card">
      <div>
        <div class="eyebrow">TODAY</div>
        <h1>{{ greeting }}，{{ auth.user?.nickname || auth.user?.username || '训练者' }}</h1>
        <p>{{ heroText }}</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="router.push('/plans')">
            <el-icon><Calendar /></el-icon>
            查看训练计划
          </el-button>
          <el-button size="large" @click="router.push('/exercises')">
            浏览动作库
          </el-button>
        </div>
      </div>
      <div class="hero-badge">
        <div class="badge-ring">
          <span>{{ weekCount }}</span>
          <small>本周训练</small>
        </div>
      </div>
    </section>

    <section class="stats-grid">
      <div v-for="item in stats" :key="item.label" class="stat-card card">
        <div class="stat-icon" :style="{ background: `${item.color}14`, color: item.color }">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value">
            {{ item.value }}<span>{{ item.unit }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="content-grid">
      <div class="card panel">
        <div class="panel-head">
          <div>
            <h3>当前训练</h3>
            <p>进行中的训练可以随时继续。</p>
          </div>
          <el-button text type="primary" @click="router.push('/plans')">去训练计划</el-button>
        </div>
        <div v-if="activeSession" class="active-summary">
          <div>
            <strong>{{ activeSession.planNameSnapshot }} · {{ activeSession.planDayTitleSnapshot }}</strong>
            <span>已记录 {{ activeSession.completedSets }} 组，训练量 {{ Number(activeSession.volumeKg || 0).toLocaleString('zh-CN') }} kg</span>
          </div>
          <el-button type="primary" @click="router.push(`/sessions/${activeSession.id}`)">继续训练</el-button>
        </div>
        <el-empty v-else description="还没有进行中的训练" :image-size="80" />
      </div>

      <div class="card panel">
        <div class="panel-head">
          <div>
            <h3>最近训练</h3>
            <p>完成训练后，可以在这里快速回看。</p>
          </div>
          <el-button text type="primary" @click="router.push('/sessions')">全部记录</el-button>
        </div>
        <div v-if="recentSessions.length" class="recent-list">
          <div
            v-for="item in recentSessions.slice(0, 4)"
            :key="item.id"
            class="recent-row"
            @click="router.push(`/sessions/${item.id}`)"
          >
            <div class="recent-date">{{ item.sessionDate?.slice(5) }}</div>
            <div class="recent-main">
              <strong>{{ item.planDayTitleSnapshot || '自由训练' }}</strong>
              <span>{{ item.planNameSnapshot || '未关联计划' }}</span>
            </div>
            <div class="recent-volume">{{ Number(item.volumeKg || 0).toLocaleString('zh-CN') }} kg</div>
          </div>
        </div>
        <el-empty v-else description="还没有训练记录" :image-size="80" />
      </div>
    </section>

    <section class="card roadmap">
      <div class="panel-head">
        <div>
          <h3>当前开发进度</h3>
          <p>按“先闭环、后增强”的顺序推进。</p>
        </div>
      </div>
      <div class="roadmap-list">
        <span class="done">登录与鉴权</span>
        <span class="done">动作库</span>
        <span class="done">训练计划</span>
        <span class="done">开始训练</span>
        <span class="done">逐组记录</span>
        <span>数据统计</span>
        <span>规则建议</span>
      </div>
    </section>
  </div>
</template>

<style scoped>
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 34px 38px;
  overflow: hidden;
  background:
    radial-gradient(circle at 88% 20%, rgba(79, 140, 255, 0.16), transparent 32%),
    linear-gradient(120deg, #ffffff 0%, #f7f9ff 100%);
}

.eyebrow {
  color: #3f7cff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 2px;
}

.hero h1 {
  margin: 10px 0 8px;
  font-size: 30px;
  letter-spacing: 0.5px;
}

.hero p {
  margin: 0;
  color: #7a8496;
  font-size: 14px;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}

.hero-actions :deep(.el-button--primary) {
  border: none;
  background: linear-gradient(90deg, #3f7cff, #6e5bff);
}

.hero-badge {
  flex-shrink: 0;
}

.badge-ring {
  display: grid;
  width: 132px;
  height: 132px;
  place-items: center;
  align-content: center;
  border: 8px solid #e8eeff;
  border-top-color: #4f8cff;
  border-radius: 50%;
  background: #fff;
}

.badge-ring span {
  font-size: 34px;
  font-weight: 800;
  line-height: 1;
}

.badge-ring small {
  margin-top: 6px;
  color: #8b95a7;
  font-size: 11px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin: 18px 0;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
}

.stat-icon {
  display: grid;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  place-items: center;
  border-radius: 12px;
  font-size: 20px;
}

.stat-label {
  color: #8b95a7;
  font-size: 12px;
}

.stat-value {
  margin-top: 4px;
  font-size: 24px;
  font-weight: 750;
}

.stat-value span {
  margin-left: 4px;
  color: #8b95a7;
  font-size: 12px;
  font-weight: 400;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.panel {
  padding: 22px 24px 12px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-head h3 {
  margin: 0;
  font-size: 16px;
}

.panel-head p {
  margin: 6px 0 0;
  color: #8b95a7;
  font-size: 12px;
}

.active-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 4px;
}

.active-summary strong {
  display: block;
  font-size: 15px;
}

.active-summary span {
  display: block;
  margin-top: 7px;
  color: #8b95a7;
  font-size: 12px;
}

.recent-list {
  padding: 4px 0 8px;
}

.recent-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 6px;
  border-bottom: 1px solid #eef1f6;
  cursor: pointer;
  transition: background 0.2s;
}

.recent-row:hover {
  background: #f8faff;
}

.recent-row:last-child {
  border-bottom: none;
}

.recent-date {
  width: 44px;
  flex-shrink: 0;
  color: #3f7cff;
  font-size: 12px;
  font-weight: 700;
}

.recent-main {
  min-width: 0;
  flex: 1;
}

.recent-main strong {
  display: block;
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-main span {
  display: block;
  margin-top: 3px;
  color: #8b95a7;
  font-size: 11px;
}

.recent-volume {
  flex-shrink: 0;
  color: #5b6b85;
  font-size: 12px;
}

.roadmap {
  margin-top: 16px;
  padding: 22px 24px 24px;
}

.roadmap-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.roadmap-list span {
  padding: 7px 14px;
  border: 1px dashed #d8deea;
  border-radius: 999px;
  color: #8b95a7;
  font-size: 12px;
}

.roadmap-list span.done {
  border-style: solid;
  border-color: #cfe0ff;
  background: #eef4ff;
  color: #3f7cff;
  font-weight: 600;
}

@media (max-width: 1100px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 800px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
