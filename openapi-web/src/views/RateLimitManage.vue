<template>
  <div>
    <div class="toolbar">
      <h2>限流配置</h2>
    </div>
    <p class="tip">
      两个限流维度：按应用（AccessKey）与按接口。为维度配置令牌桶（容量 + 每秒补充速率），
      启用后网关独立限流，任一维度拒绝即返回 429；未配置的维度不限流。
    </p>
    <el-alert
      v-if="dirtyCount > 0"
      type="warning"
      :closable="false"
      class="dirty-alert"
      :title="`有 ${dirtyCount} 处配置修改未保存，请点击「保存配置」`"
    />
    <el-tabs v-model="activeTab">
      <el-tab-pane label="按应用限流" name="app">
            <div class="action-bar">
      <el-button size="small" type="primary" :disabled="appSelected.length === 0" @click="handleSaveApp">保存配置</el-button>
      <el-button size="small" type="success" plain :disabled="appSelected.length === 0" @click="handleEnableApp(true)">批量启用</el-button>
      <el-button size="small" type="warning" plain :disabled="appSelected.length === 0" @click="handleEnableApp(false)">批量禁用</el-button>
      <el-button size="small" type="info" plain :disabled="appSelected.length < 2" @click="handleCopyApp">复制配置</el-button>
      <el-button class="danger-right" size="small" type="danger" plain :disabled="appSelected.length === 0" @click="handleDeleteApp">删除配置</el-button>
      <span v-if="appSelected.length" class="batch-tip">已选 {{ appSelected.length }} 项</span>
    </div>
        <el-table
          ref="appTableRef"
          :data="pagedAppList"
          border
          stripe
          v-loading="loading"
          @row-click="(row: AppRateLimitConfig) => appTableRef?.toggleRowSelection(row)"
          @selection-change="(rows: AppRateLimitConfig[]) => (appSelected = rows)"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column type="index" label="#" width="60" :index="appIndex" />
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
              <el-switch :model-value="row.enabled" @change="(v: boolean | string | number) => saveAppEnabled(row, v)" />
            </template>
          </el-table-column>
          <el-table-column label="容量" width="140">
            <template #default="{ row }">
              <el-input-number v-model="row.capacity" :min="1" :max="10000" @change="markDirtyApp(row)" />
            </template>
          </el-table-column>
          <el-table-column label="补充速率(个/秒)" width="160">
            <template #default="{ row }">
              <el-input-number v-model="row.refillRate" :min="1" :max="1000" @change="markDirtyApp(row)" />
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          class="pagination"
          layout="total, sizes, prev, pager, next, jumper"
          :total="appList.length"
          :page-sizes="[10, 20, 50, 100]"
          v-model:current-page="appPage"
          v-model:page-size="pageSize"
          @current-change="clearAppSelection"
          @size-change="clearAppSelection"
        />
      </el-tab-pane>
      <el-tab-pane label="按接口限流" name="interface">
            <div class="action-bar">
      <el-button size="small" type="primary" :disabled="interfaceSelected.length === 0" @click="handleSaveInterface">保存配置</el-button>
      <el-button size="small" type="success" plain :disabled="interfaceSelected.length === 0" @click="handleEnableInterface(true)">批量启用</el-button>
      <el-button size="small" type="warning" plain :disabled="interfaceSelected.length === 0" @click="handleEnableInterface(false)">批量禁用</el-button>
      <el-button size="small" type="info" plain :disabled="interfaceSelected.length < 2" @click="handleCopyInterface">复制配置</el-button>
      <el-button class="danger-right" size="small" type="danger" plain :disabled="interfaceSelected.length === 0" @click="handleDeleteInterface">删除配置</el-button>
      <span v-if="interfaceSelected.length" class="batch-tip">已选 {{ interfaceSelected.length }} 项</span>
    </div>
        <el-table
          ref="interfaceTableRef"
          :data="pagedInterfaceList"
          border
          stripe
          v-loading="loading"
          @row-click="(row: RateLimitConfig) => interfaceTableRef?.toggleRowSelection(row)"
          @selection-change="(rows: RateLimitConfig[]) => (interfaceSelected = rows)"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column type="index" label="#" width="60" :index="interfaceIndex" />
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
              <el-switch :model-value="row.enabled" @change="(v: boolean | string | number) => saveInterfaceEnabled(row, v)" />
            </template>
          </el-table-column>
          <el-table-column label="容量" width="140">
            <template #default="{ row }">
              <el-input-number v-model="row.capacity" :min="1" :max="10000" @change="markDirtyInterface(row)" />
            </template>
          </el-table-column>
          <el-table-column label="补充速率(个/秒)" width="160">
            <template #default="{ row }">
              <el-input-number v-model="row.refillRate" :min="1" :max="1000" @change="markDirtyInterface(row)" />
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          class="pagination"
          layout="total, sizes, prev, pager, next, jumper"
          :total="interfaceList.length"
          :page-sizes="[10, 20, 50, 100]"
          v-model:current-page="interfacePage"
          v-model:page-size="pageSize"
          @current-change="clearInterfaceSelection"
          @size-change="clearInterfaceSelection"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
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
const loading = ref(false)
const appSelected = ref<AppRateLimitConfig[]>([])
const interfaceSelected = ref<RateLimitConfig[]>([])
const appTableRef = ref<TableInstance>()
const interfaceTableRef = ref<TableInstance>()
const appPage = ref(1)
const interfacePage = ref(1)
const pageSize = ref(10)
const dirtyKeys = ref<Set<string>>(new Set())

