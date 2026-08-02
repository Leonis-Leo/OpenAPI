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
      <el-button
        size="small"
        type="danger"
        plain
        :disabled="!selectedRow || selectedRow.userRole === 'admin'"
        @click="toggleRole('admin')"
      >
        设为管理员
      </el-button>
      <el-button
        size="small"
        type="primary"
        plain
        :disabled="!selectedRow || selectedRow.userRole !== 'admin'"
        @click="toggleRole('user')"
      >
        设为普通用户
      </el-button>
      <el-button
        size="small"
        type="success"
        :disabled="!selectedRow || selectedRow.status === 1 || selectedRow.id === userStore.user?.id"
        @click="toggleStatus(true)"
      >
        启用
      </el-button>
      <el-button
        size="small"
        type="danger"
        :disabled="!selectedRow || selectedRow.status !== 1 || selectedRow.id === userStore.user?.id"
        @click="toggleStatus(false)"
      >
        禁用
      </el-button>
      <el-button
        size="small"
        type="danger"
        plain
        :disabled="!selectedRow || selectedRow.id === userStore.user?.id"
        @click="handleDelete(selectedRow)"
      >
        删除
      </el-button>
      <el-divider direction="vertical" />
      <el-button size="small" type="success" plain :disabled="selected.length === 0" @click="batchToggle(true)">批量启用</el-button>
      <el-button size="small" type="danger" plain :disabled="selected.length === 0" @click="batchToggle(false)">批量禁用</el-button>
      <span v-if="selected.length" class="batch-tip">已选 {{ selected.length }} 项</span>
    </div>

    <el-table
      ref="tableRef"
      :data="pagedUsers"
      border
      stripe
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
      layout="total, prev, pager, next"
      :total="filteredUsers.length"
      :page-size="pageSize"
      v-model:current-page="currentPage"
    />

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑用户' : '新增用户'" width="420px">
      <el-form label-width="80px">
        <el-form-item v-if="!editingId" label="账号">
          <el-input v-model="userForm.userAccount" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="userForm.userPassword" type="password" show-password placeholder="请输入密码" />
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
  updateUserRole,
  updateUserStatus,
  createUser,
  updateUser,
  deleteUser,
  type UserInfo
} from '@/api'

const userStore = useUserStore()
const users = ref<UserInfo[]>([])
const keyword = ref('')
const currentPage = ref(1)
const pageSize = 10
const selected = ref<UserInfo[]>([])
const tableRef = ref<TableInstance>()
const detailVisible = ref(false)
const detailRow = ref<UserInfo | null>(null)
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
const indexMethod = (i: number) => (currentPage.value - 1) * pageSize + i + 1

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
  const start = (currentPage.value - 1) * pageSize
  return filteredUsers.value.slice(start, start + pageSize)
})

watch(filteredUsers, () => {
  const max = Math.max(1, Math.ceil(filteredUsers.value.length / pageSize))
  if (currentPage.value > max) {
    currentPage.value = max
  }
})

async function load() {
  users.value = await listUsers()
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
  if (!form.userPassword) {
    ElMessage.warning('请输入密码')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateUser(editingId.value, {
        userName: form.userName,
        userPassword: form.userPassword
      })
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

async function handleDelete(row: UserInfo | null) {
  if (!row) return
  await ElMessageBox.confirm(`确定删除用户「${row.userAccount}」吗？`, '删除用户', {
    type: 'warning'
  })
  await deleteUser(row.id)
  ElMessage.success('已删除')
  await load()
}

async function toggleRole(role: string) {
  const row = selectedRow.value
  if (!row) return
  await updateUserRole(row.id, role)
  ElMessage.success(role === 'admin' ? '已设为管理员' : '已设为普通用户')
  await load()
}

async function toggleStatus(enabled: boolean) {
  const row = selectedRow.value
  if (!row) return
  await updateUserStatus(row.id, enabled)
  ElMessage.success(enabled ? '已启用' : '已禁用')
  await load()
}

function handleSelectionChange(rows: UserInfo[]) {
  selected.value = rows
}

function handleRowClick(row: UserInfo) {
  tableRef.value?.toggleRowSelection(row)
}

function openDetail(row: UserInfo) {
  detailRow.value = row
  detailVisible.value = true
}

async function batchToggle(enabled: boolean) {
  const self = userStore.user?.id
  const targets = selected.value.filter((u) => u.id !== self)
  if (targets.length === 0) {
    ElMessage.warning('不能操作当前账号')
    return
  }
  await Promise.all(targets.map((u) => updateUserStatus(u.id, enabled)))
  ElMessage.success(enabled ? '已批量启用' : '已批量禁用')
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
