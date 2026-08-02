<template>
  <div>
    <div class="toolbar">
      <h2>应用管理</h2>
      <el-button type="primary" @click="openCreate">新建应用</el-button>
    </div>
    <el-table :data="apps" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="appName" label="应用名称" />
      <el-table-column prop="accessKey" label="AccessKey" min-width="180" />
      <el-table-column label="SecretKey" min-width="240">
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
      <el-table-column prop="createTime" label="创建时间" width="180" />
    </el-table>

    <el-dialog v-model="dialogVisible" title="新建应用" width="420px">
      <el-form label-width="80px">
        <el-form-item label="应用名称">
          <el-input v-model="appName" placeholder="请输入应用名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { listApps, createApp, type AppInfo } from '@/api'

const userStore = useUserStore()
const apps = ref<AppInfo[]>([])
const dialogVisible = ref(false)
const appName = ref('')
const creating = ref(false)

async function load() {
  if (userStore.user) {
    apps.value = await listApps(userStore.user.id)
  }
}

function openCreate() {
  appName.value = ''
  dialogVisible.value = true
}

async function handleCreate() {
  if (!appName.value.trim()) {
    ElMessage.warning('请输入应用名称')
    return
  }
  creating.value = true
  try {
    if (userStore.user) {
      const app = await createApp(appName.value.trim(), userStore.user.id)
      ElMessage.success(`创建成功，请妥善保存 SecretKey（${app.accessKey}）`)
      dialogVisible.value = false
      await load()
    }
  } finally {
    creating.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
