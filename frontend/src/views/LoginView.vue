<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true
  try {
    await auth.login(form)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/')
  } catch {
    // 错误提示已由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <section class="auth-visual">
      <div class="visual-top">
        <span class="logo-mark">F</span>
        <span>FitLog</span>
      </div>

      <div class="visual-copy">
        <h1>把每一组训练<br />都变成看得见的进步</h1>
        <p>计划、记录、统计与建议，围绕同一份训练数据展开。</p>
        <ul>
          <li><span></span>逐组记录重量与次数</li>
          <li><span></span>自动统计训练量与部位均衡</li>
          <li><span></span>历史记录不受计划修改影响</li>
        </ul>
      </div>

      <div class="visual-foot">基于 Spring Boot 3 + Vue 3</div>
    </section>

    <section class="auth-panel">
      <div class="form-box">
        <h2>欢迎回来</h2>
        <p class="form-sub">登录后继续记录今天的训练。</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @keyup.enter="submit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" autocomplete="username">
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入密码"
              autocomplete="current-password"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
        </el-form>

        <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">
          登录
        </el-button>

        <div class="switch-line">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.auth-page {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(420px, 0.95fr);
  min-height: 100vh;
}

.auth-visual {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px 56px;
  background:
    radial-gradient(circle at 78% 18%, rgba(90, 120, 255, 0.28), transparent 32%),
    radial-gradient(circle at 18% 82%, rgba(124, 92, 255, 0.22), transparent 36%),
    linear-gradient(150deg, #0f172a 0%, #151f35 55%, #1b2740 100%);
  color: #fff;
}

.visual-top {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 1px;
}

.logo-mark {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 12px;
  background: linear-gradient(135deg, #4f8cff, #7c5cff);
}

.visual-copy h1 {
  margin: 0;
  font-size: clamp(34px, 4vw, 52px);
  line-height: 1.22;
  letter-spacing: 1px;
}

.visual-copy p {
  max-width: 460px;
  margin: 22px 0 30px;
  color: #aeb9cf;
  font-size: 15px;
  line-height: 1.8;
}

.visual-copy ul {
  display: grid;
  gap: 14px;
  margin: 0;
  padding: 0;
  list-style: none;
  color: #d7dfef;
  font-size: 14px;
}

.visual-copy li {
  display: flex;
  align-items: center;
  gap: 10px;
}

.visual-copy li span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #5c8dff;
  box-shadow: 0 0 12px rgba(92, 141, 255, 0.9);
}

.visual-foot {
  color: #6f7d98;
  font-size: 12px;
  letter-spacing: 1px;
}

.auth-panel {
  display: grid;
  place-items: center;
  padding: 40px 24px;
  background: #f7f8fb;
}

.form-box {
  width: 100%;
  max-width: 380px;
}

.form-box h2 {
  margin: 0;
  font-size: 30px;
  letter-spacing: 0.5px;
}

.form-sub {
  margin: 10px 0 30px;
  color: #8b95a7;
  font-size: 14px;
}

.submit-btn {
  width: 100%;
  height: 46px;
  margin-top: 6px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(90deg, #3f7cff, #6e5bff);
  font-size: 15px;
  font-weight: 600;
}

.switch-line {
  margin-top: 22px;
  color: #8b95a7;
  font-size: 13px;
  text-align: center;
}

.switch-line a {
  color: #3f7cff;
  font-weight: 600;
}

@media (max-width: 900px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .auth-visual {
    display: none;
  }

  .auth-panel {
    min-height: 100vh;
  }
}
</style>
