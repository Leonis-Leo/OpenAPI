<template>
  <div>
    <h2>接口管理</h2>
    <el-table :data="interfaces" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="description" label="描述" min-width="160" />
      <el-table-column prop="method" label="方式" width="90">
        <template #default="{ row }">
          <el-tag :type="row.method === 'GET' ? 'success' : 'warning'">
            {{ row.method }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="url" label="路径" min-width="180" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '上线' : '下线' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="订阅状态" width="110">
        <template #default="{ row }">
          <el-tag v-if="subscribeMap[row.id] === 1" type="success" size="small">已订阅</el-tag>
          <el-tag v-else-if="subscribeMap[row.id] === 0" type="warning" size="small">待审批</el-tag>
          <el-tag v-else-if="subscribeMap[row.id] === 2" type="danger" size="small">已拒绝</el-tag>
          <el-tag v-else type="info" size="small">未订阅</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button
            size="small"
            :disabled="subscribeMap[row.id] === 0 || subscribeMap[row.id] === 1"
            @click="openSubscribe(row)"
          >
            {{ subscribeMap[row.id] === 1 ? '已订阅' : subscribeMap[row.id] === 0 ? '已申请' : '订阅' }}
          </el-button>
          <el-button
            v-if="isAdmin"
            size="small"
            :type="row.status === 1 ? 'danger' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 1 ? '下线' : '上线' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="subscribeVisible" title="订阅接口" width="420px">
      <el-form label-width="80px">
        <el-form-item label="接口">
          <el-input :model-value="currentInterface?.name" disabled />
        </el-form-item>
        <el-form-item label="使用应用">
          <el-select v-model="selectedAppId" placeholder="请选择应用" style="width: 100%">
            <el-option v-for="app in apps" :key="app.id" :label="app.appName" :value="app.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="subscribeVisible = false">取消</el-button>
        <el-button type="primary" :loading="subscribing" @click="handleSubscribe">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  listApps,
  type AppInfo,
  listInterfaces,
  listAllInterfaces,
  onlineInterface,
  offlineInterface,
  subscribe,
  mySubscribes,
  type InterfaceInfo
} from '@/api'

const userStore = useUserStore()
const isAdmin = userStore.user?.userRole === 'admin'
const interfaces = ref<InterfaceInfo[]>([])
const apps = ref<AppInfo[]>([])
const subscribeMap = ref<Record<number, number>>({})
const subscribeVisible = ref(false)
const currentInterface = ref<InterfaceInfo | null>(null)
const selectedAppId = ref<number | null>(null)
const subscribing = ref(false)

async function load() {
  interfaces.value = isAdmin ? await listAllInterfaces() : await listInterfaces()
  const subscribes = await mySubscribes()
  const map: Record<number, number> = {}
  subscribes.forEach((s) => {
    map[s.interfaceId] = s.status
  })
  subscribeMap.value = map
  if (userStore.user) {
    apps.value = await listApps(userStore.user.id)
  }
}

function openSubscribe(row: InterfaceInfo) {
  currentInterface.value = row
  selectedAppId.value = apps.value[0]?.id ?? null
  subscribeVisible.value = true
}

async function handleSubscribe() {
  if (!currentInterface.value || !selectedAppId.value) {
    return
  }
  subscribing.value = true
  try {
    await subscribe(currentInterface.value.id, selectedAppId.value)
    ElMessage.success('订阅申请已提交，等待管理员审批')
    subscribeVisible.value = false
    await load()
  } finally {
    subscribing.value = false
  }
}

async function toggleStatus(row: InterfaceInfo) {
  if (row.status === 1) {
    await offlineInterface(row.id)
    ElMessage.success('已下线')
  } else {
    await onlineInterface(row.id)
    ElMessage.success('已上线')
  }
  await load()
}

onMounted(load)
</script>
