<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度需为 4—20 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需为 6—32 个字符', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true
  try {
    await auth.register({
      username: form.username,
      nickname: form.nickname,
      password: form.password
    })
    ElMessage.success('注册成功，已自动登录')
    router.push('/')
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
        <h1>从今天开始<br />认真记录每一次训练</h1>
        <p>一份计划、一组数据、一条趋势，都是判断是否进步的根据。</p>
      </div>

      <div class="visual-foot">基于 Spring Boot 3 + Vue 3</div>
    </section>

    <section class="auth-panel">
      <div class="form-box">
        <h2>创建账号</h2>
        <p class="form-sub">注册后即可创建训练计划并记录训练。</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @keyup.enter="submit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="4—20 位字母、数字或下划线" autocomplete="username" />
          </el-form-item>

          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="form.nickname" placeholder="展示在系统中的名字" />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="至少 6 位"
              autocomplete="new-password"
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              show-password
              placeholder="再次输入密码"
              autocomplete="new-password"
            />
          </el-form-item>
        </el-form>

        <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">
          注册并登录
        </el-button>

        <div class="switch-line">
          已有账号？
          <router-link to="/login">返回登录</router-link>
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
  font-size: clamp(32px, 4vw, 48px);
  line-height: 1.25;
  letter-spacing: 1px;
}

.visual-copy p {
  max-width: 460px;
  margin: 22px 0 0;
  color: #aeb9cf;
  font-size: 15px;
  line-height: 1.8;
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
  margin: 10px 0 26px;
  color: #8b95a7;
  font-size: 14px;
}

.submit-btn {
  width: 100%;
  height: 46px;
  margin-top: 4px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(90deg, #3f7cff, #6e5bff);
  font-size: 15px;
  font-weight: 600;
}

.switch-line {
  margin-top: 20px;
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
