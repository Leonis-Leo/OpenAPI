<template>
  <div>
    <div class="toolbar">
      <h2>应用管理</h2>
    </div>
    <CollapsibleFilter @search="applySearch" @reset="resetFilters">
      <el-input
        v-model="keywordInput"
        placeholder="搜索应用名称 / AccessKey"
        clearable
        style="width: 240px"
        @input="onKeywordInput"
      />
      <template #more>
        <el-select v-model="statusFilter" clearable placeholder="状态" style="width: 120px" @change="onStatusChange">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </template>
    </CollapsibleFilter>

    <div class="action-bar">
      <div class="bar-left">
        <el-dropdown @command="handleExportCommand">
          <el-button size="small" type="primary" plain>
            导出
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="csv">应用 CSV</el-dropdown-item>
              <el-dropdown-item command="json">应用 JSON</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button size="small" type="primary" @click="openCreate">新建应用</el-button>
        <el-divider direction="vertical" />
        <el-button size="small" type="primary" plain :disabled="!selectedRow" @click="copySelected">复制AK</el-button>
        <el-button size="small" type="primary" plain :disabled="!selectedRow" @click="openRename(selectedRow)">重命名</el-button>
        <el-button
          size="small"
          :type="statusAction.target === 0 ? 'warning' : 'success'"
          :disabled="statusAction.disabled"
          @click="toggleOne(statusAction.target === 1)"
        >
          {{ statusAction.label }}
        </el-button>
        <el-dropdown @command="handleMoreCommand">
          <el-button size="small" plain>
            更多
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="reset-secret" :disabled="selected.length === 0">
                重置密钥
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <div class="bar-right">
        <span v-if="selected.length" class="batch-tip">已选 {{ selected.length }} 项</span>
        <el-button size="small" type="danger" :disabled="selected.length === 0" @click="handleDelete">删除</el-button>
      </div>
      <el-pagination
        class="bar-pagination"
        size="small"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        @current-change="handlePageChange"
        @size-change="handlePageChange"
      />
    </div>

    <el-table
      ref="tableRef"
      :data="pagedApps"
      border
      stripe
      v-loading="loading"
      @row-click="handleRowClick"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column type="index" label="#" width="60" :index="indexMethod" />
      <el-table-column label="应用名称" prop="appName" min-width="120" sortable>
        <template #default="{ row }">
          <el-link type="primary" @click="openDetail(row)">{{ row.appName }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="accessKey" label="AccessKey" min-width="220" show-overflow-tooltip />
      <el-table-column label="SecretKey" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">
          <el-text type="info" size="small" class="secret-text" @click.stop="toggleSecret(row.id)">
            {{ row.secretKey ? (showSecretIds.has(row.id) ? row.secretKey : maskSecret(row.secretKey)) : row.secretKeyHint }}
          </el-text>
          <el-icon class="secret-eye" @click.stop="toggleSecret(row.id)">
            <View v-if="showSecretIds.has(row.id)" />
            <Hide v-else />
          </el-icon>
          <el-button class="inline-copy" text circle size="small" @click.stop="copyText(row.secretKey)">
            <el-icon><CopyDocument /></el-icon>
          </el-button>
        </template>
      </el-table-column>
      <el-table-column
        prop="status"
        label="状态"
        width="80"
        sortable
        :filters="[
          { text: '启用', value: 1 },
          { text: '禁用', value: 0 }
        ]"
        :filter-method="filterAppStatus"
      >
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" sortable />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" aria-label="查看详情" title="查看详情" @click.stop="openDetail(row)">
            <el-icon><View /></el-icon>
          </el-button>
          <el-button size="small" text type="primary" aria-label="重命名" title="重命名" @click.stop="openRename(row)">
            <el-icon><Edit /></el-icon>
          </el-button>
          <el-button size="small" text type="danger" aria-label="删除" title="删除" @click.stop="handleDeleteRow(row)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '重命名应用' : '新建应用'" width="420px">
      <el-form label-width="80px">
        <el-form-item label="应用名称">
          <el-input v-model="appName" placeholder="请输入应用名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="keyVisible" title="密钥信息" width="560px" :close-on-click-modal="false">
      <el-alert
        type="warning"
        :closable="false"
        title="SecretKey 仅此一次展示，请立即复制并妥善保存"
        class="key-alert"
      />
      <el-descriptions :column="1" border class="key-desc">
        <el-descriptions-item label="应用名称">{{ keyInfo?.appName }}</el-descriptions-item>
        <el-descriptions-item label="AccessKey">
          <div class="key-line">
            <el-text>{{ keyInfo?.accessKey }}</el-text>
            <el-button size="small" type="primary" plain @click="copyText(keyInfo?.accessKey)">复制</el-button>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="SecretKey">
          <div class="key-line">
            <el-text class="key-secret">{{ keyInfo?.secretKey }}</el-text>
            <el-button size="small" type="primary" @click="copyText(keyInfo?.secretKey)">复制</el-button>
          </div>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="keyVisible = false">我已保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="`应用详情 - ${detailRow?.appName ?? ''}`" width="640px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="应用名称">{{ detailRow?.appName }}</el-descriptions-item>
        <el-descriptions-item label="AccessKey">
          <span class="key-line">
            {{ detailRow?.accessKey }}
            <el-button size="small" plain @click="copyText(detailRow?.accessKey)">复制</el-button>
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="SecretKey">
          <span class="key-line">
            <span class="secret-text" @click="toggleSecret(detailRow!.id)">
              {{ detailRow?.secretKey ? (showSecretIds.has(detailRow!.id) ? detailRow.secretKey : maskSecret(detailRow.secretKey)) : detailRow?.secretKeyHint }}
            </span>
            <el-icon class="secret-eye" @click="toggleSecret(detailRow!.id)">
              <View v-if="showSecretIds.has(detailRow!.id)" />
              <Hide v-else />
            </el-icon>
            <el-button size="small" plain @click="copyText(detailRow?.secretKey)">复制</el-button>
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ detailRow?.status === 1 ? '启用' : '禁用' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailRow?.createTime }}</el-descriptions-item>
      </el-descriptions>
      <div class="detail-list-head">
        <h4>已订阅接口</h4>
        <el-input
          v-model="detailSubKeyword"
          placeholder="搜索接口 / 路径"
          clearable
          size="small"
          style="width: 180px"
          @input="detailPage = 1"
        />
      </div>
      <el-table v-if="filteredSubscribes.length" :data="pagedSubscribes" border stripe size="small">
        <el-table-column prop="interfaceName" label="接口名称" />
        <el-table-column prop="interfaceUrl" label="路径" min-width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="subscribeType(row.status)" size="small">
              {{ subscribeText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170" />
      </el-table>
      <el-pagination
        v-if="filteredSubscribes.length > detailPageSize"
        class="dialog-pagination"
        size="small"
        layout="total, prev, pager, next"
        :total="filteredSubscribes.length"
        :page-size="detailPageSize"
        v-model:current-page="detailPage"
      />
      <el-empty v-else :description="appSubscribes.length ? '无匹配结果' : '暂无订阅'" :image-size="60" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableInstance } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, CopyDocument, Delete, Edit, Hide, View } from '@element-plus/icons-vue'
