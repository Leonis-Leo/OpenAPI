<template>
  <el-container class="layout-shell">
    <el-aside :width="isCollapse ? '76px' : '240px'" class="sidebar">
      <div class="brand" :class="{ compact: isCollapse }">
        <div class="brand-mark">OP</div>
        <div v-if="!isCollapse" class="brand-copy">
          <strong>OpenAPI</strong>
          <span>开放平台</span>
        </div>
      </div>

      <div v-if="!isCollapse" class="workspace-switcher">
        <div class="workspace-icon">A</div>
        <div class="workspace-copy"><span>当前工作区</span><strong>OpenAPI 主空间</strong></div>
        <el-icon><ArrowDown /></el-icon>
      </div>

      <el-menu :default-active="route.path" router :collapse="isCollapse" :collapse-transition="false" class="menu">
        <div v-if="!isCollapse" class="menu-label">工作台</div>
        <el-menu-item index="/dashboard" title="工作台"><el-icon><HomeFilled /></el-icon><span>工作台</span></el-menu-item>

        <div v-if="!isCollapse" class="menu-label">资源管理</div>
        <el-menu-item index="/apps" title="应用管理"><el-icon><Box /></el-icon><span>应用管理</span></el-menu-item>
        <el-menu-item index="/interfaces" title="接口管理"><el-icon><Connection /></el-icon><span>接口管理</span></el-menu-item>
        <el-menu-item index="/subscribes" title="订阅审批"><el-icon><DocumentChecked /></el-icon><span>订阅审批</span></el-menu-item>
        <el-menu-item
          index="/coming-soon"
          :route="{ path: '/coming-soon', query: { name: '服务目录', desc: '按分组/标签/负责人浏览开放接口，支持开发者搜索与订阅申请入口' } }"
          title="服务目录"
        >
          <el-icon><Files /></el-icon><span>服务目录</span><span v-if="!isCollapse" class="menu-badge">规划中</span>
        </el-menu-item>

        <div v-if="isAdmin && !isCollapse" class="menu-label">运营分析</div>
        <el-menu-item v-if="isAdmin" index="/stats" title="调用统计"><el-icon><TrendCharts /></el-icon><span>调用统计</span></el-menu-item>
        <el-menu-item v-if="isAdmin" index="/stats/detail" title="调用明细"><el-icon><DataLine /></el-icon><span>调用明细</span></el-menu-item>
        <el-menu-item v-if="isAdmin" index="/logs" title="API 日志"><el-icon><Document /></el-icon><span>API 日志</span></el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/coming-soon"
          :route="{ path: '/coming-soon', query: { name: '监控告警', desc: 'JVM 指标、调用量波动与限流命中率监控，异常自动告警' } }"
          title="监控告警"
        >
          <el-icon><Odometer /></el-icon><span>监控告警</span><span v-if="!isCollapse" class="menu-badge">规划中</span>
        </el-menu-item>

        <div v-if="isAdmin && !isCollapse" class="menu-label">开放治理</div>
        <el-menu-item
          v-if="isAdmin"
          index="/coming-soon"
          :route="{ path: '/coming-soon', query: { name: '策略中心', desc: '按应用/用户/IP/接口/方法/路径维度统一配置限流、黑白名单、缓存与改写' } }"
          title="策略中心"
        >
          <el-icon><SetUp /></el-icon><span>策略中心</span><span v-if="!isCollapse" class="menu-badge">规划中</span>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/coming-soon"
          :route="{ path: '/coming-soon', query: { name: '灰度发布', desc: '按应用/用户/Header/IP 或比例路由到不同版本，保留流量与回滚记录' } }"
          title="灰度发布"
        >
          <el-icon><Promotion /></el-icon><span>灰度发布</span><span v-if="!isCollapse" class="menu-badge">规划中</span>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/coming-soon"
          :route="{ path: '/coming-soon', query: { name: '敏感数据脱敏', desc: '对请求参数/响应体/日志按字段配置脱敏策略，覆盖密码、Token、证件号等' } }"
          title="敏感数据脱敏"
        >
          <el-icon><Lock /></el-icon><span>敏感脱敏</span><span v-if="!isCollapse" class="menu-badge">规划中</span>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/coming-soon"
          :route="{ path: '/coming-soon', query: { name: 'API 文档', desc: 'OpenAPI 导入校验、在线预览、多语言 SDK 与示例代码下载' } }"
          title="API 文档"
        >
          <el-icon><Reading /></el-icon><span>API 文档</span><span v-if="!isCollapse" class="menu-badge">规划中</span>
        </el-menu-item>

        <div v-if="isAdmin && !isCollapse" class="menu-label">平台设置</div>
        <el-menu-item v-if="isAdmin" index="/users" title="用户管理"><el-icon><User /></el-icon><span>用户管理</span></el-menu-item>
        <el-menu-item v-if="isAdmin" index="/ratelimit" title="限流配置"><el-icon><Timer /></el-icon><span>限流配置</span></el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/coming-soon"
          :route="{ path: '/coming-soon', query: { name: '团队与权限', desc: '组织/团队/成员与资源级 RBAC，细粒度授权' } }"
          title="团队与权限"
        >
          <el-icon><UserFilled /></el-icon><span>团队与权限</span><span v-if="!isCollapse" class="menu-badge">规划中</span>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/coming-soon"
          :route="{ path: '/coming-soon', query: { name: '多租户', desc: '租户级数据隔离、独立密钥空间与配额' } }"
          title="多租户"
        >
          <el-icon><OfficeBuilding /></el-icon><span>多租户</span><span v-if="!isCollapse" class="menu-badge">规划中</span>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/coming-soon"
          :route="{ path: '/coming-soon', query: { name: '计费账单', desc: '按应用/接口/天计费，套餐、订单与账单导出' } }"
          title="计费账单"
        >
          <el-icon><Money /></el-icon><span>计费账单</span><span v-if="!isCollapse" class="menu-badge">规划中</span>
        </el-menu-item>

        <div v-if="isAdmin && !isCollapse" class="menu-label">开发者中心</div>
        <el-menu-item
          v-if="isAdmin"
          index="/coming-soon"
          :route="{ path: '/coming-soon', query: { name: 'SDK 与示例', desc: '多语言 SDK、签名工具与接入示例' } }"
          title="SDK 与示例"
        >
          <el-icon><Platform /></el-icon><span>SDK 与示例</span><span v-if="!isCollapse" class="menu-badge">规划中</span>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/coming-soon"
          :route="{ path: '/coming-soon', query: { name: '压测报告', desc: 'QPS / 响应时间基准与容量评估' } }"
          title="压测报告"
        >
          <el-icon><DataAnalysis /></el-icon><span>压测报告</span><span v-if="!isCollapse" class="menu-badge">规划中</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <div class="status-dot"></div><span v-if="!isCollapse">所有服务运行正常</span>
      </div>
    </el-aside>

    <el-container class="main-shell">
      <el-header class="topbar">
        <div class="topbar-left">
          <button class="icon-button" aria-label="折叠导航" @click="toggleCollapse"><el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon></button>
          <el-breadcrumb class="breadcrumb" separator="/">
            <el-breadcrumb-item>工作台</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar-right">
          <button class="icon-button" aria-label="切换主题" @click="toggleTheme"><el-icon><Sunny v-if="isDark" /><Moon v-else /></el-icon></button>
          <span class="env-pill"><i></i> 本地开发环境</span>
          <button class="icon-button" aria-label="搜索"><el-icon><Search /></el-icon></button>
          <el-popover
            v-model:visible="notificationVisible"
            placement="bottom-end"
            :width="380"
            trigger="click"
            popper-class="notification-popover"
            @show="handleOpenNotifications"
          >
            <template #reference>
              <button class="icon-button" aria-label="通知">
                <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notification-badge">
                  <el-icon><Bell /></el-icon>
                </el-badge>
              </button>
            </template>
            <div class="notification-panel">
              <div class="notification-header">
                <span>通知</span>
                <el-button v-if="unreadCount > 0" text type="primary" size="small" @click="handleReadAll">全部已读</el-button>
              </div>
              <div v-loading="notificationLoading" class="notification-body">
                <template v-if="notifications.length">
                  <div
                    v-for="item in notifications"
                    :key="item.id"
                    class="notification-item"
                    :class="{ unread: item.isRead === 0 }"
                    @click="handleNotificationClick(item)"
                  >
                    <span v-if="item.isRead === 0" class="notification-dot-item"></span>
                    <div class="notification-main">
                      <div class="notification-title">{{ item.title }}</div>
                      <div class="notification-content">{{ item.content }}</div>
                      <div class="notification-time">{{ item.createTime }}</div>
                    </div>
                  </div>
                </template>
                <div v-else class="notification-empty">暂无通知</div>
              </div>
              <div class="notification-footer">
                <el-button text type="primary" size="small" @click="goNotificationCenter">查看全部</el-button>
              </div>
            </div>
          </el-popover>
          <el-divider direction="vertical" />
          <el-dropdown @command="handleCommand">
            <button class="profile-trigger" aria-label="打开用户菜单"><el-avatar :size="32">{{ (userStore.user?.userName || userStore.user?.userAccount || 'U').slice(0, 1).toUpperCase() }}</el-avatar><span>{{ userStore.user?.userName || userStore.user?.userAccount || '用户' }}</span><el-icon><ArrowDown /></el-icon></button>
            <template #dropdown><el-dropdown-menu><el-dropdown-item command="profile">个人中心</el-dropdown-item><el-dropdown-item command="logout">退出登录</el-dropdown-item></el-dropdown-menu></template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="page-main"><router-view v-slot="{ Component }"><keep-alive><component :is="Component" /></keep-alive></router-view></el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="profileVisible" title="个人中心" width="420px">
    <el-form label-width="80px">
      <el-form-item label="账号"><el-input :model-value="userStore.user?.userAccount" disabled /></el-form-item>
      <el-form-item label="昵称"><el-input v-model="profileForm.userName" placeholder="请输入昵称" /></el-form-item>
      <el-form-item label="新密码"><el-input v-model="profileForm.userPassword" type="password" show-password placeholder="留空则不修改密码" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="profileVisible = false">取消</el-button><el-button type="primary" :loading="savingProfile" @click="handleSaveProfile">保存</el-button></template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowDown,
  Bell,
  Box,
  Connection,
  DataAnalysis,
  DataLine,
  Document,
  DocumentChecked,
  Expand,
  Files,
  Fold,
  HomeFilled,
  Lock,
  Money,
  Moon,
  Odometer,
  OfficeBuilding,
  Platform,
  Promotion,
  Reading,
  Search,
  SetUp,
  Sunny,
  Timer,
  TrendCharts,
  User,
  UserFilled
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import {
  logout as logoutApi,
  selfUpdate,
  pageNotifications,
  unreadNotificationCount,
  readNotification,
  readAllNotifications,
  type NotificationItem
} from '@/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isAdmin = userStore.user?.userRole === 'admin'
const isCollapse = ref(localStorage.getItem('openapi-sidebar') === '1')
const isDark = ref(document.documentElement.classList.contains('dark'))
const profileVisible = ref(false)
const savingProfile = ref(false)
const profileForm = reactive({ userName: '', userPassword: '' })
const notificationVisible = ref(false)
const notificationLoading = ref(false)
const notifications = ref<NotificationItem[]>([])
const unreadCount = ref(0)
let notificationTimer: ReturnType<typeof setInterval> | undefined

