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
            <div class="action-bar">
      <el-button size="small" type="success" :disabled="selectedPending.length === 0" @click="handleApprove(true)">通过</el-button>
      <el-button size="small" type="danger" :disabled="selectedPending.length === 0" @click="handleApprove(false)">拒绝</el-button>
      <span v-if="selectedPending.length" class="batch-tip">已选 {{ selectedPending.length }} 项</span>
    </div>
        <el-table
          ref="pendingTableRef"
          :data="pagedPending"
          border
          stripe
          @row-click="(row: SubscribeInfo) => pendingTableRef?.toggleRowSelection(row)"
          @selection-change="(rows: SubscribeInfo[]) => (selectedPending = rows)"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column type="index" label="#" width="60" :index="pendingIndex" />
          <el-table-column label="接口">
            <template #default="{ row }">
              <el-link type="primary" @click="openDetail(row)">{{ row.interfaceName }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="interfaceUrl" label="接口路径" min-width="180" />
          <el-table-column prop="appName" label="申请应用" />
          <el-table-column prop="createTime" label="申请时间" width="180" />
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
            <div class="action-bar">
      <el-button size="small" type="danger" plain :disabled="selectedMine.length === 0" @click="handleUnsubscribe">取消订阅</el-button>
      <span v-if="selectedMine.length" class="batch-tip">已选 {{ selectedMine.length }} 项</span>
    </div>
        <el-table
          ref="mineTableRef"
          :data="pagedMine"
          border
          stripe
          @row-click="(row: SubscribeInfo) => mineTableRef?.toggleRowSelection(row)"
          @selection-change="(rows: SubscribeInfo[]) => (selectedMine = rows)"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column type="index" label="#" width="60" :index="mineIndex" />
          <el-table-column label="接口">
            <template #default="{ row }">
              <el-link type="primary" @click="openDetail(row)">{{ row.interfaceName }}</el-link>
            </template>
          </el-table-column>
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

    <el-dialog v-model="detailVisible" :title="`订阅详情 - ${detailRow?.interfaceName ?? ''}`" width="520px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="接口">{{ detailRow?.interfaceName }}</el-descriptions-item>
        <el-descriptions-item label="路径">{{ detailRow?.interfaceUrl }}</el-descriptions-item>
        <el-descriptions-item label="应用">{{ detailRow?.appName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detailRow?.status ?? 0) }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ detailRow?.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableInstance } from 'element-plus'
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
const selectedPending = ref<SubscribeInfo[]>([])
const selectedMine = ref<SubscribeInfo[]>([])
const pendingTableRef = ref<TableInstance>()
const mineTableRef = ref<TableInstance>()
const detailVisible = ref(false)
const detailRow = ref<SubscribeInfo | null>(null)

const pendingRow = computed(() => (selectedPending.value.length === 1 ? selectedPending.value[0] : null))
const mineRow = computed(() => (selectedMine.value.length === 1 ? selectedMine.value[0] : null))
const pendingIndex = (i: number) => (pendingPage.value - 1) * pageSize + i + 1
const mineIndex = (i: number) => (minePage.value - 1) * pageSize + i + 1

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

watch(filteredPending, () => {
  const max = Math.max(1, Math.ceil(filteredPending.value.length / pageSize))
  if (pendingPage.value > max) {
    pendingPage.value = max
  }
})

watch(filteredMine, () => {
  const max = Math.max(1, Math.ceil(filteredMine.value.length / pageSize))
  if (minePage.value > max) {
    minePage.value = max
  }
})

async function load() {
  if (isAdmin) {
    pendingList.value = await listSubscribes(0)
  }
  myList.value = await mySubscribes()
}

async function handleApprove(approved: boolean) {
  const rows = selectedPending.value
  if (rows.length === 0) return
  if (rows.length > 1) {
    await ElMessageBox.confirm(`确定对选中的 ${rows.length} 条申请执行「${approved ? '通过' : '拒绝'}」吗？`, '操作确认', { type: 'warning' })
  }
  await Promise.all(rows.map((s) => approve(s.id, approved)))
  ElMessage.success(approved ? '已通过' : '已拒绝')
  await load()
}

async function handleUnsubscribe() {
  const rows = selectedMine.value.filter((s) => s.status === 1)
  if (rows.length === 0) return
  const msg = rows.length > 1
    ? `确定取消选中的 ${rows.length} 个订阅吗？`
    : `确定取消订阅「${rows[0].interfaceName}」吗？`
  await ElMessageBox.confirm(msg, '取消订阅', { type: 'warning' })
  await Promise.all(rows.map((s) => unsubscribe(s.id)))
  ElMessage.success('已取消订阅')
  await load()
}





function statusText(status: number) {
  return status === 1 ? '已通过' : status === 2 ? '已拒绝' : '待审批'
}

function statusType(status: number) {
  return status === 1 ? 'success' : status === 2 ? 'danger' : 'warning'
}

function openDetail(row: SubscribeInfo) {
  detailRow.value = row
  detailVisible.value = true
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
