<template>
  <div>
    <h2>概览</h2>
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card>
          <p class="stat-label">我的应用</p>
          <p class="stat-value">{{ appCount }}</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <p class="stat-label">已上线接口</p>
          <p class="stat-value">{{ interfaceCount }}</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <p class="stat-label">当前用户</p>
          <p class="stat-value">{{ userStore.user?.userName || '-' }}</p>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="tips">
      <h3>快速开始</h3>
      <p>1. 在「应用管理」创建应用，获得 AccessKey / SecretKey</p>
      <p>2. 在「接口管理」查看可调用的开放接口</p>
      <p>3. 使用 SDK 或带签名请求调用接口（详见知识库 [[签名鉴权原理]]）</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useUserStore } from '@/store/user'
import { listApps, listInterfaces } from '@/api'

const userStore = useUserStore()
const appCount = ref(0)
const interfaceCount = ref(0)

onMounted(async () => {
  if (userStore.user) {
    const apps = await listApps(userStore.user.id)
    appCount.value = apps.length
  }
  const infos = await listInterfaces()
  interfaceCount.value = infos.length
})
</script>

<style scoped>
.stat-label {
  margin: 0;
  color: #909399;
}
.stat-value {
  margin: 8px 0 0;
  font-size: 28px;
  font-weight: 600;
}
.tips {
  margin-top: 16px;
}
</style>