const dirtyCount = computed(() => dirtyKeys.value.size)

function markDirtyApp(row: AppRateLimitConfig) {
  dirtyKeys.value = new Set(dirtyKeys.value).add(`app:${row.appId}`)
}

function markDirtyInterface(row: RateLimitConfig) {
  dirtyKeys.value = new Set(dirtyKeys.value).add(`iface:${row.interfaceId}`)
}

function clearDirty() {
  dirtyKeys.value = new Set()
}

const appRow = computed(() => (appSelected.value.length === 1 ? appSelected.value[0] : null))
const interfaceRow = computed(() =>
  interfaceSelected.value.length === 1 ? interfaceSelected.value[0] : null
)
const appIndex = (i: number) => (appPage.value - 1) * pageSize.value + i + 1
const interfaceIndex = (i: number) => (interfacePage.value - 1) * pageSize.value + i + 1

function clearAppSelection() {
  appSelected.value = []
  appTableRef.value?.clearSelection()
}

function clearInterfaceSelection() {
  interfaceSelected.value = []
  interfaceTableRef.value?.clearSelection()
}

const pagedAppList = computed(() => {
  const start = (appPage.value - 1) * pageSize.value
  return appList.value.slice(start, start + pageSize.value)
})

const pagedInterfaceList = computed(() => {
  const start = (interfacePage.value - 1) * pageSize.value
  return interfaceList.value.slice(start, start + pageSize.value)
})

watch(appList, () => {
  const max = Math.max(1, Math.ceil(appList.value.length / pageSize.value))
  if (appPage.value > max) {
    appPage.value = max
  }
})

watch(interfaceList, () => {
  const max = Math.max(1, Math.ceil(interfaceList.value.length / pageSize.value))
  if (interfacePage.value > max) {
    interfacePage.value = max
  }
})

async function load() {
  loading.value = true
  try {
    appList.value = await listAppRateLimitConfigs()
    interfaceList.value = await listRateLimitConfigs()
    clearDirty()
  } finally {
    loading.value = false
  }
}

async function handleSaveApp() {
  const rows = appSelected.value
  if (rows.length === 0) return
  const results = await Promise.allSettled(
    rows.map((r) => saveAppRateLimitConfig({ appId: r.appId, capacity: r.capacity, refillRate: r.refillRate, enabled: r.enabled }))
  )
  summarizeResults(results, rows.length, '保存')
  await load()
}

async function handleEnableApp(enabled: boolean) {
  const rows = appSelected.value
  if (rows.length === 0) return
  await ElMessageBox.confirm(
    `确定对选中的 ${rows.length} 个应用执行「${enabled ? '启用' : '禁用'}」限流吗？`,
    '操作确认',
    { type: 'warning' }
  )
  const results = await Promise.allSettled(
    rows.map((r) =>
      saveAppRateLimitConfig({
        appId: r.appId,
        capacity: r.capacity,
        refillRate: r.refillRate,
        enabled
      })
    )
  )
  summarizeResults(results, rows.length, enabled ? '启用' : '禁用')
  await load()
}

async function saveAppEnabled(row: AppRateLimitConfig, value: boolean | string | number) {
  const enabled = Boolean(value)
  row.enabled = enabled
  await saveAppRateLimitConfig({
    appId: row.appId,
    capacity: row.capacity,
    refillRate: row.refillRate,
    enabled
  })
  ElMessage.success(`已${enabled ? '启用' : '禁用'}`)
}

