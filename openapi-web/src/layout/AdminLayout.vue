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
        <div v-if="isAdmin && !isCollapse" class="menu-label">运营分析</div>
        <el-menu-item v-if="isAdmin" index="/stats" title="调用统计"><el-icon><TrendCharts /></el-icon><span>调用统计</span></el-menu-item>
        <el-menu-item v-if="isAdmin" index="/logs" title="API 日志"><el-icon><Document /></el-icon><span>API 日志</span></el-menu-item>
        <div v-if="isAdmin && !isCollapse" class="menu-label">平台设置</div>
        <el-menu-item v-if="isAdmin" index="/users" title="用户管理"><el-icon><User /></el-icon><span>用户管理</span></el-menu-item>
        <el-menu-item v-if="isAdmin" index="/ratelimit" title="限流配置"><el-icon><Timer /></el-icon><span>限流配置</span></el-menu-item>
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
          <button class="icon-button" aria-label="通知"><el-icon><Bell /></el-icon><b class="notification-dot"></b></button>
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
import { reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, Bell, Box, Connection, Document, DocumentChecked, Expand, Fold, HomeFilled, Moon, Search, Sunny, Timer, TrendCharts, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { logout as logoutApi, selfUpdate } from '@/api'

const route = useRoute()
const userStore = useUserStore()
const isAdmin = userStore.user?.userRole === 'admin'
const isCollapse = ref(localStorage.getItem('openapi-sidebar') === '1')
const isDark = ref(document.documentElement.classList.contains('dark'))
const profileVisible = ref(false)
const savingProfile = ref(false)
const profileForm = reactive({ userName: '', userPassword: '' })

function toggleCollapse() { isCollapse.value = !isCollapse.value; localStorage.setItem('openapi-sidebar', isCollapse.value ? '1' : '0') }
function toggleTheme() { isDark.value = !isDark.value; document.documentElement.classList.toggle('dark', isDark.value); localStorage.setItem('openapi-theme', isDark.value ? 'dark' : 'light') }
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
</script>

<style scoped>
.layout-shell { height: 100vh; background: var(--app-bg); }
.sidebar { position: relative; overflow: hidden; background: var(--nav-bg); transition: width .22s ease; color: #cbd5e1; }
.brand { height: 72px; display: flex; align-items: center; gap: 12px; padding: 0 24px; box-sizing: border-box; }
.brand.compact { justify-content: center; padding: 0; }
.brand-mark { display: grid; place-items: center; width: 34px; height: 34px; border-radius: 10px; background: var(--app-primary); color: white; font-weight: 800; letter-spacing: -1px; }
.brand-copy { display: flex; flex-direction: column; line-height: 1.15; color: #f8fafc; }.brand-copy strong { font-size: 16px; }.brand-copy span { margin-top: 4px; color: #94a3b8; font-size: 11px; }
.workspace-switcher { display: flex; align-items: center; gap: 10px; margin: 0 14px 18px; padding: 10px; border: 1px solid rgba(148,163,184,.16); border-radius: 10px; background: rgba(15,23,42,.32); color: #94a3b8; }.workspace-icon { display: grid; place-items: center; width: 28px; height: 28px; border-radius: 8px; background: #dbeafe; color: #2563eb; font-weight: 700; }.workspace-copy { display: flex; flex: 1; min-width: 0; flex-direction: column; gap: 3px; }.workspace-copy span { font-size: 11px; }.workspace-copy strong { overflow: hidden; color: #e2e8f0; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.menu { height: calc(100% - 154px); overflow: hidden auto; border-right: 0; background: transparent; }.menu-label { padding: 12px 24px 7px; color: #64748b; font-size: 11px; letter-spacing: .08em; }.menu :deep(.el-menu-item) { height: 44px; margin: 3px 12px; border-radius: 9px; color: #94a3b8; }.menu :deep(.el-menu-item .el-icon) { margin-right: 12px; font-size: 17px; }.menu :deep(.el-menu-item:hover) { background: rgba(148,163,184,.1); color: #f8fafc; }.menu :deep(.el-menu-item.is-active) { background: #dbeafe; color: #1d4ed8; font-weight: 600; }.menu.el-menu--collapse { width: 100%; }.menu.el-menu--collapse :deep(.el-menu-item) { width: 44px; box-sizing: border-box; justify-content: center; margin: 6px auto; padding: 0 !important; }.menu.el-menu--collapse :deep(.el-menu-item .el-icon) { margin: 0; font-size: 19px; }
.sidebar-footer { position: absolute; right: 16px; bottom: 20px; left: 16px; display: flex; align-items: center; gap: 8px; color: #64748b; font-size: 11px; white-space: nowrap; }.status-dot { width: 7px; height: 7px; border-radius: 50%; background: #34d399; box-shadow: 0 0 0 3px rgba(52,211,153,.12); }
.main-shell { min-width: 0; }.topbar { display: flex; align-items: center; justify-content: space-between; height: 72px; padding: 0 28px; border-bottom: 1px solid var(--app-border); background: var(--app-surface); }.topbar-left,.topbar-right { display: flex; align-items: center; gap: 14px; }.icon-button { position: relative; display: grid; place-items: center; width: 34px; height: 34px; border: 0; border-radius: 8px; background: transparent; color: var(--app-muted); cursor: pointer; }.icon-button:hover { background: var(--app-bg); color: var(--app-text); }.icon-button .el-icon { font-size: 18px; }.notification-dot { position: absolute; top: 7px; right: 7px; width: 5px; height: 5px; border: 2px solid var(--app-surface); border-radius: 50%; background: #ef4444; }.breadcrumb :deep(.el-breadcrumb__inner) { color: var(--app-muted); font-size: 13px; }.breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) { color: var(--app-text); font-weight: 600; }.env-pill { display: inline-flex; align-items: center; gap: 7px; padding: 6px 10px; border-radius: 999px; background: #ecfdf5; color: #047857; font-size: 12px; }.env-pill i { width: 6px; height: 6px; border-radius: 50%; background: #10b981; }.profile-trigger { display: flex; align-items: center; gap: 9px; border: 0; background: transparent; color: var(--app-text); cursor: pointer; font-size: 13px; }.profile-trigger .el-icon { color: var(--app-muted); }
.page-main { overflow: auto; padding: 28px 32px 40px; background: var(--app-bg); }
@media (max-width: 900px) { .page-main { padding: 20px; }.env-pill,.topbar-right > .icon-button:first-of-type { display: none; }.topbar { padding: 0 18px; } }
</style>
