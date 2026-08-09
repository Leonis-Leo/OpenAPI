<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>调用明细</h2>
        <p class="page-subtitle">按天 / 应用 / 接口维度查看调用量、成功率与平均耗时，点击行可跳转当日日志</p>
      </div>
    </div>
    <CollapsibleFilter @search="reload" @reset="resetFilters">
      <el-select v-model="dimension" style="width: 130px" @change="onDimensionChange">
        <el-option label="按天" value="day" />
        <el-option label="按应用" value="app" />
        <el-option label="按接口" value="interface" />
      </el-select>
      <el-select v-model="datePreset" style="width: 130px" @change="onDatePresetChange">
        <el-option label="全部时间" value="all" />
        <el-option label="近 7 天" value="7" />
        <el-option label="近 30 天" value="30" />
        <el-option label="近 90 天" value="90" />
        <el-option label="自定义" value="custom" />
      </el-select>
      <el-date-picker
        v-if="showCustomDate"
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始"
        end-placeholder="结束"
        style="width: 240px"
        @change="reload"
      />
    </CollapsibleFilter>
    <div class="action-bar">
      <div class="bar-left">
        <el-button size="small" plain @click="exportCsv">导出 CSV</el-button>
        <el-button size="small" @click="reload">刷新</el-button>
      </div>
      <el-pagination
        class="bar-pagination"
        size="small"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        @current-change="load"
        @size-change="load"
      />
    </div>

    <div class="table-card content-card">
      <el-table
        :data="list"
        border
        stripe
        size="small"
        v-loading="loading"
        @row-click="openDetailLogs"
      >
        <el-table-column type="index" label="#" width="50" :index="indexMethod" />
        <el-table-column v-if="dimension === 'day'" prop="day" label="日期" min-width="120" />
        <el-table-column v-else-if="dimension === 'app'" prop="appName" label="应用" min-width="150" />
        <el-table-column v-else prop="interfaceName" label="接口" min-width="170" />
        <el-table-column prop="total" label="调用量" width="90" sortable align="right" />
        <el-table-column prop="success" label="成功" width="80" align="right" />
        <el-table-column prop="fail" label="失败" width="80" align="right" />
        <el-table-column label="成功率" prop="successRate" width="90" sortable align="right">
          <template #default="{ row }">
            <el-tag :type="row.successRate >= 90 ? 'success' : row.total ? 'warning' : 'info'" size="small">
              {{ row.successRate }}%
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="avgCostMs" label="平均耗时(ms)" width="110" sortable align="right" />
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click.stop="openDetailLogs(row)">查看日志</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!list.length && !loading" description="暂无明显数据" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CollapsibleFilter from '@/components/CollapsibleFilter.vue'
import { statsDailyPage, type StatsDetailItem } from '@/api'

const router = useRouter()
const route = useRoute()
const dimension = ref<'day' | 'app' | 'interface'>('day')
const list = ref<StatsDetailItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const datePreset = ref<'all' | '7' | '30' | '90' | 'custom'>('all')
const dateRange = ref<[Date, Date] | null>(null)
const showCustomDate = computed(() => datePreset.value === 'custom')

const indexMethod = (i: number) => (currentPage.value - 1) * pageSize.value + i + 1

async function load() {
  loading.value = true
  try {
    const result = await statsDailyPage({
      current: currentPage.value,
      size: pageSize.value,
      dimension: dimension.value,
      startDate: formatDate(dateRange.value?.[0]),
      endDate: formatDate(dateRange.value?.[1])
    })
    list.value = result.records
    total.value = Number(result.total)
  } finally {
    loading.value = false
  }
}

function formatDate(date?: Date): string | undefined {
  if (!date) return undefined
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function resetFilters() {
  datePreset.value = 'all'
  dateRange.value = null
  currentPage.value = 1
  load()
  syncRoute()
}

function onDatePresetChange() {
  const now = new Date()
  if (datePreset.value === 'all') {
    dateRange.value = null
  } else if (datePreset.value === 'custom') {
    // 自定义时等待用户选择日期
  } else {
    const start = new Date(now)
    start.setDate(start.getDate() - Number(datePreset.value) + 1)
    dateRange.value = [start, now]
  }
  currentPage.value = 1
  load()
  syncRoute()
}

function onDimensionChange() {
  currentPage.value = 1
  load()
  syncRoute()
}

function applyRouteFilters() {
  if (route.query.dimension === 'app' || route.query.dimension === 'interface') {
    dimension.value = route.query.dimension
  }
  if (['7', '30', '90', 'custom'].includes(String(route.query.range))) {
    datePreset.value = String(route.query.range) as '7' | '30' | '90' | 'custom'
  }
}

function syncRoute() {
  const query: Record<string, string> = {}
  if (dimension.value !== 'day') query.dimension = dimension.value
  if (datePreset.value !== 'all') query.range = datePreset.value
  router.replace({ query })
}

function openDetailLogs(row: StatsDetailItem) {
  const query: Record<string, string> = {}
  if (row.day) query.date = row.day
  if (row.appId) {
    query.appId = String(row.appId)
    if (row.appName) query.appName = row.appName
  }
  if (row.interfaceId) {
    query.interfaceId = String(row.interfaceId)
    if (row.interfaceName) query.interfaceName = row.interfaceName
  }
  router.push({ path: '/logs', query })
}

function reload() {
  currentPage.value = 1
  load()
}

function exportCsv() {
  if (!list.value.length) return
  const header = dimension.value === 'day'
    ? ['日期', '调用量', '成功', '失败', '成功率%', '平均耗时(ms)']
    : dimension.value === 'app'
      ? ['应用', '调用量', '成功', '失败', '成功率%', '平均耗时(ms)']
      : ['接口', '调用量', '成功', '失败', '成功率%', '平均耗时(ms)']
  const rows = list.value.map((row) => [
    row.day ?? row.appName ?? row.interfaceName ?? '',
    row.total,
    row.success,
    row.fail,
    row.successRate,
    row.avgCostMs
  ])
  const escape = (value: unknown) => `"${String(value).replace(/"/g, '""')}"`
  const csv = '\uFEFF' + [header, ...rows].map((line) => line.map(escape).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `stats-detail-${dimension.value}-${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

onActivated(() => {
  applyRouteFilters()
  load()
})
</script>

<style scoped>
.page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}
.page-heading h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -.02em;
  color: var(--app-text, #172033);
}
.page-subtitle {
  margin: 6px 0 0;
  color: var(--app-muted, #64748b);
  font-size: 13px;
}
.table-card {
  overflow: hidden;
}
</style>
