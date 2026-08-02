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

    <div class="batch-bar">
      <el-button size="small" :disabled="selectedApps.length === 0" @click="batchToggle(true)">
        批量启用
      </el-button>
      <el-button size="small" :disabled="selectedApps.length === 0" @click="batchToggle(false)">
        批量禁用
      </el-button>
      <el-button size="small" type="danger" :disabled="selectedApps.length === 0" @click="batchDelete">
        批量删除
      </el-button>
      <span v-if="selectedApps.length" class="batch-tip">已选 {{ selectedApps.length }} 项</span>
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
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button size="small" @click="copy(row.accessKey, 'AccessKey')">复制AK</el-button>
          <el-button size="small" @click="copy(row.secretKey, 'SecretKey')">复制SK</el-button>
          <el-dropdown trigger="click" @command="(cmd: string) => handleCommand(cmd, row)">
            <el-button size="small" type="primary" plain>
              更多<el-icon><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="rename">重命名</el-dropdown-item>
                <el-dropdown-item command="reset">重置密钥</el-dropdown-item>
                <el-dropdown-item command="toggle">
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
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
import { ArrowDown } from '@element-plus/icons-vue'
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
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const appName = ref('')
const saving = ref(false)
const selectedApps = ref<AppInfo[]>([])

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

async function copy(text: string, label: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`${label} 已复制`)
  } catch {
    ElMessage.error('复制失败')
  }
}

function openCreate() {
  editingId.value = null
  appName.value = ''
  dialogVisible.value = true
}

function openRename(row: AppInfo) {
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

async function handleCommand(cmd: string, row: AppInfo) {
  if (cmd === 'rename') {
    openRename(row)
  } else if (cmd === 'reset') {
    await ElMessageBox.confirm(`确定重置「${row.appName}」的 SecretKey 吗？旧密钥将失效`, '重置密钥', {
      type: 'warning'
    })
    const app = await resetAppSecret(row.id)
    ElMessage.success('已重置，新 SecretKey：' + app.secretKey)
    await load()
  } else if (cmd === 'toggle') {
    await updateAppStatus(row.id, row.status !== 1)
    ElMessage.success(row.status === 1 ? '已禁用' : '已启用')
    await load()
  } else if (cmd === 'delete') {
    await ElMessageBox.confirm(`确定删除应用「${row.appName}」吗？`, '删除应用', {
      type: 'warning'
    })
    await deleteApp(row.id)
    ElMessage.success('已删除')
    await load()
  }
}

function handleSelectionChange(rows: AppInfo[]) {
  selectedApps.value = rows
}

async function batchToggle(enabled: boolean) {
  await Promise.all(selectedApps.value.map((a) => updateAppStatus(a.id, enabled)))
  ElMessage.success(enabled ? '已批量启用' : '已批量禁用')
  await load()
}

async function batchDelete() {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedApps.value.length} 个应用吗？`, '批量删除', {
    type: 'warning'
  })
  await Promise.all(selectedApps.value.map((a) => deleteApp(a.id)))
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
.batch-bar {
  display: flex;
  align-items: center;
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
