<template>
  <div>
    <div class="toolbar">
      <h2>API 日志</h2>
      <div class="toolbar-right">
        <el-input
          v-model="keyword"
          placeholder="搜索路径 / IP"
          clearable
          style="width: 220px"
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-button @click="reload">搜索</el-button>
      </div>
    </div>

    <div class="action-bar">
      <el-button size="small" type="primary" plain :disabled="!selectedRow" @click="openDetail(selectedRow)">
        查看详情
      </el-button>
      <el-button size="small" type="danger" plain :disabled="!selectedRow" @click="handleDelete(selectedRow)">
        删除
      </el-button>
      <el-button size="small" type="danger" :disabled="selected.length === 0" @click="handleBatchDelete">
        批量删除
      </el-button>
      <span v-if="selected.length" class="batch-tip">已选 {{ selected.length }} 项</span>
    </div>

    <el-table
      :data="logs"
      border
      stripe
      @selection-change="(rows: ApiLog[]) => (selected = rows)"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column type="index" label="#" width="60" :index="indexMethod" />
      <el-table-column prop="createTime" label="时间" width="160" />
      <el-table-column prop="interfaceName" label="接口" width="120" />
      <el-table-column prop="method" label="方式" width="80">
        <template #default="{ row }">
          <el-tag :type="row.method === 'GET' ? 'success' : 'warning'">{{ row.method }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路径" min-width="160" />
      <el-table-column prop="appName" label="应用" width="120" />
      <el-table-column prop="userAccount" label="用户" width="120" />
      <el-table-column prop="ip" label="IP" width="130" />
      <el-table-column prop="statusCode" label="状态码" width="90">
        <template #default="{ row }">
          <el-tag :type="row.statusCode < 400 ? 'success' : 'danger'" size="small">
            {{ row.statusCode }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="costMs" label="耗时(ms)" width="90" />
    </el-table>

    <el-pagination
      class="pagination"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pageSize"
      v-model:current-page="currentPage"
      @current-change="load"
    />

    <el-dialog v-model="detailVisible" :title="`日志详情 #${detail?.id ?? ''}`" width="720px">
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
      <h4>请求参数</h4>
      <pre class="json-block">{{ prettyJson(detail?.requestParams) || '-' }}</pre>
      <h4>响应体</h4>
      <pre class="json-block">{{ prettyJson(detail?.responseBody) || '-' }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listApiLogs, getApiLog, deleteApiLog, deleteApiLogs, type ApiLog } from '@/api'

const logs = ref<ApiLog[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 10
const keyword = ref('')
const selected = ref<ApiLog[]>([])
const detailVisible = ref(false)
const detail = ref<ApiLog | null>(null)

const selectedRow = computed(() => (selected.value.length === 1 ? selected.value[0] : null))
const indexMethod = (i: number) => (currentPage.value - 1) * pageSize + i + 1

async function load() {
  const page = await listApiLogs({
    current: currentPage.value,
    size: pageSize,
    keyword: keyword.value.trim() || undefined
  })
  logs.value = page.records
  total.value = page.total
}

function reload() {
  currentPage.value = 1
  load()
}

async function openDetail(row: ApiLog | null) {
  if (!row) return
  detail.value = await getApiLog(row.id)
  detailVisible.value = true
}

async function handleDelete(row: ApiLog | null) {
  if (!row) return
  await ElMessageBox.confirm(`确定删除日志 #${row.id} 吗？`, '删除日志', {
    type: 'warning'
  })
  await deleteApiLog(row.id)
  ElMessage.success('已删除')
  await load()
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定删除选中的 ${selected.value.length} 条日志吗？`, '批量删除', {
    type: 'warning'
  })
  await deleteApiLogs(selected.value.map((l) => l.id))
  ElMessage.success('已批量删除')
  await load()
}

function prettyJson(value?: string): string {
  if (!value) return ''
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
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
.batch-tip {
  color: #909399;
  font-size: 13px;
}
.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
.json-block {
  background: #f5f7fa;
  border-radius: 4px;
  padding: 12px;
  max-height: 240px;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
}
</style>
