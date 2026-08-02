<template>
  <el-container class="layout">
    <el-aside :width="isCollapse ? '64px' : '200px'" class="aside">
      <div class="logo">{{ isCollapse ? 'OP' : 'OpenAPI 平台' }}</div>
      <el-menu
        :default-active="route.path"
        router
        :collapse="isCollapse"
        :collapse-transition="false"
        class="menu"
      >
        <el-menu-item index="/dashboard" :title="'概览'">
          <el-icon><HomeFilled /></el-icon>
          <span>概览</span>
        </el-menu-item>
        <el-menu-item index="/apps" :title="'应用管理'">
          <el-icon><Box /></el-icon>
          <span>应用管理</span>
        </el-menu-item>
        <el-menu-item index="/interfaces" :title="'接口管理'">
          <el-icon><Connection /></el-icon>
          <span>接口管理</span>
        </el-menu-item>
        <el-menu-item index="/subscribes" :title="'订阅审批'">
          <el-icon><DocumentChecked /></el-icon>
          <span>订阅审批</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/stats" :title="'调用统计'">
          <el-icon><TrendCharts /></el-icon>
          <span>调用统计</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/users" :title="'用户管理'">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/ratelimit" :title="'限流配置'">
          <el-icon><Timer /></el-icon>
          <span>限流配置</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/logs" :title="'API 日志'">
          <el-icon><Document /></el-icon>
          <span>API 日志</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleCollapse">
            <Expand v-if="isCollapse" />
            <Fold v-else />
          </el-icon>
          <span class="header-title">OpenAPI 开放平台管理后台</span>
          <el-breadcrumb class="breadcrumb" separator="/">
            <el-breadcrumb-item>首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-switch
            v-model="isDark"
            inline-prompt
            active-text="暗"
            inactive-text="亮"
            @change="toggleTheme"
          />
          <el-dropdown @command="handleCommand">
            <span class="user">
              {{ userStore.user?.userName || userStore.user?.userAccount || '用户' }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main>
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="profileVisible" title="个人中心" width="420px">
    <el-form label-width="80px">
      <el-form-item label="账号">
        <el-input :model-value="userStore.user?.userAccount" disabled />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="profileForm.userName" placeholder="请输入昵称" />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input
          v-model="profileForm.userPassword"
          type="password"
          show-password
          placeholder="留空则不修改密码"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="profileVisible = false">取消</el-button>
      <el-button type="primary" :loading="savingProfile" @click="handleSaveProfile">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Box,
  Connection,
  Document,
  DocumentChecked,
  Expand,
  Fold,
  HomeFilled,
  Timer,
  TrendCharts,
  User
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { logout as logoutApi, selfUpdate } from '@/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isAdmin = userStore.user?.userRole === 'admin'
const isDark = ref(document.documentElement.classList.contains('dark'))
const isCollapse = ref(localStorage.getItem('openapi-sidebar') === '1')
const profileVisible = ref(false)
const savingProfile = ref(false)
const profileForm = reactive({ userName: '', userPassword: '' })

function toggleTheme(value: boolean) {
  document.documentElement.classList.toggle('dark', value)
  localStorage.setItem('openapi-theme', value ? 'dark' : 'light')
}

function toggleCollapse() {
  isCollapse.value = !isCollapse.value
  localStorage.setItem('openapi-sidebar', isCollapse.value ? '1' : '0')
}

async function handleCommand(command: string) {
  if (command === 'logout') {
    try {
      await logoutApi()
    } catch {
      // 忽略退出接口异常，本地照常清理
    }
    userStore.logout()
    window.location.href = '/login'
  } else if (command === 'profile') {
    profileForm.userName = userStore.user?.userName ?? ''
    profileForm.userPassword = ''
    profileVisible.value = true
  }
}

async function handleSaveProfile() {
  savingProfile.value = true
  try {
    await selfUpdate({
      userName: profileForm.userName.trim() || undefined,
      userPassword: profileForm.userPassword || undefined
    })
    if (userStore.user && profileForm.userName.trim()) {
      userStore.user.userName = profileForm.userName.trim()
      localStorage.setItem('openapi_user', JSON.stringify(userStore.user))
    }
    ElMessage.success('已保存')
    profileVisible.value = false
  } finally {
    savingProfile.value = false
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}
.aside {
  background: #304156;
  transition: width 0.2s;
  overflow: hidden;
}
.logo {
  height: 56px;
  line-height: 56px;
  text-align: center;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
}
.menu {
  height: calc(100% - 56px);
  overflow: hidden;
  border-right: none;
  background: transparent;
}
.menu :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  color: #bfcbd9;
}
.menu :deep(.el-menu-item:hover) {
  background: #263445;
  color: #fff;
}
.menu :deep(.el-menu-item.is-active) {
  background: var(--el-color-primary, #409eff);
  color: #fff;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--el-border-color, #eee);
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: var(--el-text-color-primary, #303133);
}
.header-title {
  font-weight: 600;
}
.breadcrumb {
  margin-left: 12px;
  padding-left: 12px;
  border-left: 1px solid var(--el-border-color, #eee);
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user {
  cursor: pointer;
  color: var(--el-color-primary, #409eff);
}
</style>
