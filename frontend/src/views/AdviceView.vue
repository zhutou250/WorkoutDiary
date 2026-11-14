<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { feedbackAdvice, generateAdvice, getAdvice, listAdvice } from '../api/advice'

const loading = ref(false)
const generating = ref(false)
const advices = ref([])
const active = ref(null)
const detailVisible = ref(false)
const feedbackVisible = ref(false)
const feedbackTarget = ref(null)
const feedbackSaving = ref(false)
const feedbackForm = reactive({
  rating: 5,
  helpful: true,
  comment: ''
})

const severityMeta = {
  INFO: { label: '提示', type: 'info' },
  WARN: { label: '注意', type: 'warning' },
  ALERT: { label: '重要', type: 'danger' }
}

async function load() {
  loading.value = true
  try {
    advices.value = await listAdvice()
  } catch {
    // 请求错误已统一提示
  } finally {
    loading.value = false
  }
}

async function generate() {
  generating.value = true
  try {
    const detail = await generateAdvice()
    ElMessage.success('已生成训练建议')
    active.value = detail
    detailVisible.value = true
    await load()
  } catch {
    // 请求错误已统一提示
  } finally {
    generating.value = false
  }
}

async function openDetail(advice) {
  active.value = await getAdvice(advice.id)
  detailVisible.value = true
}

function openFeedback(advice) {
  feedbackTarget.value = advice
  Object.assign(feedbackForm, { rating: 5, helpful: true, comment: '' })
  feedbackVisible.value = true
}

async function submitFeedback() {
  feedbackSaving.value = true
  try {
    await feedbackAdvice(feedbackTarget.value.id, { ...feedbackForm })
    ElMessage.success('感谢你的反馈')
    feedbackVisible.value = false
  } catch {
    // 请求错误已统一提示
  } finally {
    feedbackSaving.value = false
  }
}

function formatDate(value) {
  return value ? `${value.slice(0, 10)} — ${value.slice(0, 10)}` : '—'
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">智能建议</h1>
        <p class="page-subtitle">规则引擎基于真实训练数据给出结论，数字由后端计算，不调用大模型虚构。</p>
      </div>
      <el-button type="primary" size="large" :loading="generating" @click="generate">
        <el-icon><MagicStick /></el-icon>
        生成训练建议
      </el-button>
    </div>

    <div v-loading="loading" class="advice-area">
      <el-empty v-if="!loading && advices.length === 0" description="还没有建议，完成几次训练后点击“生成训练建议”">
      </el-empty>

      <div v-else class="advice-grid">
        <article v-for="advice in advices" :key="advice.id" class="advice-card card">
          <div class="advice-top">
            <span class="period">{{ formatDate(advice.periodStart) }}</span>
            <el-tag size="small" type="success">{{ advice.status === 'SUCCESS' ? '已生成' : advice.status }}</el-tag>
          </div>
          <h2>{{ advice.itemCount }} 条发现</h2>
          <p>模型：{{ advice.modelName || '—' }}</p>
          <div class="advice-actions">
            <el-button type="primary" plain @click="openDetail(advice)">查看详情</el-button>
            <el-button @click="openFeedback(advice)">评价</el-button>
          </div>
        </article>
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="训练建议详情" width="680px">
      <template v-if="active">
        <p class="dialog-tip">
          分析区间：{{ active.periodStart }} 至 {{ active.periodEnd }}
        </p>
        <div class="finding-list">
          <div v-for="item in active.items" :key="item.findingKey" class="finding-card">
            <div class="finding-head">
              <el-tag :type="severityMeta[item.severity]?.type || 'info'" size="small">
                {{ severityMeta[item.severity]?.label || item.severity }}
              </el-tag>
              <strong>{{ item.title }}</strong>
            </div>
            <p class="finding-text">{{ item.suggestionText }}</p>
          </div>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="feedbackVisible" title="评价训练建议" width="440px">
      <el-form :model="feedbackForm" label-position="top">
        <el-form-item label="评分">
          <el-rate v-model="feedbackForm.rating" />
        </el-form-item>
        <el-form-item label="是否有帮助">
          <el-switch v-model="feedbackForm.helpful" active-text="有帮助" inactive-text="没帮助" />
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input v-model="feedbackForm.comment" type="textarea" :rows="3" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedbackVisible = false">取消</el-button>
        <el-button type="primary" :loading="feedbackSaving" @click="submitFeedback">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.advice-area {
  min-height: 240px;
}

.advice-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.advice-card {
  padding: 20px;
}

.advice-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.period {
  color: #8b95a7;
  font-size: 12px;
}

.advice-card h2 {
  margin: 14px 0 6px;
  font-size: 20px;
}

.advice-card p {
  margin: 0;
  color: #8b95a7;
  font-size: 12px;
}

.advice-actions {
  display: flex;
  gap: 8px;
  margin-top: 18px;
}

.dialog-tip {
  margin: 0 0 16px;
  color: #8b95a7;
  font-size: 12px;
}

.finding-list {
  display: grid;
  gap: 12px;
}

.finding-card {
  padding: 14px 16px;
  border: 1px solid #e8ebf2;
  border-radius: 11px;
  background: #fbfcfe;
}

.finding-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.finding-head strong {
  font-size: 14px;
}

.finding-text {
  margin: 10px 0 0;
  color: #5b6b85;
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 1050px) {
  .advice-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .advice-grid {
    grid-template-columns: 1fr;
  }
}
</style>
