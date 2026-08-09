<template>
  <div>
    <div class="toolbar">
      <h2>订阅审批</h2>
    </div>
    <CollapsibleFilter @search="resetSearch" @reset="resetFilters">
      <el-input
        v-model="keyword"
        placeholder="搜索接口 / 应用"
        clearable
        style="width: 240px"
        @input="resetSearch"
      />
    </CollapsibleFilter>
    <el-tabs v-model="activeTab">
      <el-tab-pane v-if="isAdmin" label="全部订阅" name="all">
        <div class="action-bar">
        <div class="bar-left">
          <el-button size="small" type="primary" plain @click="exportCurrent">导出 CSV</el-button>
          <el-divider direction="vertical" />
          <el-button class="danger-right" size="small" type="danger" plain :disabled="selectedAll.length === 0" @click="handleDeleteAll">删除记录</el-button>
            <span v-if="selectedAll.length" class="batch-tip">已选 {{ selectedAll.length }} 项</span>
          </div>
          <el-pagination
            class="bar-pagination"
            size="small"
            layout="total, sizes, prev, pager, next, jumper"
            :total="allTotal"
            :page-sizes="[10, 20, 50, 100]"
            v-model:current-page="allPage"
            v-model:page-size="pageSize"
            @current-change="handleAllPageChange"
            @size-change="handleAllPageChange"
          />
        </div>
        <el-table
          ref="allTableRef"
          :data="pagedAll"
          border
          stripe
          v-loading="loading"
          @row-click="(row: SubscribeInfo) => allTableRef?.toggleRowSelection(row)"
          @selection-change="(rows: SubscribeInfo[]) => (selectedAll = rows)"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column type="index" label="#" width="60" :index="allIndex" />
          <el-table-column label="接口">
            <template #default="{ row }">
              <el-link type="primary" @click="openDetail(row)">{{ row.interfaceName }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="interfaceUrl" label="接口路径" min-width="180" />
          <el-table-column prop="appName" label="应用" />
          <el-table-column prop="userAccount" label="申请人" width="120" />
          <el-table-column prop="status" label="状态" width="100" sortable>
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" width="180" sortable />
        </el-table>
      </el-tab-pane>
      <el-tab-pane v-if="isAdmin" label="待审批" name="pending">
    <div class="action-bar">
      <div class="bar-left">
        <el-button size="small" type="primary" plain @click="exportCurrent">导出 CSV</el-button>
        <el-divider direction="vertical" />
        <el-button size="small" type="success" :disabled="selectedPending.length === 0" @click="handleApprove(true)">通过</el-button>
        <el-button class="danger-right" size="small" type="danger" :disabled="selectedPending.length === 0" @click="handleApprove(false)">拒绝</el-button>
        <span v-if="selectedPending.length" class="batch-tip">已选 {{ selectedPending.length }} 项</span>
      </div>
      <el-pagination
        class="bar-pagination"
        size="small"
        layout="total, sizes, prev, pager, next, jumper"
        :total="pendingTotal"
        :page-sizes="[10, 20, 50, 100]"
        v-model:current-page="pendingPage"
        v-model:page-size="pageSize"
        @current-change="handlePendingPageChange"
        @size-change="handlePendingPageChange"
      />
    </div>
        <el-table
          ref="pendingTableRef"
          :data="pagedPending"
          border
          stripe
          v-loading="loading"
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
          <el-table-column v-if="isAdmin" prop="userAccount" label="申请人" width="120" />
          <el-table-column prop="createTime" label="申请时间" width="180" sortable />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="我的订阅" name="mine">
    <div class="action-bar">
      <div class="bar-left">
        <el-button size="small" type="primary" plain @click="exportCurrent">导出 CSV</el-button>
        <el-divider direction="vertical" />
        <el-button class="danger-right" size="small" type="danger" plain :disabled="selectedMine.length === 0" @click="handleUnsubscribe">取消订阅</el-button>
        <span v-if="selectedMine.length" class="batch-tip">已选 {{ selectedMine.length }} 项</span>
      </div>
      <el-pagination
        class="bar-pagination"
        size="small"
        layout="total, sizes, prev, pager, next, jumper"
        :total="mineTotal"
        :page-sizes="[10, 20, 50, 100]"
        v-model:current-page="minePage"
        v-model:page-size="pageSize"
        @current-change="handleMinePageChange"
        @size-change="handleMinePageChange"
      />
    </div>
        <el-table
          ref="mineTableRef"
          :data="pagedMine"
          border
          stripe
          v-loading="loading"
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
          <el-table-column prop="userAccount" label="申请人" width="120" />
          <el-table-column prop="status" label="状态" width="100" sortable>
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" width="180" sortable />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="detailVisible" :title="`订阅详情 - ${detailRow?.interfaceName ?? ''}`" width="520px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="接口">{{ detailRow?.interfaceName }}</el-descriptions-item>
        <el-descriptions-item label="路径">{{ detailRow?.interfaceUrl }}</el-descriptions-item>
        <el-descriptions-item label="应用">{{ detailRow?.appName }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detailRow?.userAccount }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detailRow?.status ?? 0) }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ detailRow?.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableInstance } from 'element-plus'
import { useUserStore } from '@/store/user'
import CollapsibleFilter from '@/components/CollapsibleFilter.vue'
import {
  pageSubscribes,
  pageMySubscribes,
  approve,
  unsubscribe,
  deleteSubscribeRecord,
  type SubscribeInfo
} from '@/api'

