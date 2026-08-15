<template>
  <div class="audit-page">
    <div class="page-heading">
      <div>
        <h1 class="page-title">审计日志</h1>
        <p class="page-subtitle">查看管理员与用户的关键操作记录，支持按时间、用户和资源筛选</p>
      </div>
    </div>

    <CollapsibleFilter @search="reload" @reset="resetFilters">
      <el-input
        v-model="keyword"
        placeholder="搜索资源路径 / IP"
        clearable
        style="width: 220px"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-select v-model="successFilter" placeholder="结果" clearable style="width: 110px" @change="reload">
        <el-option label="成功" :value="1" />
        <el-option label="失败" :value="0" />
      </el-select>
      <el-select v-model="actionFilter" placeholder="动作" clearable style="width: 110px" @change="reload">
        <el-option label="GET" value="GET" />
        <el-option label="POST" value="POST" />
        <el-option label="PUT" value="PUT" />
        <el-option label="DELETE" value="DELETE" />
      </el-select>
      <template #more>
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          style="width: 340px"
          @change="reload"
        />
      </template>
    </CollapsibleFilter>

    <div class="action-bar">
      <div class="bar-left">
        <el-button size="small" plain @click="exportCsv">
          <el-icon><Download /></el-icon>导出 CSV
        </el-button>
      </div>
      <el-pagination
        class="bar-pagination"
        size="small"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <div class="table-card content-card">
      <TableSkeleton v-if="loading" :rows="6" />
      <el-table v-else :data="logs" border stripe highlight-current-row @row-click="openDetail">
        <el-table-column type="index" label="#" width="60" :index="indexMethod" />
        <el-table-column prop="createTime" label="时间" width="160" sortable />
        <el-table-column prop="userAccount" label="用户" width="120" sortable />
        <el-table-column prop="action" label="动作" width="90" sortable>
          <template #default="{ row }">
            <el-tag :type="row.action === 'GET' ? 'success' : 'warning'">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="资源" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" @click="openDetail(row)">{{ row.resource }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130" sortable />
        <el-table-column prop="statusCode" label="状态码" width="90" sortable align="right">
          <template #default="{ row }">
            <el-tag :type="row.statusCode < 400 ? 'success' : 'danger'" size="small">
              {{ row.statusCode }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="success" label="结果" width="80" sortable>
          <template #default="{ row }">
            {{ row.success === 1 ? '成功' : '失败' }}
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="detailVisible" :title="`审计日志 #${detail?.id ?? ''}`" size="560px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="用户">{{ detail?.userAccount }}</el-descriptions-item>
        <el-descriptions-item label="动作">{{ detail?.action }}</el-descriptions-item>
        <el-descriptions-item label="结果">{{ detail?.success === 1 ? '成功' : '失败' }}</el-descriptions-item>
        <el-descriptions-item label="资源" :span="3">{{ detail?.resource }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail?.ip }}</el-descriptions-item>
        <el-descriptions-item label="状态码">{{ detail?.statusCode }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ detail?.createTime }}</el-descriptions-item>
      </el-descriptions>
      <div class="block-toolbar">
        <span>详情</span>
        <el-button size="small" plain @click="copyText(detail?.detail)">复制</el-button>
      </div>
      <pre class="json-block" v-html="highlightJson(prettyJson(detail?.detail))"></pre>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onActivated, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import TableSkeleton from '@/components/TableSkeleton.vue'
import CollapsibleFilter from '@/components/CollapsibleFilter.vue'
import { pageAuditLogs, getAuditLog, exportAuditLogs, type AuditLogInfo } from '@/api'

const logs = ref<AuditLogInfo[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const successFilter = ref<number | undefined>(undefined)
const actionFilter = ref<string | undefined>(undefined)
const timeRange = ref<[Date, Date] | null>(null)
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref<AuditLogInfo | null>(null)

const indexMethod = (i: number) => (currentPage.value - 1) * pageSize.value + i + 1

function formatTime(date?: Date): string | undefined {
  if (!date) return undefined
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function load() {
  loading.value = true
  const page = await pageAuditLogs({
    current: currentPage.value,
    size: pageSize.value,
    keyword: keyword.value.trim() || undefined,
    success: successFilter.value,
    action: actionFilter.value,
    startTime: formatTime(timeRange.value?.[0]),
    endTime: formatTime(timeRange.value?.[1])
  })
  logs.value = page.records
  total.value = Number(page.total)
  loading.value = false
}

function reload() {
  currentPage.value = 1
  load()
}

function resetFilters() {
  keyword.value = ''
  successFilter.value = undefined
  actionFilter.value = undefined
  timeRange.value = null
  reload()
}

function handlePageChange() {
  load()
}

function handleSizeChange() {
  reload()
}

async function openDetail(row: AuditLogInfo) {
  detail.value = await getAuditLog(row.id)
  detailVisible.value = true
}

async function exportCsv() {
  const blob = await exportAuditLogs({
    keyword: keyword.value.trim() || undefined,
    success: successFilter.value,
    action: actionFilter.value,
    startTime: formatTime(timeRange.value?.[0]),
    endTime: formatTime(timeRange.value?.[1])
  })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `audit-logs-${new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-')}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function prettyJson(value?: string): string {
  if (!value) return ''
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

function highlightJson(text: string): string {
  if (!text) return ''
  const escaped = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  return escaped.replace(
    /("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+-]?\d+)?)/g,
    (match) => {
      let cls = 'json-number'
      if (/^"/.test(match)) cls = /:$/.test(match) ? 'json-key' : 'json-string'
      else if (/true|false/.test(match)) cls = 'json-boolean'
      else if (/null/.test(match)) cls = 'json-null'
      return `<span class="${cls}">${match}</span>`
    }
  )
}

async function copyText(value?: string) {
  if (!value) return
  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

onMounted(load)
onActivated(load)
</script>

<style scoped>
.audit-page { max-width: 1600px; margin: 0 auto; }
.page-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.table-card { overflow: hidden; }
.json-block { max-height: 260px; overflow: auto; padding: 14px; border: 1px solid var(--app-border); border-radius: 8px; background: #f8fafc; color: #334155; font-family: "JetBrains Mono", Consolas, monospace; font-size: 12px; line-height: 1.65; white-space: pre-wrap; }
.json-block :deep(.json-key) { color: #2563eb; }
.json-block :deep(.json-string) { color: #059669; }
.json-block :deep(.json-number) { color: #d97706; }
.json-block :deep(.json-boolean) { color: #db2777; }
.json-block :deep(.json-null) { color: #94a3b8; }
.block-toolbar { display: flex; align-items: center; justify-content: space-between; margin: 18px 0 6px; color: var(--app-text); font-size: 13px; font-weight: 600; }
</style>