import CollapsibleFilter from '@/components/CollapsibleFilter.vue'
import { useUserStore } from '@/store/user'
import {
  pageApps,
  createApp,
  updateAppName,
  resetAppSecret,
  updateAppStatus,
  deleteApp,
  revealAppSecret,
  mySubscribes,
  type SubscribeInfo,
  type AppInfo
} from '@/api'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const apps = ref<AppInfo[]>([])
const loading = ref(false)
const keyword = ref('')
const keywordInput = ref('')
const statusFilter = ref<number>()
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selected = ref<AppInfo[]>([])
const tableRef = ref<TableInstance>()
const detailVisible = ref(false)
const detailRow = ref<AppInfo | null>(null)
const appSubscribes = ref<SubscribeInfo[]>([])
const detailPage = ref(1)
const detailPageSize = ref(5)
const detailSubKeyword = ref('')
const filteredSubscribes = computed(() => {
  const kw = detailSubKeyword.value.trim().toLowerCase()
  if (!kw) return appSubscribes.value
  return appSubscribes.value.filter(
    (sub) =>
      (sub.interfaceName || '').toLowerCase().includes(kw) ||
      (sub.interfaceUrl || '').toLowerCase().includes(kw)
  )
})
const pagedSubscribes = computed(() => {
  const start = (detailPage.value - 1) * detailPageSize.value
  return filteredSubscribes.value.slice(start, start + detailPageSize.value)
})
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const appName = ref('')
const saving = ref(false)
const keyVisible = ref(false)
const keyInfo = ref<AppInfo | null>(null)
const showSecretIds = ref<Set<number>>(new Set())

