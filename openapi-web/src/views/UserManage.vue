<template>
  <div>
    <div class="toolbar">
      <h2>用户管理</h2>
    </div>
    <CollapsibleFilter @search="applySearch" @reset="resetFilters">
      <el-input
        v-model="keywordInput"
        placeholder="搜索账号 / 昵称"
        clearable
        style="width: 240px"
        @input="onKeywordInput"
      />
    </CollapsibleFilter>

    <div class="action-bar">
      <div class="bar-left">
        <el-button size="small" type="primary" plain @click="exportUsers">导出 CSV</el-button>
        <el-button size="small" type="primary" @click="openCreate">新增用户</el-button>
        <el-divider direction="vertical" />
        <el-button size="small" type="primary" plain :disabled="!selectedRow" @click="openEdit(selectedRow)">编辑</el-button>
        <el-button
          size="small"
          type="primary"
          plain
          :disabled="roleAction.disabled"
          @click="toggleRole(roleAction.target)"
        >
          {{ roleAction.label }}
        </el-button>
        <el-button
          size="small"
          :type="statusAction.target === 0 ? 'danger' : 'success'"
          :disabled="statusAction.disabled"
          @click="toggleStatus(statusAction.target === 1)"
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
              <el-dropdown-item command="reset-password" :disabled="selected.length === 0">
                重置密码
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <div class="bar-right">
        <span v-if="selected.length" class="batch-tip">已选 {{ selected.length }} 项</span>
        <el-button size="small" type="danger" plain :disabled="selected.length === 0" @click="handleDelete">删除</el-button>
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
        <el-form-item label="角色">
          <el-select v-model="userForm.role" style="width: 100%" :disabled="editingId === userStore.user?.id">
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
      <h4>订阅记录</h4>
      <el-table v-if="detailSubscribes.length" :data="detailSubscribes" border stripe size="small">
        <el-table-column prop="interfaceName" label="接口" />
        <el-table-column prop="interfaceUrl" label="路径" min-width="160" />
        <el-table-column prop="appName" label="应用" />
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
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableInstance } from 'element-plus'
import { useUserStore } from '@/store/user'
import { ArrowDown } from '@element-plus/icons-vue'
import CollapsibleFilter from '@/components/CollapsibleFilter.vue'
import {
  pageUsers,
  listAppsForAdmin,
  listSubscribes,
  updateUserRole,
  updateUserStatus,
  createUser,
  updateUser,
  deleteUser,
  type AppInfo,
  type SubscribeInfo,
  type UserInfo
} from '@/api'

const userStore = useUserStore()
const users = ref<UserInfo[]>([])
const loading = ref(false)
const keyword = ref('')
const keywordInput = ref('')
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selected = ref<UserInfo[]>([])
const tableRef = ref<TableInstance>()
const detailVisible = ref(false)
const detailRow = ref<UserInfo | null>(null)
const detailApps = ref<AppInfo[]>([])
const detailSubscribes = ref<SubscribeInfo[]>([])
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
const roleAction = computed(() => {
  const rows = selected.value
  if (rows.length === 0) return { label: '设为管理员', disabled: true, target: 'admin' as const }
  const admins = rows.filter((u) => u.userRole === 'admin')
  if (admins.length === rows.length) return { label: '设为普通用户', disabled: false, target: 'user' as const }
  if (admins.length === 0) return { label: '设为管理员', disabled: false, target: 'admin' as const }
  return { label: '角色调整', disabled: true, target: 'admin' as const }
})
const statusAction = computed(() => {
  const rows = selected.value
  if (rows.length === 0) return { label: '启用', disabled: true, target: 1 }
  const enabled = rows.filter((u) => u.status === 1)
  if (enabled.length === rows.length) return { label: '禁用', disabled: false, target: 0 }
  if (enabled.length === 0) return { label: '启用', disabled: false, target: 1 }
  return { label: '启用/禁用', disabled: true, target: 1 }
})
const indexMethod = (i: number) => (currentPage.value - 1) * pageSize.value + i + 1
let keywordTimer: ReturnType<typeof setTimeout> | undefined

function onKeywordInput() {
  clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => {
    keyword.value = keywordInput.value
    currentPage.value = 1
    load()
  }, 300)
}

function applySearch() {
  clearTimeout(keywordTimer)
  keyword.value = keywordInput.value
  currentPage.value = 1
  load()
}

function resetFilters() {
  clearTimeout(keywordTimer)
  keywordInput.value = ''
  keyword.value = ''
  currentPage.value = 1
  load()
}

const pagedUsers = computed(() => users.value)

function handlePageChange() {
  clearSelection()
  load()
}

async function load() {
  loading.value = true
  try {
    const result = await pageUsers({
      current: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value || undefined
    })
    users.value = result.records
    total.value = Number(result.total)
  } finally {
    loading.value = false
  }
}

function exportUsers() {
  const header = ['账号', '昵称', '角色', '状态', '创建时间']
  const rows = users.value.map((user) => [
    user.userAccount,
    user.userName ?? '',
    user.userRole === 'admin' ? '管理员' : '普通用户',
    user.status === 1 ? '启用' : '禁用',
    user.createTime
  ])
  const escape = (value: string) => `"${String(value).replace(/"/g, '""')}"`
  const csv = '\uFEFF' + [header, ...rows].map((row) => row.map(escape).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `users-${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('CSV 已导出当前页数据')
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
      await updateUser(editingId.value, { ...payload, role: form.role })
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
  detailSubscribes.value = []
  detailVisible.value = true
  loadApps(row.id)
  loadSubscribes(row.id)
}

function openResetPassword() {
  resetTargets.value = selected.value.filter((u) => u.id !== userStore.user?.id)
  if (resetTargets.value.length === 0) return
  resetPassword.value = ''
  resetVisible.value = true
}

function handleMoreCommand(command: string) {
  if (command === 'reset-password') openResetPassword()
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
  detailApps.value = await listAppsForAdmin(userId)
}

async function loadSubscribes(userId: number) {
  const all = await listSubscribes()
  detailSubscribes.value = all.filter((s) => s.userId === userId)
}

function subscribeType(status: number) {
  return status === 1 ? 'success' : status === 2 ? 'danger' : 'warning'
}

function subscribeText(status: number) {
  return status === 1 ? '已通过' : status === 2 ? '已拒绝' : '待审批'
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
