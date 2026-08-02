<template>
  <div>
    <div class="toolbar">
      <h2>应用管理</h2>
      <div class="toolbar-right">
        <el-input
          v-model="keyword"
          placeholder="搜索应用名称 / AccessKey"
          clearable
          style="width: 240px"
          @input="currentPage = 1"
        />
        <el-button type="primary" @click="openCreate">新建应用</el-button>
      </div>
    </div>

            <div class="action-bar">
      <el-button size="small" type="primary" plain :disabled="!selectedRow" @click="copySelected('ak')">复制AK</el-button>
      <el-button size="small" type="primary" plain :disabled="!selectedRow" @click="copySelected('sk')">复制SK</el-button>
      <el-button size="small" type="primary" plain :disabled="!selectedRow" @click="openRename(selectedRow)">重命名</el-button>
      <el-button size="small" type="warning" plain :disabled="selected.length === 0" @click="handleResetSecret">重置密钥</el-button>
      <el-button size="small" type="success" :disabled="selected.length === 0" @click="toggleOne(true)">启用</el-button>
      <el-button size="small" type="warning" :disabled="selected.length === 0" @click="toggleOne(false)">禁用</el-button>
      <el-button size="small" type="danger" :disabled="selected.length === 0" @click="handleDelete">删除</el-button>
      <span v-if="selected.length" class="batch-tip">已选 {{ selected.length }} 项</span>
    </div>

    <el-table
      ref="tableRef"
      :data="pagedApps"
      border
      stripe
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
          <el-text type="info" size="small">{{ row.secretKey }}</el-text>
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
      layout="total, prev, pager, next"
      :total="filteredApps.length"
      :page-size="pageSize"
      v-model:current-page="currentPage"
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

    <el-dialog v-model="detailVisible" :title="`应用详情 - ${detailRow?.appName ?? ''}`" width="640px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="应用名称">{{ detailRow?.appName }}</el-descriptions-item>
        <el-descriptions-item label="AccessKey">{{ detailRow?.accessKey }}</el-descriptions-item>
        <el-descriptions-item label="SecretKey">{{ detailRow?.secretKey }}</el-descriptions-item>
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
const keyword = ref('')
const currentPage = ref(1)
const pageSize = 10
const selected = ref<AppInfo[]>([])
const tableRef = ref<TableInstance>()
const detailVisible = ref(false)
const detailRow = ref<AppInfo | null>(null)
const appSubscribes = ref<SubscribeInfo[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const appName = ref('')
const saving = ref(false)

const selectedRow = computed(() => (selected.value.length === 1 ? selected.value[0] : null))
const indexMethod = (i: number) => (currentPage.value - 1) * pageSize + i + 1

const filteredApps = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return apps.value
  return apps.value.filter(
    (a) => a.appName.toLowerCase().includes(kw) || a.accessKey.toLowerCase().includes(kw)
  )
})

const pagedApps = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredApps.value.slice(start, start + pageSize)
})

watch(filteredApps, () => {
  const max = Math.max(1, Math.ceil(filteredApps.value.length / pageSize))
  if (currentPage.value > max) {
    currentPage.value = max
  }
})

async function load() {
  if (userStore.user) {
    apps.value = await listApps(userStore.user.id)
  }
}

async function copySelected(type: 'ak' | 'sk') {
  const row = selectedRow.value
  if (!row) return
  const text = type === 'ak' ? row.accessKey : row.secretKey
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`${type === 'ak' ? 'AccessKey' : 'SecretKey'} 已复制`)
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
        const app = await createApp(appName.value.trim(), userStore.user.id)
        ElMessage.success(`创建成功，SecretKey 请妥善保存（${app.accessKey}）`)
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
  await Promise.all(rows.map((a) => resetAppSecret(a.id)))
  ElMessage.success('已重置')
  await load()
}

async function toggleOne(enabled: boolean) {
  const rows = selected.value
  if (rows.length === 0) return
  if (rows.length > 1) {
    await ElMessageBox.confirm(`确定对选中的 ${rows.length} 个应用执行「${enabled ? '启用' : '禁用'}」吗？`, '操作确认', { type: 'warning' })
  }
  await Promise.all(rows.map((a) => updateAppStatus(a.id, enabled)))
  ElMessage.success(enabled ? '已启用' : '已禁用')
  await load()
}

async function handleDelete() {
  const rows = selected.value
  if (rows.length === 0) return
  const msg = rows.length > 1
    ? `确定删除选中的 ${rows.length} 个应用吗？`
    : `确定删除应用「${rows[0].appName}」吗？`
  await ElMessageBox.confirm(msg, '删除应用', { type: 'warning' })
  await Promise.all(rows.map((a) => deleteApp(a.id)))
  ElMessage.success('已删除')
  await load()
}

function handleSelectionChange(rows: AppInfo[]) {
  selected.value = rows
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
.batch-tip {
  color: #909399;
  font-size: 13px;
}
.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
.sub-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
