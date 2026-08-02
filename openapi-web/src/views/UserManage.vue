<template>
  <div>
    <div class="toolbar">
      <h2>用户管理</h2>
      <div class="toolbar-right">
        <el-input
          v-model="keyword"
          placeholder="搜索账号 / 昵称"
          clearable
          style="width: 220px"
          @input="currentPage = 1"
        />
        <el-button type="primary" @click="openCreate">新增用户</el-button>
      </div>
    </div>

            <div class="action-bar">
      <el-button size="small" type="primary" plain :disabled="!selectedRow" @click="openEdit(selectedRow)">编辑</el-button>
      <el-button size="small" type="danger" plain :disabled="selected.length === 0" @click="toggleRole('admin')">设为管理员</el-button>
      <el-button size="small" type="primary" plain :disabled="selected.length === 0" @click="toggleRole('user')">设为普通用户</el-button>
      <el-button size="small" type="warning" plain :disabled="selected.length === 0" @click="openResetPassword">重置密码</el-button>
      <el-button size="small" type="success" :disabled="selected.length === 0" @click="toggleStatus(true)">启用</el-button>
      <el-button size="small" type="danger" :disabled="selected.length === 0" @click="toggleStatus(false)">禁用</el-button>
      <el-button class="danger-right" size="small" type="danger" plain :disabled="selected.length === 0" @click="handleDelete">删除</el-button>
      <span v-if="selected.length" class="batch-tip">已选 {{ selected.length }} 项</span>
    </div>

    <el-table
      ref="tableRef"
      :data="pagedUsers"
      border
      stripe
      v-loading="loading"
      @row-click="handleRowClick"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column type="index" label="#" width="60" :index="indexMethod" />
      <el-table-column label="账号">
        <template #default="{ row }">
          <el-link type="primary" @click="openDetail(row)">{{ row.userAccount }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="userName" label="昵称" />
      <el-table-column prop="userRole" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.userRole === 'admin' ? 'danger' : 'info'">
            {{ row.userRole === 'admin' ? '管理员' : '普通用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
    </el-table>

    <el-pagination
      class="pagination"
      layout="total, sizes, prev, pager, next, jumper"
      :total="filteredUsers.length"
      :page-sizes="[10, 20, 50, 100]"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      @current-change="clearSelection"
      @size-change="clearSelection"
    />

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑用户' : '新增用户'" width="420px">
      <el-form label-width="80px">
        <el-form-item v-if="!editingId" label="账号">
          <el-input v-model="userForm.userAccount" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="userForm.userPassword"
            type="password"
            show-password
            :placeholder="editingId ? '留空则不修改密码' : '请输入密码'"
          />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="userForm.userName" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="角色">
          <el-select v-model="userForm.role" style="width: 100%">
            <el-option label="普通用户" value="user" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="`用户详情 - ${detailRow?.userAccount ?? ''}`" width="520px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="账号">{{ detailRow?.userAccount }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detailRow?.userName }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          {{ detailRow?.userRole === 'admin' ? '管理员' : '普通用户' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ detailRow?.status === 1 ? '启用' : '禁用' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailRow?.createTime }}</el-descriptions-item>
      </el-descriptions>
      <h4>所属应用</h4>
      <el-table v-if="detailApps.length" :data="detailApps" border stripe size="small">
        <el-table-column prop="appName" label="应用名称" />
        <el-table-column prop="accessKey" label="AccessKey" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
      </el-table>
      <el-empty v-else description="暂无应用" :image-size="60" />
    </el-dialog>

    <el-dialog v-model="resetVisible" :title="`重置密码（${resetTargets.length} 人）`" width="420px">
      <el-form label-width="90px">
        <el-form-item label="新密码">
          <el-input
            v-model="resetPassword"
            type="password"
            show-password
            placeholder="请输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="handleResetPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableInstance } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  listUsers,
  listApps,
  updateUserRole,
  updateUserStatus,
  createUser,
  updateUser,
  deleteUser,
  type AppInfo,
  type UserInfo
} from '@/api'

const userStore = useUserStore()
const users = ref<UserInfo[]>([])
const loading = ref(false)
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const selected = ref<UserInfo[]>([])
const tableRef = ref<TableInstance>()
const detailVisible = ref(false)
const detailRow = ref<UserInfo | null>(null)
const detailApps = ref<AppInfo[]>([])
const resetVisible = ref(false)
const resetTargets = ref<UserInfo[]>([])
const resetPassword = ref('')
const resetting = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const userForm = ref({
  userAccount: '',
  userPassword: '',
  userName: '',
  role: 'user'
})

const selectedRow = computed(() => (selected.value.length === 1 ? selected.value[0] : null))
const indexMethod = (i: number) => (currentPage.value - 1) * pageSize.value + i + 1

const filteredUsers = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return users.value
  return users.value.filter(
    (u) =>
      u.userAccount.toLowerCase().includes(kw) ||
      (u.userName ?? '').toLowerCase().includes(kw)
  )
})

const pagedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredUsers.value.slice(start, start + pageSize.value)
})

watch(filteredUsers, () => {
  const max = Math.max(1, Math.ceil(filteredUsers.value.length / pageSize.value))
  if (currentPage.value > max) {
    currentPage.value = max
  }
})

async function load() {
  loading.value = true
  try {
    users.value = await listUsers()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  userForm.value = { userAccount: '', userPassword: '', userName: '', role: 'user' }
  dialogVisible.value = true
}

function openEdit(row: UserInfo | null) {
  if (!row) return
  editingId.value = row.id
  userForm.value = { userAccount: row.userAccount, userPassword: '', userName: row.userName ?? '', role: row.userRole }
  dialogVisible.value = true
}

async function handleSave() {
  const form = userForm.value
  if (!editingId.value && !form.userAccount.trim()) {
    ElMessage.warning('请输入账号')
    return
  }
  if (!editingId.value && !form.userPassword) {
    ElMessage.warning('请输入密码')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      const payload: { userName?: string; userPassword?: string } = { userName: form.userName }
      if (form.userPassword) {
        payload.userPassword = form.userPassword
      }
      await updateUser(editingId.value, payload)
      ElMessage.success('已保存')
    } else {
      await createUser({
        userAccount: form.userAccount.trim(),
        userPassword: form.userPassword,
        userName: form.userName,
        role: form.role
      })
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function handleDelete() {
  const rows = selected.value.filter((u) => u.id !== userStore.user?.id)
  if (rows.length === 0) return
  const msg = rows.length > 1
    ? `确定删除选中的 ${rows.length} 个用户吗？`
    : `确定删除用户「${rows[0].userAccount}」吗？`
  await ElMessageBox.confirm(msg, '删除用户', { type: 'warning' })
  const results = await Promise.allSettled(rows.map((u) => deleteUser(u.id)))
  summarizeResults(results, rows.length, '删除')
  await load()
}

async function toggleRole(role: string) {
  const rows = selected.value
  if (rows.length === 0) return
  if (rows.length > 1) {
    await ElMessageBox.confirm(`确定对选中的 ${rows.length} 个用户执行「${role === 'admin' ? '设为管理员' : '设为普通用户'}」吗？`, '操作确认', { type: 'warning' })
  }
  const results = await Promise.allSettled(rows.map((u) => updateUserRole(u.id, role)))
  summarizeResults(results, rows.length, role === 'admin' ? '设为管理员' : '设为普通用户')
  await load()
}

async function toggleStatus(enabled: boolean) {
  const rows = selected.value.filter((u) => u.id !== userStore.user?.id)
  if (rows.length === 0) return
  if (rows.length > 1) {
    await ElMessageBox.confirm(`确定对选中的 ${rows.length} 个用户执行「${enabled ? '启用' : '禁用'}」吗？`, '操作确认', { type: 'warning' })
  }
  const results = await Promise.allSettled(rows.map((u) => updateUserStatus(u.id, enabled)))
  summarizeResults(results, rows.length, enabled ? '启用' : '禁用')
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

function handleSelectionChange(rows: UserInfo[]) {
  selected.value = rows
}

function clearSelection() {
  selected.value = []
  tableRef.value?.clearSelection()
}

function handleRowClick(row: UserInfo) {
  tableRef.value?.toggleRowSelection(row)
}

function openDetail(row: UserInfo) {
  detailRow.value = row
  detailApps.value = []
  detailVisible.value = true
  loadApps(row.id)
}

function openResetPassword() {
  resetTargets.value = selected.value.filter((u) => u.id !== userStore.user?.id)
  if (resetTargets.value.length === 0) return
  resetPassword.value = ''
  resetVisible.value = true
}

async function handleResetPassword() {
  if (!resetPassword.value) {
    ElMessage.warning('请输入新密码')
    return
  }
  resetting.value = true
  try {
    const results = await Promise.allSettled(
      resetTargets.value.map((u) => updateUser(u.id, { userPassword: resetPassword.value }))
    )
    summarizeResults(results, resetTargets.value.length, '重置密码')
    resetVisible.value = false
  } finally {
    resetting.value = false
  }
}

async function loadApps(userId: number) {
  detailApps.value = await listApps(userId)
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
</style>
