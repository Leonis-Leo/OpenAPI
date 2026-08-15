import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import AdminLayout from '@/layout/AdminLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue')
    },
    {
      path: '/network-error',
      name: 'network-error',
      component: () => import('@/views/NetworkErrorView.vue'),
      meta: { title: '网络异常', public: true }
    },
    {
      path: '/',
      component: AdminLayout,
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue'),
          meta: { title: '概览' }
        },
        {
          path: 'apps',
          name: 'apps',
          component: () => import('@/views/AppManage.vue'),
          meta: { title: '应用管理' }
        },
        {
          path: 'interfaces',
          name: 'interfaces',
          component: () => import('@/views/InterfaceManage.vue'),
          meta: { title: '接口管理' }
        },
        {
          path: 'subscribes',
          name: 'subscribes',
          component: () => import('@/views/SubscribeManage.vue'),
          meta: { title: '订阅审批' }
        },
        {
          path: 'stats',
          name: 'stats',
          component: () => import('@/views/StatsView.vue'),
          meta: { title: '调用统计' }
        },
        {
          path: 'stats/detail',
          name: 'stats-detail',
          component: () => import('@/views/StatsDetailView.vue'),
          meta: { title: '调用明细' }
        },
        {
          path: 'coming-soon',
          name: 'coming-soon',
          component: () => import('@/views/ComingSoonView.vue'),
          meta: { title: '规划中' }
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/views/UserManage.vue'),
          meta: { title: '用户管理' }
        },
        {
          path: 'ratelimit',
          name: 'ratelimit',
          component: () => import('@/views/RateLimitManage.vue'),
          meta: { title: '限流配置' }
        },
        {
          path: 'logs',
          name: 'logs',
          component: () => import('@/views/LogManage.vue'),
          meta: { title: 'API 日志' }
        },
        {
          path: 'audit-logs',
          name: 'audit-logs',
          component: () => import('@/views/AuditLogManage.vue'),
          meta: { title: '审计日志' }
        },
        {
          path: 'notifications',
          name: 'notifications',
          component: () => import('@/views/NotificationCenter.vue'),
          meta: { title: '通知中心' }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue')
    }
  ]
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (!to.meta.public && to.path !== '/login' && !userStore.user) {
    return '/login'
  }
  const adminRoutes = ['/stats', '/stats/detail', '/users', '/ratelimit', '/logs', '/audit-logs']
  if (adminRoutes.includes(to.path) && userStore.user?.userRole !== 'admin') {
    return '/dashboard'
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title
    ? `${to.meta.title} - OpenAPI 开放平台`
    : 'OpenAPI 开放平台'
})

export default router
