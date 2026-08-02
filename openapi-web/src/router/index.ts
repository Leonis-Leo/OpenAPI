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
          path: 'users',
          name: 'users',
          component: () => import('@/views/UserManage.vue'),
          meta: { title: '用户管理' }
        }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.path !== '/login' && !userStore.token) {
    return '/login'
  }
  return true
})

export default router
