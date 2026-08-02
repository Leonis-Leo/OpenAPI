<template>
  <div>
    <div class="toolbar">
      <h2>订阅审批</h2>
      <el-input
        v-model="keyword"
        placeholder="搜索接口 / 应用"
        clearable
        style="width: 220px"
      />
    </div>
    <el-tabs v-model="activeTab">
      <el-tab-pane v-if="isAdmin" label="待审批" name="pending">
        <el-table :data="pagedPending" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="interfaceName" label="接口" />
          <el-table-column prop="interfaceUrl" label="接口路径" min-width="180" />
          <el-table-column prop="appName" label="申请应用" />
          <el-table-column prop="createTime" label="申请时间" width="180" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button type="success" size="small" @click="handleApprove(row, true)">通过</el-button>
              <el-button type="danger" size="small" @click="handleApprove(row, false)">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          class="pagination"
          layout="total, prev, pager, next"
          :total="filteredPending.length"
          :page-size="pageSize"
          v-model:current-page="pendingPage"
        />
      </el-tab-pane>
      <el-tab-pane label="我的订阅" name="mine">
        <el-table :data="pagedMine" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="interfaceName" label="接口" />
          <el-table-column prop="interfaceUrl" label="接口路径" min-width="180" />
          <el-table-column prop="appName" label="应用" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" width="180" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 1"
                type="danger"
                size="small"
                plain
                @click="handleUnsubscribe(row)"
              >
                取消订阅
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          class="pagination"
          layout="total, prev, pager, next"
          :total="filteredMine.length"
          :page-size="pageSize"
          v-model:current-page="minePage"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { listSubscribes, mySubscribes, approve, unsubscribe, type SubscribeInfo } from '@/api'

const userStore = useUserStore()
const isAdmin = userStore.user?.userRole === 'admin'
const activeTab = ref('pending')
const pendingList = ref<SubscribeInfo[]>([])
const myList = ref<SubscribeInfo[]>([])
const keyword = ref('')
const pendingPage = ref(1)
const minePage = ref(1)
const pageSize = 10

function matchKw(item: SubscribeInfo): boolean {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return true
  return (
    item.interfaceName.toLowerCase().includes(kw) ||
    item.appName.toLowerCase().includes(kw) ||
    item.interfaceUrl.toLowerCase().includes(kw)
  )
}

const filteredPending = computed(() => pendingList.value.filter(matchKw))
const pagedPending = computed(() => {
  const start = (pendingPage.value - 1) * pageSize
  return filteredPending.value.slice(start, start + pageSize)
})
const filteredMine = computed(() => myList.value.filter(matchKw))
const pagedMine = computed(() => {
  const start = (minePage.value - 1) * pageSize
  return filteredMine.value.slice(start, start + pageSize)
})

async function load() {
  if (isAdmin) {
    pendingList.value = await listSubscribes(0)
  }
  myList.value = await mySubscribes()
}

async function handleApprove(row: SubscribeInfo, approved: boolean) {
  await approve(row.id, approved)
  ElMessage.success(approved ? '已通过' : '已拒绝')
  await load()
}

async function handleUnsubscribe(row: SubscribeInfo) {
  await ElMessageBox.confirm(`确定取消订阅「${row.interfaceName}」吗？`, '取消订阅', {
    type: 'warning'
  })
  await unsubscribe(row.id)
  ElMessage.success('已取消订阅')
  await load()
}

function statusText(status: number) {
  return status === 1 ? '已通过' : status === 2 ? '已拒绝' : '待审批'
}

function statusType(status: number) {
  return status === 1 ? 'success' : status === 2 ? 'danger' : 'warning'
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
