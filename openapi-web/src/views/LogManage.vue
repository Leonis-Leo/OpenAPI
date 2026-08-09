<template>
  <div class="logs-page">
    <div class="page-heading">
      <div><h1 class="page-title">API 日志</h1><p class="page-subtitle">追踪接口调用状态，快速定位异常请求和性能瓶颈</p></div>
      <el-button plain @click="exportCsv"><el-icon><Download /></el-icon>导出 CSV</el-button>
    </div>
    <div class="summary-grid">
      <el-card shadow="never" class="summary-card"><div class="summary-label">总调用量</div><div class="summary-value">{{ total }}</div><div class="summary-foot"><span class="summary-icon blue"><DataLine /></span>当前筛选范围</div></el-card>
      <el-card shadow="never" class="summary-card"><div class="summary-label">成功率</div><div class="summary-value">{{ successRate }}<small>%</small></div><div class="summary-foot positive"><span class="summary-icon green"><CircleCheckFilled /></span>请求成功</div></el-card>
      <el-card shadow="never" class="summary-card"><div class="summary-label">平均耗时</div><div class="summary-value">{{ averageCost }}<small>ms</small></div><div class="summary-foot"><span class="summary-icon amber"><Timer /></span>当前页平均值</div></el-card>
      <el-card shadow="never" class="summary-card"><div class="summary-label">异常请求</div><div class="summary-value danger-number">{{ errorCount }}</div><div class="summary-foot negative"><span class="summary-icon red"><WarningFilled /></span>需要关注</div></el-card>
    </div>
    <div class="filter-card content-card">
      <div class="filter-head">
        <span class="filter-title">筛选条件</span>
        <div class="filter-head-actions">
          <el-button plain @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="reload"><el-icon><Search /></el-icon>查询</el-button>
        </div>
      </div>
      <el-form label-position="top" class="filter-fields">
        <el-form-item label="结果">
          <el-select v-model="statusType" placeholder="全部" clearable style="width: 100%" @change="reload">
            <el-option label="成功" value="success" />
            <el-option label="失败" value="fail" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态码">
          <el-select v-model="statusFilter" placeholder="全部" clearable style="width: 100%" @change="reload">
            <el-option label="401" :value="401" />
            <el-option label="403" :value="403" />
            <el-option label="429" :value="429" />
            <el-option label="500" :value="500" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围" class="filter-time">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
            @change="reload"
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="keyword"
            placeholder="搜索路径 / IP"
            clearable
            style="width: 100%"
            @keyup.enter="reload"
            @clear="reload"
          />
        </el-form-item>
      </el-form>
      <div v-if="activeFilters.length" class="filter-chips">
        <el-tag
          v-for="filter in activeFilters"
          :key="filter.key"
          closable
          size="small"
          @close="clearFilter(filter.key)"
        >
          {{ filter.label }}
        </el-tag>
      </div>
    </div>

    <div class="table-card content-card">
      <div class="table-heading"><div><strong>调用记录</strong><span>共 {{ total }} 条记录</span></div><div class="action-bar">
      <div class="bar-left">
        <el-button size="small" type="primary" plain :disabled="!selectedRow" @click="openDetail(selectedRow)">
          查看详情
        </el-button>
        <el-button class="danger-right" size="small" type="danger" :disabled="selected.length === 0" @click="handleDelete">
          删除
        </el-button>
        <el-divider direction="vertical" />
        <el-button size="small" type="danger" plain @click="handleClear">清空日志</el-button>
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
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
      </div></div>

    <el-table
      ref="tableRef"
      :data="logs"
      border
      stripe
      v-loading="loading"
      @row-click="handleRowClick"
      @selection-change="(rows: ApiLog[]) => (selected = rows)"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column type="index" label="#" width="60" :index="indexMethod" />
      <el-table-column prop="createTime" label="时间" width="160" sortable />
      <el-table-column label="接口" width="140">
        <template #default="{ row }">
          <el-link type="primary" @click="openDetail(row)">{{ row.interfaceName }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="method" label="方式" width="80">
        <template #default="{ row }">
          <el-tag :type="row.method === 'GET' ? 'success' : 'warning'">{{ row.method }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路径" min-width="160" />
      <el-table-column prop="appName" label="应用" width="120" />
      <el-table-column prop="userAccount" label="用户" width="120" />
      <el-table-column prop="ip" label="IP" width="130" />
      <el-table-column prop="statusCode" label="状态码" width="90" sortable>
        <template #default="{ row }">
          <el-tag :type="row.statusCode < 400 ? 'success' : 'danger'" size="small">
            {{ row.statusCode }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="costMs" label="耗时(ms)" width="90" sortable />
    </el-table>
    </div>

    <el-drawer v-model="detailVisible" :title="`日志详情 #${detail?.id ?? ''}`" size="560px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="接口">{{ detail?.interfaceName }}</el-descriptions-item>
        <el-descriptions-item label="应用">{{ detail?.appName }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ detail?.userAccount }}</el-descriptions-item>
        <el-descriptions-item label="方式">{{ detail?.method }}</el-descriptions-item>
        <el-descriptions-item label="路径" :span="2">{{ detail?.path }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail?.ip }}</el-descriptions-item>
        <el-descriptions-item label="状态码">{{ detail?.statusCode }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail?.costMs }} ms</el-descriptions-item>
      </el-descriptions>
      <div class="block-toolbar">
        <span>请求参数</span>
        <el-button size="small" plain @click="copyText(detail?.requestParams)">复制</el-button>
      </div>
      <pre class="json-block" v-html="highlightJson(prettyJson(detail?.requestParams))"></pre>
      <div class="block-toolbar">
        <span>请求头</span>
        <el-button size="small" plain @click="copyText(detail?.requestHeaders)">复制</el-button>
      </div>
      <pre class="json-block" v-html="highlightJson(prettyJson(detail?.requestHeaders))"></pre>
      <div class="block-toolbar">
        <span>响应体</span>
        <el-button size="small" plain @click="copyText(detail?.responseBody)">复制</el-button>
      </div>
      <pre class="json-block" v-html="highlightJson(prettyJson(detail?.responseBody))"></pre>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableInstance } from 'element-plus'
