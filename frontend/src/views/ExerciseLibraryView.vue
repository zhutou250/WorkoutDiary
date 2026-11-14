<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getExerciseMeta, getExercises } from '../api/exercise'

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

const categoryLabels = {
  STRENGTH: '力量',
  CARDIO: '有氧',
  MOBILITY: '灵活性'
}

const modeLabels = {
  REPS: '次数',
  TIME: '计时',
  CARDIO: '有氧'
}

const loading = ref(false)
const records = ref([])
const total = ref(0)
const meta = reactive({ bodyParts: [], equipment: [] })
const query = reactive({
  keyword: '',
  bodyPart: '',
  equipmentId: null,
  page: 1,
  size: 10
})

async function loadMeta() {
  try {
    const data = await getExerciseMeta()
    meta.bodyParts = data.bodyParts || []
    meta.equipment = data.equipment || []
  } catch {
    // 错误提示已由请求拦截器统一处理
  }
}

async function loadData() {
  loading.value = true
  try {
    const data = await getExercises({
      keyword: query.keyword || undefined,
      bodyPart: query.bodyPart || undefined,
      equipmentId: query.equipmentId || undefined,
      page: query.page,
      size: query.size
    })
    records.value = data.records
    total.value = data.total
  } catch {
    // 错误提示已由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  loadData()
}

function reset() {
  query.keyword = ''
  query.bodyPart = ''
  query.equipmentId = null
  query.page = 1
  loadData()
}

function handlePageChange(page) {
  query.page = page
  loadData()
}

onMounted(async () => {
  await Promise.all([loadMeta(), loadData()])
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">动作库</h1>
        <p class="page-subtitle">按部位、器械和关键词查找动作，制定计划时可直接引用。</p>
      </div>
      <div class="count-chip">共 {{ total }} 个动作</div>
    </div>

    <div class="filter-card card">
      <el-input
        v-model="query.keyword"
        placeholder="搜索动作名称或说明"
        clearable
        style="width: 240px"
        @keyup.enter="search"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-select v-model="query.bodyPart" placeholder="全部部位" clearable style="width: 150px">
        <el-option
          v-for="item in meta.bodyParts"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>

      <el-select v-model="query.equipmentId" placeholder="全部器械" clearable style="width: 160px">
        <el-option
          v-for="item in meta.equipment"
          :key="item.value"
          :label="item.label"
          :value="Number(item.value)"
        />
      </el-select>

      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <div class="table-card card">
      <el-table v-loading="loading" :data="records" stripe style="width: 100%">
        <el-table-column prop="name" label="动作名称" min-width="160">
          <template #default="{ row }">
            <div class="exercise-name">
              <span class="dot"></span>
              {{ row.name }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="部位" width="100">
          <template #default="{ row }">
            {{ bodyPartLabels[row.bodyPart] || row.bodyPart }}
          </template>
        </el-table-column>
        <el-table-column label="器械" width="120">
          <template #default="{ row }">
            {{ row.equipmentName || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">
              {{ categoryLabels[row.category] || row.category }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="记录方式" width="100">
          <template #default="{ row }">
            {{ modeLabels[row.mode] || row.mode }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="动作说明" min-width="260" show-overflow-tooltip />
      </el-table>

      <div class="pager">
        <el-pagination
          layout="total, prev, pager, next"
          :total="total"
          :page-size="query.size"
          :current-page="query.page"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.count-chip {
  padding: 7px 14px;
  border: 1px solid #dbe4f3;
  border-radius: 999px;
  background: #fff;
  color: #5b6b85;
  font-size: 12px;
}

.exercise-name {
  display: flex;
  align-items: center;
  gap: 9px;
  font-weight: 600;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f8cff, #7c5cff);
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding: 18px 4px 4px;
}
</style>
