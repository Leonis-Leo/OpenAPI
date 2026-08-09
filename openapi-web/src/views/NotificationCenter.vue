<template>
  <div>
    <div class="toolbar">
      <h2>通知中心</h2>
      <div class="toolbar-right">
        <el-radio-group v-model="filterStatus" @change="handleFilterChange">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="unread">未读</el-radio-button>
        </el-radio-group>
      </div>
    </div>
    <div class="action-bar">
      <div class="bar-left">
        <el-button size="small" plain @click="handleReadAll">全部已读</el-button>
        <el-button size="small" type="danger" plain :disabled="total === 0" @click="handleClearAll">清空</el-button>
        <el-divider direction="vertical" />
        <el-button size="small" type="primary" plain :disabled="selected.length === 0" @click="handleMarkSelected">
          标记已读
        </el-button>
        <el-button class="danger-right" size="small" type="danger" plain :disabled="selected.length === 0" @click="handleDeleteSelected">
          删除
        </el-button>
        <span v-if="selected.length" class="batch-tip">已选 {{ selected.length }} 项</span>
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
      :data="list"
      border
      stripe
      v-loading="loading"
      @row-click="handleRowClick"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column type="index" label="#" width="60" :index="indexMethod" />
      <el-table-column label="标题" min-width="150">
        <template #default="{ row }">
          <span class="title-cell">
            <i v-if="row.isRead === 0" class="unread-dot"></i>
            <el-link type="primary" @click="openItem(row)">{{ row.title }}</el-link>
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
      <el-table-column prop="createTime" label="时间" width="170" sortable />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.isRead === 0 ? 'warning' : 'info'" size="small">
            {{ row.isRead === 0 ? '未读' : '已读' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onActivated, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableInstance } from 'element-plus'
import {
  pageNotifications,
  readNotification,
  readAllNotifications,
  deleteNotification,
  clearNotifications,
  type NotificationItem
} from '@/api'

const router = useRouter()
const filterStatus = ref<'all' | 'unread'>('all')
const list = ref<NotificationItem[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selected = ref<NotificationItem[]>([])
const tableRef = ref<TableInstance>()

const indexMethod = (i: number) => (currentPage.value - 1) * pageSize.value + i + 1

async function load() {
  loading.value = true
  try {
    const result = await pageNotifications({
      current: currentPage.value,
      size: pageSize.value,
      read: filterStatus.value === 'unread' ? 0 : undefined
    })
    list.value = result.records
    total.value = Number(result.total)
  } finally {
    loading.value = false
  }
}

function handleFilterChange() {
  currentPage.value = 1
  clearSelection()
  load()
}

function handlePageChange() {
  clearSelection()
  load()
}

function handleSelectionChange(rows: NotificationItem[]) {
  selected.value = rows
}

function handleRowClick(row: NotificationItem) {
  tableRef.value?.toggleRowSelection(row)
}

function clearSelection() {
  selected.value = []
  tableRef.value?.clearSelection()
}

async function openItem(row: NotificationItem) {
  if (row.isRead === 0) {
    try {
      await readNotification(row.id)
      row.isRead = 1
      if (filterStatus.value === 'unread') {
        list.value = list.value.filter((n) => n.id !== row.id)
        total.value = Math.max(0, total.value - 1)
      }
    } catch {
      // 标记失败不阻塞跳转
    }
  }
  if (row.link) {
    router.push(row.link)
  }
}

async function handleMarkSelected() {
  const rows = selected.value.filter((n) => n.isRead === 0)
  if (rows.length === 0) return
  const results = await Promise.allSettled(rows.map((n) => readNotification(n.id)))
  summarizeResults(results, rows.length, '标记已读')
  await load()
}

async function handleReadAll() {
  try {
    await readAllNotifications()
    ElMessage.success('已全部标记为已读')
  } catch {
    return
  }
  await load()
}

async function handleDeleteSelected() {
  const rows = selected.value
  if (rows.length === 0) return
  const msg = rows.length > 1
    ? `确定删除选中的 ${rows.length} 条通知吗？`
    : `确定删除通知「${rows[0].title}」吗？`
  await ElMessageBox.confirm(msg, '删除通知', { type: 'warning' })
  const results = await Promise.allSettled(rows.map((n) => deleteNotification(n.id)))
  summarizeResults(results, rows.length, '删除')
  await load()
}

async function handleClearAll() {
  await ElMessageBox.confirm('确定清空全部通知吗？此操作不可恢复。', '清空通知', { type: 'warning' })
  try {
    await clearNotifications()
    ElMessage.success('通知已清空')
  } catch {
    return
  }
  await load()
}

function summarizeResults(
  results: PromiseSettledResult<unknown>[],
  totalCount: number,
  action: string
) {
  const ok = results.filter((r) => r.status === 'fulfilled').length
  const fail = totalCount - ok
  if (fail === 0) {
    ElMessage.success(`${action}成功 ${totalCount} 条`)
  } else if (ok === 0) {
    ElMessage.error(`${action}失败 ${fail} 条`)
  } else {
    ElMessage.warning(`${action}成功 ${ok} 条，失败 ${fail} 条`)
  }
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
.title-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.unread-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2563eb;
  flex: 0 0 auto;
}
</style>