const userStore = useUserStore()
const isAdmin = userStore.user?.userRole === 'admin'
const activeTab = ref('pending')
const pendingList = ref<SubscribeInfo[]>([])
const myList = ref<SubscribeInfo[]>([])
const allList = ref<SubscribeInfo[]>([])
const keyword = ref('')
const loading = ref(false)
const pendingPage = ref(1)
const minePage = ref(1)
const allPage = ref(1)
const pageSize = ref(10)
const allTotal = ref(0)
const pendingTotal = ref(0)
const mineTotal = ref(0)
const selectedPending = ref<SubscribeInfo[]>([])
const selectedMine = ref<SubscribeInfo[]>([])
const selectedAll = ref<SubscribeInfo[]>([])
const pendingTableRef = ref<TableInstance>()
const mineTableRef = ref<TableInstance>()
const allTableRef = ref<TableInstance>()
const detailVisible = ref(false)
const detailRow = ref<SubscribeInfo | null>(null)

const pendingRow = computed(() => (selectedPending.value.length === 1 ? selectedPending.value[0] : null))
const mineRow = computed(() => (selectedMine.value.length === 1 ? selectedMine.value[0] : null))
const pendingIndex = (i: number) => (pendingPage.value - 1) * pageSize.value + i + 1
const mineIndex = (i: number) => (minePage.value - 1) * pageSize.value + i + 1
const allIndex = (i: number) => (allPage.value - 1) * pageSize.value + i + 1

function clearPendingSelection() {
  selectedPending.value = []
  pendingTableRef.value?.clearSelection()
}

function clearMineSelection() {
  selectedMine.value = []
  mineTableRef.value?.clearSelection()
}

function clearAllSelection() {
  selectedAll.value = []
  allTableRef.value?.clearSelection()
}

function handleAllPageChange() {
  clearAllSelection()
  load()
}

function handlePendingPageChange() {
  clearPendingSelection()
  load()
}

function handleMinePageChange() {
  clearMineSelection()
  load()
}

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
const pagedPending = computed(() => filteredPending.value)
const filteredMine = computed(() => myList.value.filter(matchKw))
const pagedMine = computed(() => filteredMine.value)
const filteredAll = computed(() => allList.value.filter(matchKw))
const pagedAll = computed(() => filteredAll.value)

function resetSearch() {
  pendingPage.value = 1
  minePage.value = 1
  allPage.value = 1
}

function resetFilters() {
  keyword.value = ''
  resetSearch()
}

async function load() {
  loading.value = true
  try {
    if (isAdmin) {
      const [pending, all] = await Promise.all([
        pageSubscribes({ current: pendingPage.value, size: pageSize.value, status: 0 }),
        pageSubscribes({ current: allPage.value, size: pageSize.value })
      ])
      pendingList.value = pending.records
      pendingTotal.value = Number(pending.total)
      allList.value = all.records
      allTotal.value = Number(all.total)
    }
    const mine = await pageMySubscribes({ current: minePage.value, size: pageSize.value })
    myList.value = mine.records
    mineTotal.value = Number(mine.total)
  } finally {
    loading.value = false
  }
}

function exportCurrent() {
  const records = activeTab.value === 'all' ? allList.value : activeTab.value === 'pending' ? pendingList.value : myList.value
  const header = ['接口', '路径', '应用', '申请人', '状态', '申请时间']
  const rows = records.map((item) => [
    item.interfaceName,
    item.interfaceUrl,
    item.appName,
    item.userAccount ?? '',
    statusText(item.status),
    item.createTime
  ])
  const escape = (value: string) => `"${String(value).replace(/"/g, '""')}"`
  const csv = '\uFEFF' + [header, ...rows].map((row) => row.map(escape).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `subscriptions-${activeTab.value}-${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('CSV 已导出当前页数据')
}

async function handleDeleteAll() {
  const rows = selectedAll.value
  if (rows.length === 0) return
  const msg = rows.length > 1
    ? `确定删除选中的 ${rows.length} 条订阅记录吗？`
    : `确定删除「${rows[0].interfaceName}」的订阅记录吗？`
  await ElMessageBox.confirm(msg, '删除记录', { type: 'warning' })
  const results = await Promise.allSettled(rows.map((s) => deleteSubscribeRecord(s.id)))
  summarizeResults(results, rows.length, '删除')
  await load()
}

async function handleApprove(approved: boolean) {
  const rows = selectedPending.value
  if (rows.length === 0) return
  if (rows.length > 1) {
    await ElMessageBox.confirm(`确定对选中的 ${rows.length} 条申请执行「${approved ? '通过' : '拒绝'}」吗？`, '操作确认', { type: 'warning' })
  }
  const results = await Promise.allSettled(rows.map((s) => approve(s.id, approved)))
  summarizeResults(results, rows.length, approved ? '通过' : '拒绝')
  await load()
}

async function handleUnsubscribe() {
  const rows = selectedMine.value.filter((s) => s.status === 1)
  if (rows.length === 0) return
  const msg = rows.length > 1
    ? `确定取消选中的 ${rows.length} 个订阅吗？`
    : `确定取消订阅「${rows[0].interfaceName}」吗？`
  await ElMessageBox.confirm(msg, '取消订阅', { type: 'warning' })
  const results = await Promise.allSettled(rows.map((s) => unsubscribe(s.id)))
  summarizeResults(results, rows.length, '取消订阅')
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

onActivated(load)
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
