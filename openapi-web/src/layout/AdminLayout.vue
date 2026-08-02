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
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>概览</span>
        </el-menu-item>
        <el-menu-item index="/apps">
          <el-icon><Box /></el-icon>
          <span>应用管理</span>
        </el-menu-item>
        <el-menu-item index="/interfaces">
          <el-icon><Connection /></el-icon>
          <span>接口管理</span>
        </el-menu-item>
        <el-menu-item index="/subscribes">
          <el-icon><DocumentChecked /></el-icon>
          <span>订阅审批</span>
        </el-menu-item>
        <el-menu-item index="/stats">
          <el-icon><TrendCharts /></el-icon>
          <span>调用统计</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/ratelimit">
          <el-icon><Timer /></el-icon>
          <span>限流配置</span>
        </el-menu-item>
        <el-menu-item index="/logs">
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
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isDark = ref(document.documentElement.classList.contains('dark'))
const isCollapse = ref(localStorage.getItem('openapi-sidebar') === '1')

function toggleTheme(value: boolean) {
  document.documentElement.classList.toggle('dark', value)
  localStorage.setItem('openapi-theme', value ? 'dark' : 'light')
}

function toggleCollapse() {
  isCollapse.value = !isCollapse.value
  localStorage.setItem('openapi-sidebar', isCollapse.value ? '1' : '0')
}

function handleCommand(command: string) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
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
