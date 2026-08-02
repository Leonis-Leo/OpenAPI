<template>
  <div>
    <h2>限流配置</h2>
    <p class="tip">
      两个限流维度：按应用（AccessKey）与按接口。为维度配置令牌桶（容量 + 每秒补充速率），
      启用后网关独立限流，任一维度拒绝即返回 429；未配置的维度不限流。
    </p>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="按应用限流" name="app">
        <el-table :data="appList" border stripe>
          <el-table-column prop="appId" label="ID" width="70" />
          <el-table-column prop="appName" label="应用名称" />
          <el-table-column prop="accessKey" label="AccessKey" min-width="200" show-overflow-tooltip />
          <el-table-column label="配置状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.configured" type="success" size="small">已配置</el-tag>
              <el-tag v-else type="info" size="small">未配置</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="启用" width="90">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" />
            </template>
          </el-table-column>
          <el-table-column label="容量" width="140">
            <template #default="{ row }">
              <el-input-number v-model="row.capacity" :min="1" :max="10000" />
            </template>
          </el-table-column>
          <el-table-column label="补充速率(个/秒)" width="160">
            <template #default="{ row }">
              <el-input-number v-model="row.refillRate" :min="1" :max="1000" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" type="primary" plain @click="handleSaveApp(row)">保存</el-button>
              <el-button size="small" @click="handleDeleteApp(row)">删除配置</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="按接口限流" name="interface">
        <el-table :data="interfaceList" border stripe>
          <el-table-column prop="interfaceId" label="ID" width="70" />
          <el-table-column prop="interfaceName" label="接口名称" />
          <el-table-column prop="method" label="方式" width="80">
            <template #default="{ row }">
              <el-tag :type="row.method === 'GET' ? 'success' : 'warning'">{{ row.method }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="url" label="路径" min-width="180" />
          <el-table-column label="配置状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.configured" type="success" size="small">已配置</el-tag>
              <el-tag v-else type="info" size="small">未配置</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="启用" width="90">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" />
            </template>
          </el-table-column>
          <el-table-column label="容量" width="140">
            <template #default="{ row }">
              <el-input-number v-model="row.capacity" :min="1" :max="10000" />
            </template>
          </el-table-column>
          <el-table-column label="补充速率(个/秒)" width="160">
            <template #default="{ row }">
              <el-input-number v-model="row.refillRate" :min="1" :max="1000" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" type="primary" plain @click="handleSaveInterface(row)">保存</el-button>
              <el-button size="small" @click="handleDeleteInterface(row)">删除配置</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listRateLimitConfigs,
  saveRateLimitConfig,
  deleteRateLimitConfig,
  listAppRateLimitConfigs,
  saveAppRateLimitConfig,
  deleteAppRateLimitConfig,
  type RateLimitConfig,
  type AppRateLimitConfig
} from '@/api'

const activeTab = ref('app')
const appList = ref<AppRateLimitConfig[]>([])
const interfaceList = ref<RateLimitConfig[]>([])

async function load() {
  appList.value = await listAppRateLimitConfigs()
  interfaceList.value = await listRateLimitConfigs()
}

async function handleSaveApp(row: AppRateLimitConfig) {
  await saveAppRateLimitConfig({
    appId: row.appId,
    capacity: row.capacity,
    refillRate: row.refillRate,
    enabled: row.enabled
  })
  ElMessage.success('已保存')
  await load()
}

async function handleDeleteApp(row: AppRateLimitConfig) {
  await ElMessageBox.confirm(`确定删除「${row.appName}」的限流配置吗？`, '删除配置', {
    type: 'warning'
  })
  await deleteAppRateLimitConfig(row.appId)
  ElMessage.success('已删除')
  await load()
}

async function handleSaveInterface(row: RateLimitConfig) {
  await saveRateLimitConfig({
    interfaceId: row.interfaceId,
    capacity: row.capacity,
    refillRate: row.refillRate,
    enabled: row.enabled
  })
  ElMessage.success('已保存')
  await load()
}

async function handleDeleteInterface(row: RateLimitConfig) {
  await ElMessageBox.confirm(`确定删除「${row.interfaceName}」的限流配置吗？`, '删除配置', {
    type: 'warning'
  })
  await deleteRateLimitConfig(row.interfaceId)
  ElMessage.success('已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.tip {
  color: #909399;
  font-size: 13px;
}
</style>
