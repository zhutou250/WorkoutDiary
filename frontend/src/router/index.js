import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue'),
    meta: { guest: true }
  },
  {
    path: '/',
    component: () => import('../layouts/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'dashboard',
        component: () => import('../views/DashboardView.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'exercises',
        name: 'exercises',
        component: () => import('../views/ExerciseLibraryView.vue'),
        meta: { title: '动作库' }
      },
      {
        path: 'plans',
        name: 'plans',
        component: () => import('../views/PlansView.vue'),
        meta: { title: '训练计划' }
      },
      {
        path: 'plans/:id',
        name: 'plan-detail',
        component: () => import('../views/PlanDetailView.vue'),
        meta: { title: '计划详情' }
      },
      {
        path: 'sessions',
        name: 'sessions',
        component: () => import('../views/SessionsView.vue'),
        meta: { title: '训练记录' }
      },
      {
        path: 'sessions/:id',
        name: 'session-detail',
        component: () => import('../views/SessionDetailView.vue'),
        meta: { title: '训练详情' }
      },
      {
        path: 'metrics',
        name: 'metrics',
        component: () => import('../views/MetricsView.vue'),
        meta: { title: '身体数据' }
      },
      {
        path: 'stats',
        name: 'stats',
        component: () => import('../views/StatsView.vue'),
        meta: { title: '数据统计' }
      },
      {
        path: 'advice',
        name: 'advice',
        component: () => import('../views/AdviceView.vue'),
        meta: { title: '智能建议' }
      },
      {
        path: 'admin',
        name: 'admin',
        component: () => import('../views/AdminView.vue'),
        meta: { title: '管理后台' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('fitness_token')
  if (to.meta.requiresAuth && !token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guest && token) {
    return { name: 'dashboard' }
  }
  return true
})

export default router
