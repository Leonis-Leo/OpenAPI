<template>
  <div>
    <div class="toolbar">
      <h2>用户管理</h2>
      <el-input
        v-model="keyword"
        placeholder="搜索账号 / 昵称"
        clearable
        style="width: 220px"
        @input="currentPage = 1"
      />
    </div>
    <el-table :data="pagedUsers" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="userAccount" label="账号" />
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
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" @click="toggleRole(row)">
            {{ row.userRole === 'admin' ? '设为普通用户' : '设为管理员' }}
          </el-button>
          <el-button
            size="small"
            :type="row.status === 1 ? 'danger' : 'success'"
            :disabled="row.id === userStore.user?.id"
            @click="toggleStatus(row)"
          >
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="pagination"
      layout="total, prev, pager, next"
      :total="filteredUsers.length"
      :page-size="pageSize"
      v-model:current-page="currentPage"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { listUsers, updateUserRole, updateUserStatus, type UserInfo } from '@/api'

const userStore = useUserStore()
const users = ref<UserInfo[]>([])
const keyword = ref('')
const currentPage = ref(1)
const pageSize = 10

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

async function load() {
  users.value = await listUsers()
}

async function toggleRole(row: UserInfo) {
  await updateUserRole(row.id, row.userRole === 'admin' ? 'user' : 'admin')
  ElMessage.success('已更新角色')
  await load()
}

async function toggleStatus(row: UserInfo) {
  await updateUserStatus(row.id, row.status !== 1)
  ElMessage.success(row.status === 1 ? '已禁用' : '已启用')
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
.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