const selectedRow = computed(() => (selected.value.length === 1 ? selected.value[0] : null))
const indexMethod = (i: number) => (currentPage.value - 1) * pageSize.value + i + 1
const statusAction = computed(() => {
  const rows = selected.value
  if (rows.length === 0) return { label: '启用', disabled: true, target: 1 }
  const enabled = rows.filter((app) => app.status === 1)
  if (enabled.length === rows.length) return { label: '禁用', disabled: false, target: 0 }
  if (enabled.length === 0) return { label: '启用', disabled: false, target: 1 }
  return { label: '启用/禁用', disabled: true, target: 1 }
})
let keywordTimer: ReturnType<typeof setTimeout> | undefined

function exportApps() {
  const header = ['应用名称', 'AccessKey', '状态', '创建时间']
  const rows = apps.value.map((app) => [
    app.appName,
    app.accessKey,
    app.status === 1 ? '启用' : '禁用',
    app.createTime
  ])
  const escape = (value: string) => `"${String(value).replace(/"/g, '""')}"`
  const csv = '\uFEFF' + [header, ...rows].map((row) => row.map(escape).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `apps-${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('CSV 已导出当前页数据')
}

function filterAppStatus(value: number, row: AppInfo) {
  return row.status === value
}

function exportAppsJson() {
  const blob = new Blob([JSON.stringify(apps.value, null, 2)], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `apps-${new Date().toISOString().slice(0, 10)}.json`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('JSON 已导出当前页数据')
}

function handleExportCommand(command: string) {
  if (command === 'json') exportAppsJson()
  else exportApps()
}

function handleMoreCommand(command: string) {
  if (command === 'reset-secret') handleResetSecret()
}

function onKeywordInput() {
  clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => {
    keyword.value = keywordInput.value
    currentPage.value = 1
    load()
  }, 300)
}

function applyRouteFilters() {
  const query = route.query
  if (typeof query.keyword === 'string') {
    keywordInput.value = query.keyword
    keyword.value = query.keyword
  }
  if (typeof query.status === 'string') {
    const status = Number(query.status)
    statusFilter.value = Number.isInteger(status) ? status : undefined
  }
}

function syncRoute() {
  const query: Record<string, string> = {}
  if (keyword.value) query.keyword = keyword.value
  if (statusFilter.value !== undefined) query.status = String(statusFilter.value)
  router.replace({ query })
}

function applySearch() {
  clearTimeout(keywordTimer)
  keyword.value = keywordInput.value
  currentPage.value = 1
  load()
  syncRoute()
}

function resetFilters() {
  clearTimeout(keywordTimer)
  keywordInput.value = ''
  keyword.value = ''
  statusFilter.value = undefined
  currentPage.value = 1
  load()
  syncRoute()
}

const pagedApps = computed(() => apps.value)

function onStatusChange() {
  currentPage.value = 1
  load()
  syncRoute()
}

function handlePageChange() {
  clearSelection()
  load()
}

async function load() {
  loading.value = true
  try {
    if (userStore.user) {
      const result = await pageApps({
        current: currentPage.value,
        size: pageSize.value,
        keyword: keyword.value || undefined,
        status: statusFilter.value
      })
      apps.value = result.records
    total.value = Number(result.total)
    }
  } finally {
    loading.value = false
  }
}

async function copySelected() {
  const row = selectedRow.value
  if (!row) return
  const text = row.accessKey
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('AccessKey 已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

function openCreate() {
  editingId.value = null
  appName.value = ''
  dialogVisible.value = true
}

function openRename(row: AppInfo | null) {
  if (!row) return
  editingId.value = row.id
  appName.value = row.appName
  dialogVisible.value = true
}

async function handleSave() {
  if (!appName.value.trim()) {
    ElMessage.warning('请输入应用名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateAppName(editingId.value, appName.value.trim())
      ElMessage.success('已保存')
    } else {
      if (userStore.user) {
        const app = await createApp(appName.value.trim())
        keyInfo.value = app
        keyVisible.value = true
      }
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function handleResetSecret() {
  const rows = selected.value
  if (rows.length === 0) return
  const msg = rows.length > 1
    ? `确定重置选中的 ${rows.length} 个应用的 SecretKey 吗？旧密钥将失效`
    : `确定重置「${rows[0].appName}」的 SecretKey 吗？旧密钥将失效`
  await ElMessageBox.confirm(msg, '重置密钥', { type: 'warning' })
  const { value: currentPassword } = await ElMessageBox.prompt('请输入当前登录密码以确认重置', '二次验证', {
    inputType: 'password', inputPlaceholder: '当前登录密码', inputValidator: (v) => v ? true : '请输入密码'
  })
  if (rows.length === 1) {
    const app = await resetAppSecret(rows[0].id, currentPassword)
    keyInfo.value = app
    keyVisible.value = true
  } else {
    const results = await Promise.allSettled(rows.map((a) => resetAppSecret(a.id, currentPassword)))
    summarizeResults(results, rows.length, '重置密钥')
  }
  await load()
}

async function copyText(text?: string) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

function maskSecret(secret?: string): string {
  if (!secret) return ''
  if (secret.length <= 8) return '********'
  return secret.slice(0, 4) + '****' + secret.slice(-4)
}

async function toggleSecret(id: number) {
  const next = new Set(showSecretIds.value)
  if (next.has(id)) {
    next.delete(id)
    const row = apps.value.find((item) => item.id === id)
    if (row) row.secretKey = undefined
  } else {
    const row = apps.value.find((item) => item.id === id)
    if (!row) return
    if (!row.secretKey) {
      try {
        const revealed = await revealAppSecret(id)
        row.secretKey = revealed.secretKey
      } catch {
        return
      }
    }
    next.add(id)
  }
  showSecretIds.value = next
}

async function toggleOne(enabled: boolean) {
  const rows = selected.value
  if (rows.length === 0) return
  if (rows.length > 1) {
    await ElMessageBox.confirm(`确定对选中的 ${rows.length} 个应用执行「${enabled ? '启用' : '禁用'}」吗？`, '操作确认', { type: 'warning' })
  }
  const results = await Promise.allSettled(rows.map((a) => updateAppStatus(a.id, enabled)))
  summarizeResults(results, rows.length, enabled ? '启用' : '禁用')
  await load()
}

async function handleDelete() {
  const rows = selected.value
  if (rows.length === 0) return
  const msg = rows.length > 1
    ? `确定删除选中的 ${rows.length} 个应用吗？`
    : `确定删除应用「${rows[0].appName}」吗？`
  await ElMessageBox.confirm(msg, '删除应用', { type: 'warning' })
  const results = await Promise.allSettled(rows.map((a) => deleteApp(a.id)))
  summarizeResults(results, rows.length, '删除')
  await load()
}

async function handleDeleteRow(row: AppInfo) {
  selected.value = [row]
  await handleDelete()
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

function handleSelectionChange(rows: AppInfo[]) {
  selected.value = rows
}

function clearSelection() {
  selected.value = []
  tableRef.value?.clearSelection()
}

function handleRowClick(row: AppInfo) {
  tableRef.value?.toggleRowSelection(row)
}

function openDetail(row: AppInfo) {
  detailRow.value = row
  appSubscribes.value = []
  detailPage.value = 1
  detailSubKeyword.value = ''
  detailVisible.value = true
  loadSubscribes(row.id)
}

async function loadSubscribes(appId: number) {
  const subscribes = await mySubscribes()
  appSubscribes.value = subscribes.filter((s) => s.appId === appId)
}

function subscribeType(status: number) {
  return status === 1 ? 'success' : status === 2 ? 'danger' : 'warning'
}

function subscribeText(status: number) {
  return status === 1 ? '已订阅' : status === 2 ? '已拒绝' : '待审批'
}





onMounted(() => {
  applyRouteFilters()
  load()
})

onActivated(() => {
  applyRouteFilters()
  load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.toolbar-right {
  display: flex;
  gap: 8px;
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
.key-alert {
  margin-bottom: 12px;
}
.key-desc {
  margin-bottom: 4px;
}
.key-line {
  display: flex;
  align-items: center;
  gap: 8px;
}
.key-secret {
  font-family: Consolas, Monaco, monospace;
  word-break: break-all;
}
.secret-text {
  cursor: pointer;
  font-family: Consolas, Monaco, monospace;
}
.secret-eye {
  margin-left: 6px;
  cursor: pointer;
  vertical-align: middle;
  color: var(--el-text-color-secondary, #909399);
}
.sub-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.dialog-pagination {
  justify-content: flex-end;
  margin-top: 10px;
}
.detail-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 14px 0 8px;
}
.detail-list-head h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text, #172033);
}
</style>