async function handleCopyApp() {
  const rows = appSelected.value
  if (rows.length < 2) return
  const source = rows[0]
  const targets = rows.slice(1).map((r) => ({
    appId: r.appId,
    capacity: source.capacity,
    refillRate: source.refillRate,
    enabled: source.enabled
  }))
  await ElMessageBox.confirm(
    `将「${source.appName}」的配置（容量 ${source.capacity}、速率 ${source.refillRate}）复制到其余 ${targets.length} 个应用吗？`,
    '复制配置',
    { type: 'warning' }
  )
  const results = await Promise.allSettled(targets.map((t) => saveAppRateLimitConfig(t)))
  summarizeResults(results, targets.length, '复制')
  await load()
}

async function handleDeleteApp() {
  const rows = appSelected.value
  if (rows.length === 0) return
  const msg = rows.length > 1
    ? `确定删除选中的 ${rows.length} 个应用的限流配置吗？`
    : `确定删除「${rows[0].appName}」的限流配置吗？`
  await ElMessageBox.confirm(msg, '删除配置', { type: 'warning' })
  const results = await Promise.allSettled(rows.map((r) => deleteAppRateLimitConfig(r.appId)))
  summarizeResults(results, rows.length, '删除')
  await load()
}

async function handleSaveInterface() {
  const rows = interfaceSelected.value
  if (rows.length === 0) return
  const results = await Promise.allSettled(
    rows.map((r) => saveRateLimitConfig({ interfaceId: r.interfaceId, capacity: r.capacity, refillRate: r.refillRate, enabled: r.enabled }))
  )
  summarizeResults(results, rows.length, '保存')
  await load()
}

async function handleEnableInterface(enabled: boolean) {
  const rows = interfaceSelected.value
  if (rows.length === 0) return
  await ElMessageBox.confirm(
    `确定对选中的 ${rows.length} 个接口执行「${enabled ? '启用' : '禁用'}」限流吗？`,
    '操作确认',
    { type: 'warning' }
  )
  const results = await Promise.allSettled(
    rows.map((r) =>
      saveRateLimitConfig({
        interfaceId: r.interfaceId,
        capacity: r.capacity,
        refillRate: r.refillRate,
        enabled
      })
    )
  )
  summarizeResults(results, rows.length, enabled ? '启用' : '禁用')
  await load()
}

async function saveInterfaceEnabled(row: RateLimitConfig, value: boolean | string | number) {
  const enabled = Boolean(value)
  row.enabled = enabled
  await saveRateLimitConfig({
    interfaceId: row.interfaceId,
    capacity: row.capacity,
    refillRate: row.refillRate,
    enabled
  })
  ElMessage.success(`已${enabled ? '启用' : '禁用'}`)
}

async function handleCopyInterface() {
  const rows = interfaceSelected.value
  if (rows.length < 2) return
  const source = rows[0]
  const targets = rows.slice(1).map((r) => ({
    interfaceId: r.interfaceId,
    capacity: source.capacity,
    refillRate: source.refillRate,
    enabled: source.enabled
  }))
  await ElMessageBox.confirm(
    `将「${source.interfaceName}」的配置（容量 ${source.capacity}、速率 ${source.refillRate}）复制到其余 ${targets.length} 个接口吗？`,
    '复制配置',
    { type: 'warning' }
  )
  const results = await Promise.allSettled(targets.map((t) => saveRateLimitConfig(t)))
  summarizeResults(results, targets.length, '复制')
  await load()
}

async function handleDeleteInterface() {
  const rows = interfaceSelected.value
  if (rows.length === 0) return
  const msg = rows.length > 1
    ? `确定删除选中的 ${rows.length} 个接口的限流配置吗？`
    : `确定删除「${rows[0].interfaceName}」的限流配置吗？`
  await ElMessageBox.confirm(msg, '删除配置', { type: 'warning' })
  const results = await Promise.allSettled(rows.map((r) => deleteRateLimitConfig(r.interfaceId)))
  summarizeResults(results, rows.length, '删除')
  await load()
}

function summarizeResults(
  results: PromiseSettledResult<unknown>[],
  total: number,
  action: string
) {
  const ok = results.filter((r) => r.status === 'fulfilled').length
  const fail = total - ok
  if (fail === 0) {
    ElMessage.success(`${action}成功 ${total} 项`)
  } else if (ok === 0) {
    ElMessage.error(`${action}失败 ${fail} 项`)
  } else {
    ElMessage.warning(`${action}成功 ${ok} 项，失败 ${fail} 项`)
  }
}

onMounted(load)
</script>

<style scoped>
.tip {
  color: #909399;
  font-size: 13px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.dirty-alert {
  margin-bottom: 12px;
}
.action-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.danger-right {
  margin-left: auto;
}
.batch-tip {
  color: #909399;
  font-size: 13px;
}
.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