import { CircleCheckFilled, DataLine, Download, Search, Timer, WarningFilled } from '@element-plus/icons-vue'
import {
  listApiLogs,
  getApiLog,
  deleteApiLog,
  deleteApiLogs,
  clearApiLogs,
  type ApiLog
} from '@/api'

const logs = ref<ApiLog[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const statusFilter = ref<number | undefined>(undefined)
const statusType = ref<'success' | 'fail' | undefined>(undefined)
const timeRange = ref<[Date, Date] | null>(null)
const appFilter = ref<number | undefined>(undefined)
const appFilterName = ref('')
const interfaceFilter = ref<number | undefined>(undefined)
const interfaceFilterName = ref('')
const dateFilter = ref('')
const route = useRoute()
const loading = ref(false)
const selected = ref<ApiLog[]>([])
const tableRef = ref<TableInstance>()
const detailVisible = ref(false)
const detail = ref<ApiLog | null>(null)

const selectedRow = computed(() => (selected.value.length === 1 ? selected.value[0] : null))
const successRate = computed(() => {
  if (!logs.value.length) return '0.0'
  return ((logs.value.filter((item) => item.statusCode < 400).length / logs.value.length) * 100).toFixed(1)
})
const averageCost = computed(() => logs.value.length ? Math.round(logs.value.reduce((sum, item) => sum + Number(item.costMs || 0), 0) / logs.value.length) : 0)
const errorCount = computed(() => logs.value.filter((item) => item.statusCode >= 400).length)
const indexMethod = (i: number) => (currentPage.value - 1) * pageSize.value + i + 1
const activeFilters = computed(() => {
  const list: { key: string; label: string }[] = []
  if (dateFilter.value) list.push({ key: 'date', label: `日期：${dateFilter.value}` })
  if (appFilter.value) list.push({ key: 'app', label: `应用：${appFilterName.value || `#${appFilter.value}`}` })
  if (interfaceFilter.value) list.push({ key: 'interface', label: `接口：${interfaceFilterName.value || `#${interfaceFilter.value}`}` })
  return list
})

async function load() {
  loading.value = true
  const page = await listApiLogs({
    current: currentPage.value,
    size: pageSize.value,
    keyword: keyword.value.trim() || undefined,
    success: statusType.value === 'success' ? 1 : statusType.value === 'fail' ? 0 : undefined,
    statusCode: statusFilter.value,
    appId: appFilter.value,
    interfaceId: interfaceFilter.value,
    startTime: dateFilter.value ? `${dateFilter.value} 00:00:00` : formatTime(timeRange.value?.[0]),
    endTime: dateFilter.value ? `${dateFilter.value} 23:59:59` : formatTime(timeRange.value?.[1])
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
  statusType.value = undefined
  statusFilter.value = undefined
  timeRange.value = null
  keyword.value = ''
  dateFilter.value = ''
  appFilter.value = undefined
  appFilterName.value = ''
  interfaceFilter.value = undefined
  interfaceFilterName.value = ''
  reload()
}

function formatTime(date?: Date): string | undefined {
  if (!date) return undefined
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function applyRouteFilters() {
  const query = route.query
  if (typeof query.date === 'string') {
    dateFilter.value = query.date
  }
  if (typeof query.appId === 'string' && query.appId) {
    appFilter.value = Number(query.appId)
    appFilterName.value = typeof query.appName === 'string' ? query.appName : ''
  }
  if (typeof query.interfaceId === 'string' && query.interfaceId) {
    interfaceFilter.value = Number(query.interfaceId)
    interfaceFilterName.value = typeof query.interfaceName === 'string' ? query.interfaceName : ''
  }
}

function clearFilter(key: string) {
  if (key === 'date') dateFilter.value = ''
  if (key === 'app') { appFilter.value = undefined; appFilterName.value = '' }
  if (key === 'interface') { interfaceFilter.value = undefined; interfaceFilterName.value = '' }
  reload()
}

function handleRowClick(row: ApiLog) {
  tableRef.value?.toggleRowSelection(row)
}

function clearSelection() {
  selected.value = []
  tableRef.value?.clearSelection()
}

function handlePageChange() {
  clearSelection()
  load()
}

function handleSizeChange() {
  clearSelection()
  reload()
}

async function openDetail(row: ApiLog | null) {
  if (!row) return
  detail.value = await getApiLog(row.id)
  detailVisible.value = true
}

async function handleDelete() {
  const rows = selected.value
  if (rows.length === 0) return
  const msg = rows.length > 1
    ? `确定删除选中的 ${rows.length} 条日志吗？`
    : `确定删除日志 #${rows[0].id} 吗？`
  await ElMessageBox.confirm(msg, '删除日志', { type: 'warning' })
  const results = await Promise.allSettled(rows.map((l) => deleteApiLog(l.id)))
  summarizeResults(results, rows.length, '删除')
  await load()
}

async function handleClear() {
  await ElMessageBox.confirm('确定清空全部日志吗？此操作不可恢复', '清空日志', {
    type: 'warning'
  })
  await clearApiLogs()
  ElMessage.success('已清空')
  reload()
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
  const escaped = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  return escaped.replace(
    /("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+-]?\d+)?)/g,
    (match) => {
      let cls = 'json-number'
      if (/^"/.test(match)) {
        cls = /:$/.test(match) ? 'json-key' : 'json-string'
      } else if (/true|false/.test(match)) {
        cls = 'json-boolean'
      } else if (/null/.test(match)) {
        cls = 'json-null'
      }
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

function exportCsv() {
  if (logs.value.length === 0) {
    ElMessage.warning('当前页无日志可导出')
    return
  }
  const headers = ['时间', '接口', '方式', '路径', '应用', '用户', 'IP', '状态码', '耗时(ms)']
  const rows = logs.value.map((l) => [
    l.createTime,
    l.interfaceName,
    l.method,
    l.path,
    l.appName,
    l.userAccount,
    l.ip,
    l.statusCode,
    l.costMs
  ])
  const csv = [headers, ...rows]
    .map((r) => r.map((c) => `"${String(c ?? '').replace(/"/g, '""')}"`).join(','))
    .join('\r\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `api-logs-${new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-')}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(() => {
  applyRouteFilters()
  load()
})

onActivated(() => {
  applyRouteFilters()
  load()
})
</script>

<style scoped>
.logs-page { max-width: 1600px; margin: 0 auto; }
.page-heading,.table-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; margin: 24px 0 18px; }
.summary-card { min-height: 128px; }.summary-label { color: var(--app-muted); font-size: 13px; }.summary-value { margin: 8px 0 12px; color: var(--app-text); font-size: 28px; font-weight: 700; letter-spacing: -.03em; }.summary-value small { margin-left: 3px; color: var(--app-muted); font-size: 13px; font-weight: 500; }.danger-number { color: #dc2626; }.summary-foot { display: flex; align-items: center; gap: 7px; color: var(--app-muted); font-size: 11px; }.summary-foot.positive { color: #059669; }.summary-foot.negative { color: #dc2626; }.summary-icon { display: grid; place-items: center; width: 20px; height: 20px; border-radius: 6px; }.summary-icon.blue { background: #dbeafe; color: #2563eb; }.summary-icon.green { background: #d1fae5; color: #059669; }.summary-icon.amber { background: #fef3c7; color: #d97706; }.summary-icon.red { background: #fee2e2; color: #dc2626; }
.filter-card { padding: 16px; margin-bottom: 16px; }
.filter-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.filter-title { color: var(--app-text); font-size: 13px; font-weight: 600; }
.filter-head-actions { display: flex; align-items: center; gap: 8px; }
.filter-fields { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); column-gap: 16px; row-gap: 2px; }
.filter-fields :deep(.el-form-item) { margin-bottom: 0; }
.filter-fields :deep(.el-form-item__label) { color: var(--app-muted); font-size: 12px; font-weight: 500; line-height: 1.4; padding-bottom: 4px; }
.filter-fields :deep(.el-form-item__content) { width: 100%; }
.filter-fields .filter-time { min-width: 320px; }
.filter-chips { display: flex; flex-wrap: wrap; gap: 8px; padding-top: 12px; margin-top: 12px; border-top: 1px dashed var(--app-border); }
.table-card { overflow: hidden; }.table-heading { padding: 18px 20px; border-bottom: 1px solid var(--app-border); }.table-heading strong { color: var(--app-text); font-size: 15px; }.table-heading span { margin-left: 10px; color: var(--app-muted); font-size: 12px; }.action-bar { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }.danger-right { margin-left: 4px; }.batch-tip { color: var(--app-muted); font-size: 12px; }.table-card :deep(.el-table) { border: 0; }.table-card :deep(.el-table__inner-wrapper::before) { display: none; }.table-card :deep(.el-table th:first-child),.table-card :deep(.el-table td:first-child) { padding-left: 20px; }.pagination { justify-content: flex-end; padding: 18px 20px; }
.json-block { max-height: 260px; overflow: auto; padding: 14px; border: 1px solid var(--app-border); border-radius: 8px; background: #f8fafc; color: #334155; font-family: "JetBrains Mono", Consolas, monospace; font-size: 12px; line-height: 1.65; white-space: pre-wrap; }.json-block :deep(.json-key) { color: #2563eb; }.json-block :deep(.json-string) { color: #059669; }.json-block :deep(.json-number) { color: #d97706; }.json-block :deep(.json-boolean) { color: #db2777; }.json-block :deep(.json-null) { color: #94a3b8; }.block-toolbar { display: flex; align-items: center; justify-content: space-between; margin: 18px 0 6px; color: var(--app-text); font-size: 13px; font-weight: 600; }
@media (max-width: 1100px) { .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }.filter-fields { grid-template-columns: 1fr; }.filter-fields .filter-time { min-width: 0; } }
@media (max-width: 640px) { .summary-grid { grid-template-columns: 1fr; }.page-heading { align-items: flex-start; flex-direction: column; }.table-heading { align-items: flex-start; flex-direction: column; }.action-bar { width: 100%; }.danger-right { margin-left: auto; } }
</style>
