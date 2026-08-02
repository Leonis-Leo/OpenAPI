<template>
  <el-container class="layout">
    <el-aside width="200px">
      <div class="logo">OpenAPI 平台</div>
      <el-menu :default-active="route.path" router class="menu">
        <el-menu-item index="/dashboard">
          <span>概览</span>
        </el-menu-item>
        <el-menu-item index="/apps">
          <span>应用管理</span>
        </el-menu-item>
        <el-menu-item index="/interfaces">
          <span>接口管理</span>
        </el-menu-item>
        <el-menu-item index="/subscribes">
          <span>订阅审批</span>
        </el-menu-item>
        <el-menu-item index="/stats">
          <span>调用统计</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/ratelimit">
          <span>限流配置</span>
        </el-menu-item>
        <el-menu-item index="/logs">
          <span>API 日志</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="header-title">OpenAPI 开放平台管理后台</span>
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
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isDark = ref(document.documentElement.classList.contains('dark'))

function toggleTheme(value: boolean) {
  document.documentElement.classList.toggle('dark', value)
  localStorage.setItem('openapi-theme', value ? 'dark' : 'light')
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
.logo {
  height: 56px;
  line-height: 56px;
  text-align: center;
  font-weight: 600;
  color: #409eff;
  border-bottom: 1px solid #eee;
}
.menu {
  height: calc(100% - 56px);
  overflow-y: auto;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--el-border-color, #eee);
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-title {
  font-weight: 600;
}
.user {
  cursor: pointer;
  color: #409eff;
}
</style>
