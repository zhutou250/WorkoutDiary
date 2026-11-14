<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { getCalendar, getExerciseTrend, getOverview, getWeightTrend } from '../api/stats'

const loading = ref(false)
const overview = ref(null)
const weightTrend = ref(null)
const calendar = ref(null)
const selectedExerciseId = ref(null)
const exerciseTrend = ref(null)

const weeklyChartEl = ref()
const muscleChartEl = ref()
const weightChartEl = ref()
const trendChartEl = ref()
const charts = []

const leadingBlanks = computed(() => {
  const first = calendar.value?.days?.[0]?.date
  if (!first) return 0
  return (new Date(`${first}T00:00:00`).getDay() + 6) % 7
})

const now = new Date()
const calendarState = reactive({
  year: now.getFullYear(),
  month: now.getMonth() + 1
})

function numberText(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function completionText() {
  const value = overview.value?.completionPct
  return value == null ? '--' : `${value}%`
}

async function loadOverview() {
  overview.value = await getOverview(12)
  if (overview.value.exercises.length && !selectedExerciseId.value) {
    selectedExerciseId.value = overview.value.exercises[0].exerciseId
  }
  await nextTick()
  renderWeeklyChart()
  renderMuscleChart()
  await loadExerciseTrend()
}

async function loadWeight() {
  weightTrend.value = await getWeightTrend()
  await nextTick()
  renderWeightChart()
}

async function loadCalendar() {
  calendar.value = await getCalendar(calendarState.year, calendarState.month)
}

async function loadExerciseTrend() {
  if (!selectedExerciseId.value) {
    exerciseTrend.value = null
    return
  }
  exerciseTrend.value = await getExerciseTrend(selectedExerciseId.value)
  await nextTick()
  renderTrendChart()
}

function renderWeeklyChart() {
  if (!weeklyChartEl.value) return
  const chart = echarts.init(weeklyChartEl.value)
  charts.push(chart)
  const weeks = overview.value?.weeks || []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['训练量(kg)', '训练次数'] },
    grid: { left: 50, right: 40, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: weeks.map((item) => item.weekStart.slice(5)) },
    yAxis: [
      { type: 'value', name: 'kg' },
      { type: 'value', name: '次', minInterval: 1 }
    ],
    series: [
      {
        name: '训练量(kg)',
        type: 'bar',
        data: weeks.map((item) => item.volumeKg),
        itemStyle: { color: '#4f8cff', borderRadius: [4, 4, 0, 0] }
      },
      {
        name: '训练次数',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: weeks.map((item) => item.sessionCount),
        itemStyle: { color: '#7c5cff' }
      }
    ]
  })
}

function renderMuscleChart() {
  if (!muscleChartEl.value) return
  const chart = echarts.init(muscleChartEl.value)
  charts.push(chart)
  const muscles = [...(overview.value?.muscles || [])].sort((a, b) => a.effectiveSets - b.effectiveSets)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 80, right: 30, top: 20, bottom: 30 },
    xAxis: { type: 'value', name: '有效组数' },
    yAxis: { type: 'category', data: muscles.map((item) => item.name) },
    series: [
      {
        type: 'bar',
        data: muscles.map((item) => item.effectiveSets),
        itemStyle: { color: '#18a058', borderRadius: [0, 4, 4, 0] }
      }
    ]
  })
}

function renderWeightChart() {
  if (!weightChartEl.value) return
  const chart = echarts.init(weightChartEl.value)
  charts.push(chart)
  const points = weightTrend.value?.points || []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['体重', '7日平均'] },
    grid: { left: 50, right: 30, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: points.map((item) => item.date) },
    yAxis: { type: 'value', name: 'kg', scale: true },
    series: [
      { name: '体重', type: 'line', data: points.map((item) => item.weightKg), itemStyle: { color: '#4f8cff' } },
      { name: '7日平均', type: 'line', smooth: true, data: points.map((item) => item.movingAvg7), itemStyle: { color: '#f0a020' } }
    ]
  })
}

function renderTrendChart() {
  if (!trendChartEl.value) return
  const chart = echarts.init(trendChartEl.value)
  charts.push(chart)
  const points = exerciseTrend.value?.points || []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['e1RM(kg)', '训练量(kg)'] },
    grid: { left: 50, right: 40, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: points.map((item) => item.date) },
    yAxis: [
      { type: 'value', name: 'kg', scale: true },
      { type: 'value', name: 'kg' }
    ],
    series: [
      { name: 'e1RM(kg)', type: 'line', data: points.map((item) => item.e1rmKg), itemStyle: { color: '#7c5cff' } },
      { name: '训练量(kg)', type: 'bar', yAxisIndex: 1, data: points.map((item) => item.volumeKg), itemStyle: { color: '#cfe0ff', borderRadius: [4, 4, 0, 0] } }
    ]
  })
}

function prevMonth() {
  if (calendarState.month === 1) {
    calendarState.year -= 1
    calendarState.month = 12
  } else {
    calendarState.month -= 1
  }
  loadCalendar()
}

