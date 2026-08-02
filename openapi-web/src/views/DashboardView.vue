<template>
  <div>
    <div class="toolbar">
      <h2>概览</h2>
      <span class="welcome">你好，{{ userStore.user?.userName || userStore.user?.userAccount || '用户' }} 👋</span>
    </div>
    <el-row :gutter="16">
      <el-col :span="4">
        <el-card class="stat-card clickable" shadow="hover" @click="router.push('/apps')">
          <p class="stat-label">我的应用</p>
          <p class="stat-value">{{ appCount }}</p>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card clickable" shadow="hover" @click="router.push('/subscribes')">
          <p class="stat-label">已订阅接口</p>
          <p class="stat-value">{{ subscribeCount }}</p>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card clickable" shadow="hover" @click="router.push('/interfaces')">
          <p class="stat-label">可调用接口</p>
          <p class="stat-value">{{ interfaceCount }}</p>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card clickable" shadow="hover" @click="router.push('/stats')">
          <p class="stat-label">累计调用</p>
          <p class="stat-value">{{ stats.total }}</p>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card clickable" shadow="hover" @click="router.push('/stats')">
          <p class="stat-label">成功率</p>
          <p class="stat-value success">{{ stats.successRate }}%</p>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card clickable" shadow="hover" @click="router.push('/subscribes')">
          <p class="stat-label">待审批订阅</p>
          <p class="stat-value warning">{{ pendingCount }}</p>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="tips">
      <h3>快速开始</h3>
      <p>1. 在「应用管理」创建应用，获得 AccessKey / SecretKey</p>
      <p>2. 在「接口管理」查看可调用的开放接口</p>
      <p>3. 使用 SDK 或带签名请求调用接口（详见项目知识库）</p>
    </el-card>
    <el-card class="tips">
      <h3>快捷入口</h3>
      <div class="quick-links">
        <el-link type="primary" @click="router.push('/apps')">应用管理</el-link>
        <el-link type="primary" @click="router.push('/interfaces')">接口管理</el-link>
        <el-link type="primary" @click="router.push('/stats')">调用统计</el-link>
        <el-link type="primary" @click="router.push('/logs')">API 日志</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { listApps, listInterfaces, mySubscribes, listSubscribes, statsOverview, type StatsOverview } from '@/api'

const router = useRouter()
const userStore = useUserStore()
const appCount = ref(0)
const interfaceCount = ref(0)
const subscribeCount = ref(0)
const pendingCount = ref(0)
const stats = ref<StatsOverview>({ total: 0, success: 0, fail: 0, successRate: 0 })

onMounted(async () => {
  if (userStore.user) {
    const apps = await listApps(userStore.user.id)
    appCount.value = apps.length
    const subscribes = await mySubscribes()
    subscribeCount.value = subscribes.filter((s) => s.status === 1).length
  }
  const infos = await listInterfaces()
  interfaceCount.value = infos.length
  stats.value = await statsOverview()
  if (userStore.user?.userRole === 'admin') {
    const pending = await listSubscribes(0)
    pendingCount.value = pending.length
  }
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.welcome {
  color: #909399;
  font-size: 14px;
}
.stat-label {
  margin: 0;
  color: #909399;
}
.stat-value {
  margin: 8px 0 0;
  font-size: 28px;
  font-weight: 600;
}
.stat-value.success {
  color: #67c23a;
}
.stat-value.warning {
  color: #e6a23c;
}
.clickable {
  cursor: pointer;
}
.tips {
  margin-top: 16px;
}
.quick-links {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}
</style>