function toggleCollapse() { isCollapse.value = !isCollapse.value; localStorage.setItem('openapi-sidebar', isCollapse.value ? '1' : '0') }
function toggleTheme() { isDark.value = !isDark.value; document.documentElement.classList.toggle('dark', isDark.value); localStorage.setItem('openapi-theme', isDark.value ? 'dark' : 'light') }

async function refreshUnreadCount() {
  try {
    unreadCount.value = await unreadNotificationCount()
  } catch {
    // 忽略轮询失败，下次重试
  }
}

async function loadNotifications() {
  notificationLoading.value = true
  try {
    const result = await pageNotifications({ current: 1, size: 10 })
    notifications.value = result.records
  } finally {
    notificationLoading.value = false
  }
}

async function handleOpenNotifications() {
  await Promise.all([loadNotifications(), refreshUnreadCount()])
}

function goNotificationCenter() {
  notificationVisible.value = false
  router.push('/notifications')
}

async function handleNotificationClick(item: NotificationItem) {
  if (item.isRead === 0) {
    try {
      await readNotification(item.id)
      item.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch {
      // 标记失败不阻塞跳转
    }
  }
  notificationVisible.value = false
  if (item.link) {
    router.push(item.link)
  }
}

async function handleReadAll() {
  try {
    await readAllNotifications()
  } catch {
    // 忽略
  }
  notifications.value.forEach((n) => (n.isRead = 1))
  unreadCount.value = 0
}

async function handleCommand(command: string) {
  if (command === 'logout') { try { await logoutApi() } catch { /* local cleanup still applies */ } userStore.logout(); window.location.href = '/login' }
  if (command === 'profile') { profileForm.userName = userStore.user?.userName ?? ''; profileForm.userPassword = ''; profileVisible.value = true }
}
async function handleSaveProfile() {
  savingProfile.value = true
  try {
    await selfUpdate({ userName: profileForm.userName.trim() || undefined, userPassword: profileForm.userPassword || undefined })
    if (userStore.user && profileForm.userName.trim()) { userStore.user.userName = profileForm.userName.trim(); localStorage.setItem('openapi_user', JSON.stringify(userStore.user)) }
    ElMessage.success('已保存'); profileVisible.value = false
  } finally { savingProfile.value = false }
}

watch(
  () => route.path,
  () => refreshUnreadCount()
)

onMounted(() => {
  refreshUnreadCount()
  notificationTimer = setInterval(refreshUnreadCount, 30000)
})

onUnmounted(() => {
  if (notificationTimer) clearInterval(notificationTimer)
})
</script>

<style scoped>
.layout-shell { height: 100vh; background: var(--app-bg); }
.sidebar { position: relative; overflow: hidden; background: var(--nav-bg); transition: width .22s ease; color: #cbd5e1; }
.brand { height: 72px; display: flex; align-items: center; gap: 12px; padding: 0 24px; box-sizing: border-box; }
.brand.compact { justify-content: center; padding: 0; }
.brand-mark { display: grid; place-items: center; width: 34px; height: 34px; border-radius: 10px; background: var(--app-primary); color: white; font-weight: 800; letter-spacing: -1px; }
.brand-copy { display: flex; flex-direction: column; line-height: 1.15; color: #f8fafc; }.brand-copy strong { font-size: 16px; }.brand-copy span { margin-top: 4px; color: #94a3b8; font-size: 11px; }
.workspace-switcher { display: flex; align-items: center; gap: 10px; margin: 0 14px 18px; padding: 10px; border: 1px solid rgba(148,163,184,.16); border-radius: 10px; background: rgba(15,23,42,.32); color: #94a3b8; }.workspace-icon { display: grid; place-items: center; width: 28px; height: 28px; border-radius: 8px; background: #dbeafe; color: #2563eb; font-weight: 700; }.workspace-copy { display: flex; flex: 1; min-width: 0; flex-direction: column; gap: 3px; }.workspace-copy span { font-size: 11px; }.workspace-copy strong { overflow: hidden; color: #e2e8f0; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.menu { height: calc(100% - 154px); overflow: hidden auto; border-right: 0; background: transparent; }.menu-label { padding: 12px 24px 7px; color: #71809a; font-size: 11px; letter-spacing: .08em; }.menu :deep(.el-menu-item) { position: relative; height: 44px; margin: 3px 12px; border-radius: 9px; color: #a8b5ca; transition: background-color .18s ease, color .18s ease, transform .18s ease; }.menu :deep(.el-menu-item .el-icon) { margin-right: 12px; font-size: 17px; }.menu :deep(.el-menu-item:hover) { background: rgba(148,163,184,.12); color: #f8fafc; transform: translateX(1px); }.menu :deep(.el-menu-item.is-active) { background: #dbeafe; color: #1d4ed8; font-weight: 600; }.menu :deep(.el-menu-item.is-active)::before { position: absolute; left: 0; top: 10px; bottom: 10px; width: 3px; border-radius: 0 4px 4px 0; background: #2563eb; content: ''; }.menu :deep(.el-menu-item) .menu-badge { margin-left: auto; padding: 1px 6px; border-radius: 6px; background: rgba(148, 163, 184, .16); color: #94a3b8; font-size: 10px; font-weight: 500; line-height: 16px; }.menu :deep(.el-menu-item.is-active) .menu-badge { background: rgba(37, 99, 235, .12); color: #1d4ed8; }.menu.el-menu--collapse { width: 100%; }.menu.el-menu--collapse :deep(.el-menu-item) { width: 44px; box-sizing: border-box; justify-content: center; margin: 6px auto; padding: 0 !important; }.menu.el-menu--collapse :deep(.el-menu-item .el-icon) { margin: 0; font-size: 19px; }.menu.el-menu--collapse :deep(.el-menu-item.is-active)::before { left: -1px; top: 10px; bottom: 10px; }
.sidebar-footer { position: absolute; right: 16px; bottom: 20px; left: 16px; display: flex; align-items: center; gap: 8px; color: #64748b; font-size: 11px; white-space: nowrap; }.status-dot { width: 7px; height: 7px; border-radius: 50%; background: #34d399; box-shadow: 0 0 0 3px rgba(52,211,153,.12); }
.main-shell { min-width: 0; }.topbar { display: flex; align-items: center; justify-content: space-between; height: 72px; padding: 0 28px; border-bottom: 1px solid var(--app-border); background: var(--app-surface); }.topbar-left,.topbar-right { display: flex; align-items: center; gap: 14px; }.icon-button { position: relative; display: grid; place-items: center; width: 34px; height: 34px; border: 0; border-radius: 8px; background: transparent; color: var(--app-muted); cursor: pointer; }.icon-button:hover { background: var(--app-bg); color: var(--app-text); }.icon-button .el-icon { font-size: 18px; }.notification-badge :deep(.el-badge__content) { border: 0; font-size: 10px; line-height: 16px; height: 16px; min-width: 16px; padding: 0 4px; }.breadcrumb :deep(.el-breadcrumb__inner) { color: var(--app-muted); font-size: 13px; }.breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) { color: var(--app-text); font-weight: 600; }.env-pill { display: inline-flex; align-items: center; gap: 7px; padding: 6px 10px; border-radius: 999px; background: #ecfdf5; color: #047857; font-size: 12px; }.env-pill i { width: 6px; height: 6px; border-radius: 50%; background: #10b981; }.profile-trigger { display: flex; align-items: center; gap: 9px; border: 0; background: transparent; color: var(--app-text); cursor: pointer; font-size: 13px; }.profile-trigger .el-icon { color: var(--app-muted); }
.notification-panel { min-height: 120px; }.notification-header { display: flex; align-items: center; justify-content: space-between; padding: 4px 6px 10px; font-weight: 600; }.notification-body { max-height: 380px; overflow: auto; }.notification-item { position: relative; display: flex; gap: 8px; padding: 10px 8px; border-radius: 8px; cursor: pointer; }.notification-item:hover { background: var(--el-fill-color-light, #f5f7fa); }.notification-item.unread { background: #eff6ff; }.notification-item.unread:hover { background: #dbeafe; }.notification-dot-item { flex: 0 0 auto; width: 8px; height: 8px; margin-top: 6px; border-radius: 50%; background: #2563eb; }.notification-main { flex: 1; min-width: 0; }.notification-title { font-size: 13px; font-weight: 600; color: var(--el-text-color-primary, #303133); }.notification-content { margin-top: 3px; font-size: 12px; line-height: 1.5; color: var(--el-text-color-regular, #606266); word-break: break-all; }.notification-time { margin-top: 4px; font-size: 11px; color: var(--el-text-color-secondary, #909399); }.notification-empty { padding: 32px 0; text-align: center; color: var(--el-text-color-secondary, #909399); font-size: 13px; }.notification-footer { padding: 6px 4px 0; border-top: 1px solid var(--el-border-color-lighter, #ebeef5); text-align: center; }
.page-main { overflow: auto; padding: 28px 32px 40px; background: var(--app-bg); }
@media (max-width: 900px) { .page-main { padding: 20px; }.env-pill,.topbar-right > .icon-button:first-of-type { display: none; }.topbar { padding: 0 18px; } }
</style>
