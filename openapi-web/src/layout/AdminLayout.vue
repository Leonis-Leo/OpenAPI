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
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="header-title">OpenAPI 开放平台管理后台</span>
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
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

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
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #eee;
}
.header-title {
  font-weight: 600;
}
.user {
  cursor: pointer;
  color: #409eff;
}
</style>
