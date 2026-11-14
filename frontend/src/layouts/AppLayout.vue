<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activeMenu = computed(() => {
  if (route.path.startsWith('/plans')) return '/plans'
  if (route.path.startsWith('/sessions')) return '/sessions'
  return route.path
})
const pageTitle = computed(() => route.meta.title || '首页')
const avatarText = computed(() => {
  const name = auth.user?.nickname || auth.user?.username || 'U'
  return name.slice(0, 1).toUpperCase()
})
const todayText = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date())

async function handleCommand(command) {
  if (command !== 'logout') {
    return
  }
  try {
    await ElMessageBox.confirm('退出后需要重新登录，确定退出吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
    auth.logout()
    router.push('/login')
  } catch {
    // 用户取消
  }
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="232px" class="aside">
      <div class="brand">
        <div class="brand-mark">F</div>
        <div>
          <div class="brand-name">FitLog</div>
          <div class="brand-sub">训练计划与记录</div>
        </div>
      </div>

      <el-menu
        :default-active="activeMenu"
        router
        class="menu"
        background-color="transparent"
        text-color="#c3cad9"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/exercises">
          <el-icon><Search /></el-icon>
          <span>动作库</span>
        </el-menu-item>
        <el-menu-item index="/plans">
          <el-icon><Calendar /></el-icon>
          <span>训练计划</span>
        </el-menu-item>
        <el-menu-item index="/sessions">
          <el-icon><Notebook /></el-icon>
          <span>训练记录</span>
        </el-menu-item>
        <el-menu-item index="/metrics">
          <el-icon><DataBoard /></el-icon>
          <span>身体数据</span>
        </el-menu-item>
        <el-menu-item index="/stats">
          <el-icon><TrendCharts /></el-icon>
          <span>数据统计</span>
        </el-menu-item>
        <el-menu-item index="/advice">
          <el-icon><MagicStick /></el-icon>
          <span>智能建议</span>
        </el-menu-item>
        <el-menu-item v-if="auth.user?.role === 'ADMIN'" index="/admin">
          <el-icon><Setting /></el-icon>
          <span>管理后台</span>
        </el-menu-item>
      </el-menu>

      <div class="aside-foot">
        <span>毕业设计</span>
        <span>2026 — 2027</span>
      </div>
    </el-aside>

    <el-container>
      <el-header height="68px" class="header">
        <div>
          <div class="header-title">{{ pageTitle }}</div>
          <div class="header-date">{{ todayText }}</div>
        </div>

        <el-dropdown trigger="click" @command="handleCommand">
          <span class="user-chip">
            <el-avatar :size="32" class="avatar">{{ avatarText }}</el-avatar>
            <span class="user-name">{{ auth.user?.nickname || auth.user?.username || '用户' }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}

.aside {
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #111827 0%, #162033 100%);
  color: #fff;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 22px 26px;
}

.brand-mark {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 11px;
  background: linear-gradient(135deg, #4f8cff, #7c5cff);
  font-size: 20px;
  font-weight: 800;
}

.brand-name {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-sub {
  margin-top: 2px;
  color: #8f9bb3;
  font-size: 11px;
}

.menu {
  flex: 1;
  border-right: none;
  padding: 4px 12px;
}

.menu :deep(.el-menu-item) {
  height: 46px;
  margin: 4px 0;
  border-radius: 10px;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.07);
}

.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(79, 140, 255, 0.9), rgba(124, 92, 255, 0.75));
  font-weight: 600;
}

.aside-foot {
  display: flex;
  justify-content: space-between;
  padding: 18px 22px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  color: #6f7b91;
  font-size: 11px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  border-bottom: 1px solid #e8ebf2;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
}

.header-title {
  font-size: 16px;
  font-weight: 700;
}

.header-date {
  margin-top: 3px;
  color: #8b95a7;
  font-size: 12px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 10px 5px 6px;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s;
}

.user-chip:hover {
  background: #f2f4f8;
}

.avatar {
  background: linear-gradient(135deg, #4f8cff, #7c5cff);
  font-weight: 700;
}

.user-name {
  font-size: 13px;
  color: #374151;
}

.main {
  padding: 0;
  background: #f3f5f9;
}

@media (max-width: 900px) {
  .aside {
    width: 76px !important;
  }

  .brand {
    justify-content: center;
    padding: 20px 0;
  }

  .brand > div:last-child,
  .aside-foot,
  .menu :deep(.el-menu-item span) {
    display: none;
  }

  .menu {
    padding: 4px 8px;
  }

  .menu :deep(.el-menu-item) {
    justify-content: center;
  }
}
</style>
