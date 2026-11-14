<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { copyPlanTemplate, createPlan, deletePlan, listPlanTemplates, listPlans } from '../api/plans'

const router = useRouter()
const loading = ref(false)
const plans = ref([])
const dialogVisible = ref(false)
const saving = ref(false)
const templates = ref([])
const templateDialogVisible = ref(false)
const templateSaving = ref(false)
const formRef = ref()

const form = reactive({
  name: '',
  goal: 'HYPERTROPHY',
  startDate: '',
  weeks: 4
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

const rules = {
  name: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  weeks: [{ required: true, message: '请输入计划周数', trigger: 'change' }]
}

function goalLabel(value) {
  return goalOptions.find((item) => item.value === value)?.label || value || '未设置'
}

function formatDate(value) {
  return value || '未设置开始日期'
}

function todayString() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

async function loadData() {
  loading.value = true
  try {
    plans.value = await listPlans()
  } catch {
    // 请求错误已统一提示
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, {
    name: '',
    goal: 'HYPERTROPHY',
    startDate: todayString(),
    weeks: 4
  })
  dialogVisible.value = true
}

async function openTemplates() {
  try {
    templates.value = await listPlanTemplates()
    templateDialogVisible.value = true
  } catch {
    // 请求错误已统一提示
  }
}

async function useTemplate(template) {
  templateSaving.value = true
  try {
    const data = await copyPlanTemplate(template.id)
    ElMessage.success('已从模板创建计划')
    templateDialogVisible.value = false
    router.push(`/plans/${data.id}`)
  } catch {
    // 请求错误已统一提示
  } finally {
    templateSaving.value = false
  }
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const data = await createPlan({ ...form })
    ElMessage.success('计划已创建，可以开始安排训练日')
    dialogVisible.value = false
    router.push(`/plans/${data.id}`)
  } catch {
    // 请求错误已统一提示
  } finally {
    saving.value = false
  }
}

async function removePlan(plan) {
  try {
    await ElMessageBox.confirm(
      `删除计划「${plan.name}」后，该计划下的训练日也会删除，历史训练记录仍会保留。`,
      '删除计划',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deletePlan(plan.id)
    ElMessage.success('计划已删除')
    await loadData()
  } catch {
    // 用户取消或请求失败
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">训练计划</h1>
        <p class="page-subtitle">把每周训练日和动作安排好，训练时可以直接从这里开始。</p>
      </div>
      <div class="head-actions">
        <el-button size="large" @click="openTemplates">
          <el-icon><CopyDocument /></el-icon>
          使用模板
        </el-button>
        <el-button type="primary" size="large" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新建计划
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="plan-area">
      <el-empty v-if="!loading && plans.length === 0" description="还没有训练计划">
        <el-button type="primary" @click="openCreate">创建第一份计划</el-button>
      </el-empty>

      <div v-else class="plan-grid">
        <article v-for="plan in plans" :key="plan.id" class="plan-card card">
          <div class="plan-top">
            <div>
              <el-tag :type="statusMeta[plan.status]?.type || 'info'" effect="plain" size="small">
                {{ statusMeta[plan.status]?.label || plan.status }}
              </el-tag>
              <h2>{{ plan.name }}</h2>
            </div>
            <el-button text type="danger" @click="removePlan(plan)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>

          <div class="plan-meta">
            <span><el-icon><Flag /></el-icon>{{ goalLabel(plan.goal) }}</span>
            <span><el-icon><Calendar /></el-icon>{{ formatDate(plan.startDate) }}</span>
            <span><el-icon><Timer /></el-icon>共 {{ plan.weeks }} 周</span>
          </div>

          <div class="plan-counts">
            <div>
              <strong>{{ plan.dayCount }}</strong>
              <span>训练日</span>
            </div>
            <div>
              <strong>{{ plan.exerciseCount }}</strong>
              <span>动作安排</span>
            </div>
          </div>

          <el-button class="open-btn" @click="router.push(`/plans/${plan.id}`)">
            查看与编辑计划
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </article>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="新建训练计划" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="计划名称" prop="name">
          <el-input v-model="form.name" maxlength="100" placeholder="例如：四周增肌计划" />
        </el-form-item>
        <el-form-item label="训练目标">
          <el-select v-model="form.goal" style="width: 100%">
            <el-option v-for="item in goalOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择开始日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="计划周数" prop="weeks">
          <el-input-number v-model="form.weeks" :min="1" :max="52" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">创建计划</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="templateDialogVisible" title="从系统模板创建" width="620px">
      <el-empty v-if="templates.length === 0" description="暂无可用模板" :image-size="80" />
      <div v-else class="template-grid">
        <article v-for="template in templates" :key="template.id" class="template-card">
          <h3>{{ template.name }}</h3>
          <p>{{ goalLabel(template.goal) }} · {{ template.weeks }} 周 · {{ template.dayCount }} 个训练日</p>
          <el-button type="primary" plain :loading="templateSaving" @click="useTemplate(template)">
            使用此模板
          </el-button>
        </article>
      </div>
      <template #footer>
        <el-button @click="templateDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.plan-area {
  min-height: 240px;
}

.head-actions {
  display: flex;
  gap: 8px;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.template-card {
  padding: 16px;
  border: 1px solid #e8ebf2;
  border-radius: 12px;
}

.template-card h3 {
  margin: 0;
  font-size: 15px;
}

.template-card p {
  margin: 8px 0 14px;
  color: #8b95a7;
  font-size: 12px;
}

.plan-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.plan-card {
  padding: 22px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.plan-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 34px rgba(31, 41, 55, 0.09);
}

.plan-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.plan-top h2 {
  margin: 12px 0 0;
  font-size: 19px;
  line-height: 1.4;
}

.plan-meta {
  display: grid;
  gap: 9px;
  margin: 20px 0;
  color: #6f7b91;
  font-size: 12px;
}

.plan-meta span {
  display: flex;
  align-items: center;
  gap: 7px;
}

.plan-counts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  padding: 14px;
  border-radius: 12px;
  background: #f6f8fc;
}

.plan-counts div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.plan-counts strong {
  font-size: 22px;
}

.plan-counts span {
  color: #8b95a7;
  font-size: 11px;
}

.open-btn {
  width: 100%;
  margin-top: 18px;
  border-color: #dce5f5;
  color: #3f7cff;
}

@media (max-width: 1100px) {
  .plan-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .plan-grid {
    grid-template-columns: 1fr;
  }
}
</style>
