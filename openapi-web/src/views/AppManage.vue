<template>
  <div>
    <div class="toolbar">
      <h2>应用管理</h2>
      <div class="toolbar-right">
        <el-input
          v-model="keywordInput"
          placeholder="搜索应用名称 / AccessKey"
          clearable
          style="width: 240px"
          @input="onKeywordInput"
        />
        <el-button type="primary" @click="openCreate">新建应用</el-button>
      </div>
    </div>

            <div class="action-bar">
      <el-button size="small" type="primary" plain :disabled="!selectedRow" @click="copySelected">复制AK</el-button>
      <el-button size="small" type="primary" plain :disabled="!selectedRow" @click="openRename(selectedRow)">重命名</el-button>
      <el-button size="small" type="warning" plain :disabled="selected.length === 0" @click="handleResetSecret">重置密钥</el-button>
      <el-button size="small" type="success" :disabled="selected.length === 0" @click="toggleOne(true)">启用</el-button>
      <el-button size="small" type="warning" :disabled="selected.length === 0" @click="toggleOne(false)">禁用</el-button>
      <el-button class="danger-right" size="small" type="danger" :disabled="selected.length === 0" @click="handleDelete">删除</el-button>
      <span v-if="selected.length" class="batch-tip">已选 {{ selected.length }} 项</span>
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
      <el-table-column label="应用名称" min-width="120">
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
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
    </el-table>

    <el-pagination
      class="pagination"
      layout="total, sizes, prev, pager, next, jumper"
      :total="filteredApps.length"
      :page-sizes="[10, 20, 50, 100]"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      @current-change="clearSelection"
      @size-change="clearSelection"
    />

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
      <h4>已订阅接口</h4>
      <el-table v-if="appSubscribes.length" :data="appSubscribes" border stripe size="small">
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
      <el-empty v-else description="暂无订阅" :image-size="60" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableInstance } from 'element-plus'
import { Hide, View } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import {
  listApps,
  createApp,
  updateAppName,
  resetAppSecret,
  updateAppStatus,
  deleteApp,
  mySubscribes,
  type SubscribeInfo,
  type AppInfo
} from '@/api'

const userStore = useUserStore()
const apps = ref<AppInfo[]>([])
const loading = ref(false)
const keyword = ref('')
const keywordInput = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const selected = ref<AppInfo[]>([])
const tableRef = ref<TableInstance>()
const detailVisible = ref(false)
const detailRow = ref<AppInfo | null>(null)
const appSubscribes = ref<SubscribeInfo[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const appName = ref('')
const saving = ref(false)
const keyVisible = ref(false)
const keyInfo = ref<AppInfo | null>(null)
const showSecretIds = ref<Set<number>>(new Set())

const selectedRow = computed(() => (selected.value.length === 1 ? selected.value[0] : null))
const indexMethod = (i: number) => (currentPage.value - 1) * pageSize.value + i + 1
let keywordTimer: ReturnType<typeof setTimeout> | undefined

function onKeywordInput() {
  clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => {
    keyword.value = keywordInput.value
    currentPage.value = 1
  }, 300)
}

const filteredApps = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return apps.value
  return apps.value.filter(
    (a) => a.appName.toLowerCase().includes(kw) || a.accessKey.toLowerCase().includes(kw)
  )
})

const pagedApps = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredApps.value.slice(start, start + pageSize.value)
})

watch(filteredApps, () => {
  const max = Math.max(1, Math.ceil(filteredApps.value.length / pageSize.value))
  if (currentPage.value > max) {
    currentPage.value = max
  }
})

async function load() {
  loading.value = true
  try {
    if (userStore.user) {
      apps.value = await listApps()
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
  if (rows.length === 1) {
    const app = await resetAppSecret(rows[0].id)
    keyInfo.value = app
    keyVisible.value = true
  } else {
    const results = await Promise.allSettled(rows.map((a) => resetAppSecret(a.id)))
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

function toggleSecret(id: number) {
  const next = new Set(showSecretIds.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
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





onMounted(load)
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
</style>
