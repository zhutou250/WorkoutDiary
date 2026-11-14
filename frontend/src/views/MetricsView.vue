<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteMetric, listMetrics, upsertMetric } from '../api/metrics'

const loading = ref(false)
const records = ref([])
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({
  recordDate: '',
  weightKg: null,
  bodyFatPct: null,
  waistCm: null,
  note: ''
})

const rules = {
  recordDate: [{ required: true, message: '请选择记录日期', trigger: 'change' }]
}

const latest = computed(() => records.value[0] || null)

function todayString() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

async function load() {
  loading.value = true
  try {
    records.value = await listMetrics()
  } catch {
    // 请求错误已统一提示
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, {
    recordDate: todayString(),
    weightKg: latest.value?.weightKg ?? null,
    bodyFatPct: latest.value?.bodyFatPct ?? null,
    waistCm: latest.value?.waistCm ?? null,
    note: ''
  })
  dialogVisible.value = true
}

function openEdit(item) {
  Object.assign(form, {
    recordDate: item.recordDate,
    weightKg: item.weightKg,
    bodyFatPct: item.bodyFatPct,
    waistCm: item.waistCm,
    note: item.note || ''
  })
  dialogVisible.value = true
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await upsertMetric({ ...form })
    ElMessage.success('身体数据已保存')
    dialogVisible.value = false
    await load()
  } catch {
    // 请求错误已统一提示
  } finally {
    saving.value = false
  }
}

async function remove(item) {
  try {
    await ElMessageBox.confirm(`删除 ${item.recordDate} 的记录吗？`, '删除记录', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteMetric(item.id)
    ElMessage.success('记录已删除')
    await load()
  } catch {
    // 用户取消或请求失败
  }
}

function valueText(value, suffix) {
  return value == null ? '—' : `${value} ${suffix}`
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">身体数据</h1>
        <p class="page-subtitle">记录体重、体脂率和围度，趋势图会在数据统计中展示。</p>
      </div>
      <el-button type="primary" size="large" @click="openCreate">
        <el-icon><Plus /></el-icon>
        记录身体数据
      </el-button>
    </div>

    <section class="summary-grid">
      <div class="summary-card card">
        <span>最新体重</span>
        <strong>{{ valueText(latest?.weightKg, 'kg') }}</strong>
      </div>
      <div class="summary-card card">
        <span>最新体脂率</span>
        <strong>{{ valueText(latest?.bodyFatPct, '%') }}</strong>
      </div>
      <div class="summary-card card">
        <span>最新腰围</span>
        <strong>{{ valueText(latest?.waistCm, 'cm') }}</strong>
      </div>
      <div class="summary-card card">
        <span>记录次数</span>
        <strong>{{ records.length }}</strong>
      </div>
    </section>

    <div class="card table-card">
      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="recordDate" label="日期" width="140" />
        <el-table-column label="体重" width="120">
          <template #default="{ row }">{{ valueText(row.weightKg, 'kg') }}</template>
        </el-table-column>
        <el-table-column label="体脂率" width="120">
          <template #default="{ row }">{{ valueText(row.bodyFatPct, '%') }}</template>
        </el-table-column>
        <el-table-column label="腰围" width="120">
          <template #default="{ row }">{{ valueText(row.waistCm, 'cm') }}</template>
        </el-table-column>
        <el-table-column prop="note" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column label="" width="130" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button text type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && records.length === 0" description="还没有身体数据记录" :image-size="100" />
    </div>

    <el-dialog v-model="dialogVisible" title="记录身体数据" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="记录日期" prop="recordDate">
          <el-date-picker v-model="form.recordDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="体重（kg）">
            <el-input-number v-model="form.weightKg" :min="0" :max="500" :step="0.1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="体脂率（%）">
            <el-input-number v-model="form.bodyFatPct" :min="0" :max="100" :step="0.1" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item label="腰围（cm）">
          <el-input-number v-model="form.waistCm" :min="0" :max="300" :step="0.5" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.note" maxlength="255" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
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
}

.table-card {
  padding: 8px 16px 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

@media (max-width: 900px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
