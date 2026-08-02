<template>
  <div>
    <h2>限流配置</h2>
    <p class="tip">
      两个限流维度：按应用（AccessKey）与按接口。为维度配置令牌桶（容量 + 每秒补充速率），
      启用后网关独立限流，任一维度拒绝即返回 429；未配置的维度不限流。
    </p>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="按应用限流" name="app">
            <div class="action-bar">
      <el-button size="small" type="primary" :disabled="appSelected.length === 0" @click="handleSaveApp">保存配置</el-button>
      <el-button size="small" type="danger" plain :disabled="appSelected.length === 0" @click="handleDeleteApp">删除配置</el-button>
      <span v-if="appSelected.length" class="batch-tip">已选 {{ appSelected.length }} 项</span>
    </div>
        <el-table
          ref="appTableRef"
          :data="appList"
          border
          stripe
          @row-click="(row: AppRateLimitConfig) => appTableRef?.toggleRowSelection(row)"
          @selection-change="(rows: AppRateLimitConfig[]) => (appSelected = rows)"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column type="index" label="#" width="60" />
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
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="按接口限流" name="interface">
            <div class="action-bar">
      <el-button size="small" type="primary" :disabled="interfaceSelected.length === 0" @click="handleSaveInterface">保存配置</el-button>
      <el-button size="small" type="danger" plain :disabled="interfaceSelected.length === 0" @click="handleDeleteInterface">删除配置</el-button>
      <span v-if="interfaceSelected.length" class="batch-tip">已选 {{ interfaceSelected.length }} 项</span>
    </div>
        <el-table
          ref="interfaceTableRef"
          :data="interfaceList"
          border
          stripe
          @row-click="(row: RateLimitConfig) => interfaceTableRef?.toggleRowSelection(row)"
          @selection-change="(rows: RateLimitConfig[]) => (interfaceSelected = rows)"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column type="index" label="#" width="60" />
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
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableInstance } from 'element-plus'
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
const appSelected = ref<AppRateLimitConfig[]>([])
const interfaceSelected = ref<RateLimitConfig[]>([])
const appTableRef = ref<TableInstance>()
const interfaceTableRef = ref<TableInstance>()

const appRow = computed(() => (appSelected.value.length === 1 ? appSelected.value[0] : null))
const interfaceRow = computed(() =>
  interfaceSelected.value.length === 1 ? interfaceSelected.value[0] : null
)

async function load() {
  appList.value = await listAppRateLimitConfigs()
  interfaceList.value = await listRateLimitConfigs()
}

async function handleSaveApp() {
  const rows = appSelected.value
  if (rows.length === 0) return
  await Promise.all(rows.map((r) => saveAppRateLimitConfig({ appId: r.appId, capacity: r.capacity, refillRate: r.refillRate, enabled: r.enabled })))
  ElMessage.success('已保存')
  await load()
}

async function handleDeleteApp() {
  const rows = appSelected.value
  if (rows.length === 0) return
  const msg = rows.length > 1
    ? `确定删除选中的 ${rows.length} 个应用的限流配置吗？`
    : `确定删除「${rows[0].appName}」的限流配置吗？`
  await ElMessageBox.confirm(msg, '删除配置', { type: 'warning' })
  await Promise.all(rows.map((r) => deleteAppRateLimitConfig(r.appId)))
  ElMessage.success('已删除')
  await load()
}

async function handleSaveInterface() {
  const rows = interfaceSelected.value
  if (rows.length === 0) return
  await Promise.all(rows.map((r) => saveRateLimitConfig({ interfaceId: r.interfaceId, capacity: r.capacity, refillRate: r.refillRate, enabled: r.enabled })))
  ElMessage.success('已保存')
  await load()
}

async function handleDeleteInterface() {
  const rows = interfaceSelected.value
  if (rows.length === 0) return
  const msg = rows.length > 1
    ? `确定删除选中的 ${rows.length} 个接口的限流配置吗？`
    : `确定删除「${rows[0].interfaceName}」的限流配置吗？`
  await ElMessageBox.confirm(msg, '删除配置', { type: 'warning' })
  await Promise.all(rows.map((r) => deleteRateLimitConfig(r.interfaceId)))
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
.action-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.batch-tip {
  color: #909399;
  font-size: 13px;
}
</style>