function nextMonth() {
  if (calendarState.month === 12) {
    calendarState.year += 1
    calendarState.month = 1
  } else {
    calendarState.month += 1
  }
  loadCalendar()
}

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([loadOverview(), loadWeight(), loadCalendar()])
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  charts.forEach((chart) => chart.dispose())
})
</script>

<template>
  <div v-loading="loading" class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">数据统计</h1>
        <p class="page-subtitle">所有数字由后端统一计算，只统计已完成的正式组。</p>
      </div>
    </div>

    <template v-if="overview">
      <section class="summary-grid">
        <div class="summary-card card">
          <span>总训练量</span>
          <strong>{{ numberText(overview.totalVolumeKg) }}</strong>
          <small>kg</small>
        </div>
        <div class="summary-card card">
          <span>完成训练</span>
          <strong>{{ overview.completedSessions }}</strong>
          <small>次</small>
        </div>
        <div class="summary-card card">
          <span>完成组数</span>
          <strong>{{ overview.completedSets }}</strong>
          <small>共计划 {{ overview.plannedSets }} 组</small>
        </div>
        <div class="summary-card card">
          <span>计划完成度</span>
          <strong>{{ completionText() }}</strong>
          <small>正式组口径</small>
        </div>
      </section>

      <section class="chart-grid">
        <div class="chart-card card">
          <h3>每周训练量与频率</h3>
          <div ref="weeklyChartEl" class="chart"></div>
        </div>
        <div class="chart-card card">
          <h3>肌肉部位有效组数</h3>
          <div ref="muscleChartEl" class="chart"></div>
        </div>
      </section>

      <section class="chart-grid">
        <div class="chart-card card">
          <div class="chart-head">
            <h3>动作力量趋势</h3>
            <el-select v-model="selectedExerciseId" style="width: 200px" @change="loadExerciseTrend">
              <el-option
                v-for="item in overview.exercises"
                :key="item.exerciseId"
                :label="item.exerciseName"
                :value="item.exerciseId"
              />
            </el-select>
          </div>
          <div v-if="exerciseTrend?.points?.length" ref="trendChartEl" class="chart"></div>
          <el-empty v-else description="该动作还没有足够记录" :image-size="80" />
        </div>
        <div class="chart-card card">
          <h3>体重趋势</h3>
          <div v-if="weightTrend?.points?.length" ref="weightChartEl" class="chart"></div>
          <el-empty v-else description="还没有体重记录" :image-size="80" />
        </div>
      </section>

      <section class="calendar-card card">
        <div class="chart-head">
          <h3>训练日历</h3>
          <div class="month-nav">
            <el-button text @click="prevMonth"><el-icon><ArrowLeft /></el-icon></el-button>
            <span>{{ calendarState.year }} 年 {{ calendarState.month }} 月</span>
            <el-button text @click="nextMonth"><el-icon><ArrowRight /></el-icon></el-button>
          </div>
        </div>
        <div class="weekdays">
          <span v-for="name in ['一', '二', '三', '四', '五', '六', '日']" :key="name">{{ name }}</span>
        </div>
        <div class="calendar-days">
          <span v-for="index in leadingBlanks" :key="`blank-${index}`" class="calendar-day blank"></span>
          <div
            v-for="day in calendar?.days || []"
            :key="day.date"
            class="calendar-day"
            :class="{ trained: day.trained }"
          >
            <span class="day-num">{{ Number(day.date.slice(8)) }}</span>
            <span v-if="day.trained" class="day-dot"></span>
          </div>
        </div>
      </section>
    </template>
    <el-empty v-else-if="!loading" description="还没有训练数据，先完成一次训练" />
  </div>
</template>

<style scoped>
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
  font-size: 26px;
  line-height: 1;
}

.summary-card small {
  margin-top: 7px;
  color: #9aa4b5;
  font-size: 10px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.chart-card {
  padding: 20px 22px;
}

.chart-card h3 {
  margin: 0 0 12px;
  font-size: 15px;
}

.chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.chart-head h3 {
  margin: 0;
}

.chart {
  width: 100%;
  height: 300px;
}

.calendar-card {
  padding: 20px 22px;
}

.month-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #5b6b85;
  font-size: 13px;
}

.weekdays,
.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
}

.weekdays {
  margin-top: 16px;
}

.weekdays span {
  color: #8b95a7;
  font-size: 11px;
  text-align: center;
}

.calendar-days {
  margin-top: 8px;
}

.calendar-day {
  position: relative;
  display: grid;
  min-height: 42px;
  place-items: center;
  border-radius: 9px;
  background: #f7f9fc;
}

.calendar-day.trained {
  background: #eef4ff;
}

.calendar-day.blank {
  background: transparent;
}

.day-num {
  color: #5b6b85;
  font-size: 12px;
}

.day-dot {
  position: absolute;
  bottom: 6px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #4f8cff;
}

@media (max-width: 1050px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
