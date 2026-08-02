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
      <el-button size="small" :disabled="!selectedRow" @click="copySelected('ak')">复制AK</el-button>
      <el-button size="small" :disabled="!selectedRow" @click="copySelected('sk')">复制SK</el-button>
      <el-button size="small" :disabled="!selectedRow" @click="openRename(selectedRow)">重命名</el-button>
      <el-button size="small" :disabled="!selectedRow" @click="handleResetSecret(selectedRow)">重置密钥</el-button>
      <el-button size="small" :disabled="!selectedRow || selectedRow.status === 1" @click="toggleOne(true)">启用</el-button>
      <el-button size="small" :disabled="!selectedRow || selectedRow.status !== 1" @click="toggleOne(false)">禁用</el-button>
      <el-button size="small" type="danger" :disabled="!selectedRow" @click="handleDelete(selectedRow)">删除</el-button>
      <el-divider direction="vertical" />
      <el-button size="small" :disabled="selected.length === 0" @click="batchToggle(true)">批量启用</el-button>
      <el-button size="small" :disabled="selected.length === 0" @click="batchToggle(false)">批量禁用</el-button>
      <el-button size="small" type="danger" :disabled="selected.length === 0" @click="batchDelete">批量删除</el-button>
      <span v-if="selected.length" class="batch-tip">已选 {{ selected.length }} 项</span>
    </div>

    <el-table
      :data="pagedApps"
      border
      stripe
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="appName" label="应用名称" min-width="120" />
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  listApps,
  createApp,
  updateAppName,
  resetAppSecret,
  updateAppStatus,
  deleteApp,
  type AppInfo
} from '@/api'

const userStore = useUserStore()
const apps = ref<AppInfo[]>([])
const keyword = ref('')
const currentPage = ref(1)
const pageSize = 10
const selected = ref<AppInfo[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const appName = ref('')
const saving = ref(false)

const selectedRow = computed(() => (selected.value.length === 1 ? selected.value[0] : null))

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

async function handleResetSecret(row: AppInfo | null) {
  if (!row) return
  await ElMessageBox.confirm(`确定重置「${row.appName}」的 SecretKey 吗？旧密钥将失效`, '重置密钥', {
    type: 'warning'
  })
  const app = await resetAppSecret(row.id)
  ElMessage.success('已重置，新 SecretKey：' + app.secretKey)
  await load()
}

async function toggleOne(enabled: boolean) {
  const row = selectedRow.value
  if (!row) return
  await updateAppStatus(row.id, enabled)
  ElMessage.success(enabled ? '已启用' : '已禁用')
  await load()
}

async function handleDelete(row: AppInfo | null) {
  if (!row) return
  await ElMessageBox.confirm(`确定删除应用「${row.appName}」吗？`, '删除应用', {
    type: 'warning'
  })
  await deleteApp(row.id)
  ElMessage.success('已删除')
  await load()
}

function handleSelectionChange(rows: AppInfo[]) {
  selected.value = rows
}

async function batchToggle(enabled: boolean) {
  await Promise.all(selected.value.map((a) => updateAppStatus(a.id, enabled)))
  ElMessage.success(enabled ? '已批量启用' : '已批量禁用')
  await load()
}

async function batchDelete() {
  await ElMessageBox.confirm(`确定删除选中的 ${selected.value.length} 个应用吗？`, '批量删除', {
    type: 'warning'
  })
  await Promise.all(selected.value.map((a) => deleteApp(a.id)))
  ElMessage.success('已批量删除')
  await load()
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
</style>
