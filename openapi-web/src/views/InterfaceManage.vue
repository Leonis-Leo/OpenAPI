<template>
  <div>
    <div class="toolbar">
      <h2>接口管理</h2>
      <div class="toolbar-right">
        <el-input
          v-model="keyword"
          placeholder="搜索名称 / 路径"
          clearable
          style="width: 220px"
          @input="currentPage = 1"
        />
        <el-button v-if="isAdmin" type="primary" @click="openCreateForm">新增接口</el-button>
      </div>
    </div>

    <div v-if="isAdmin" class="batch-bar">
      <el-button size="small" :disabled="selectedInterfaces.length === 0" @click="batchStatus(1)">
        批量上线
      </el-button>
      <el-button size="small" :disabled="selectedInterfaces.length === 0" @click="batchStatus(0)">
        批量下线
      </el-button>
      <el-button
        size="small"
        type="danger"
        :disabled="selectedInterfaces.length === 0"
        @click="batchDelete"
      >
        批量删除
      </el-button>
      <span v-if="selectedInterfaces.length" class="batch-tip">已选 {{ selectedInterfaces.length }} 项</span>
    </div>

    <el-table
      :data="pagedInterfaces"
      border
      stripe
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" />
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
      <el-table-column label="操作" width="300">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="openDetail(row)">详情/调试</el-button>
          <el-button
            size="small"
            :disabled="subscribeMap[row.id] === 0 || subscribeMap[row.id] === 1"
            @click="openSubscribe(row)"
          >
            {{ subscribeMap[row.id] === 1 ? '已订阅' : subscribeMap[row.id] === 0 ? '已申请' : '订阅' }}
          </el-button>
          <el-button
            v-if="subscribeMap[row.id] === 1"
            size="small"
            type="danger"
            plain
            @click="handleUnsubscribe(row)"
          >
            取消订阅
          </el-button>
          <el-button
            v-if="isAdmin"
            size="small"
            :type="row.status === 1 ? 'danger' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 1 ? '下线' : '上线' }}
          </el-button>
          <el-button v-if="isAdmin" size="small" @click="openEditForm(row)">编辑</el-button>
          <el-button v-if="isAdmin" size="small" type="danger" plain @click="handleDeleteInterface(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pagination"
      layout="total, prev, pager, next"
      :total="filteredInterfaces.length"
      :page-size="pageSize"
      v-model:current-page="currentPage"
    />

    <el-dialog v-model="formVisible" :title="editingId ? '编辑接口' : '新增接口'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="名称"><el-input v-model="interfaceForm.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="interfaceForm.description" /></el-form-item>
        <el-form-item label="方式">
          <el-select v-model="interfaceForm.method" style="width: 120px">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
          </el-select>
        </el-form-item>
        <el-form-item label="路径"><el-input v-model="interfaceForm.url" placeholder="/api/xxx" /></el-form-item>
        <el-form-item label="请求参数">
          <el-input v-model="interfaceForm.requestParams" type="textarea" :rows="3" placeholder='JSON，如 {"key":"说明"}' />
        </el-form-item>
        <el-form-item label="响应示例">
          <el-input v-model="interfaceForm.responseExample" type="textarea" :rows="3" placeholder="JSON 响应示例" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingForm" @click="handleSaveForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="`接口详情 - ${debugInterface?.name ?? ''}`" width="720px">
      <el-tabs v-model="detailTab">
        <el-tab-pane label="接口信息" name="info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="名称">{{ debugInterface?.name }}</el-descriptions-item>
            <el-descriptions-item label="方式">{{ debugInterface?.method }}</el-descriptions-item>
            <el-descriptions-item label="路径" :span="2">{{ debugInterface?.url }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ debugInterface?.description }}</el-descriptions-item>
          </el-descriptions>
          <h4>请求参数说明</h4>
          <pre class="json-block">{{ prettyJson(debugInterface?.requestParams) }}</pre>
          <h4>响应示例</h4>
          <pre class="json-block">{{ prettyJson(debugInterface?.responseExample) }}</pre>
        </el-tab-pane>
        <el-tab-pane label="在线调试" name="debug">
          <el-form label-width="90px">
            <el-form-item label="调试应用">
              <el-select v-model="debugAppId" placeholder="选择应用（需已订阅）" style="width: 100%">
                <el-option v-for="app in apps" :key="app.id" :label="app.appName" :value="app.id" />
              </el-select>
            </el-form-item>
            <el-form-item v-for="key in debugParamKeys" :key="key" :label="key">
              <el-input v-model="debugParams[key]" :placeholder="paramDescription(key)" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="debugLoading" @click="handleDebug">发送请求</el-button>
            </el-form-item>
          </el-form>
          <pre v-if="debugResult" class="json-block">{{ debugResult }}</pre>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

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
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  listApps,
  type AppInfo,
  listInterfaces,
  listAllInterfaces,
  interfaceDetail,
  createInterface,
  updateInterface,
  deleteInterface,
  type InterfaceForm,
  onlineInterface,
  offlineInterface,
  subscribe,
  mySubscribes,
  unsubscribe,
  type InterfaceInfo
} from '@/api'
import { hmacSha256Hex, buildSignContent, type SignParams } from '@/utils/sign'

const userStore = useUserStore()
const isAdmin = userStore.user?.userRole === 'admin'
const interfaces = ref<InterfaceInfo[]>([])
const keyword = ref('')
const currentPage = ref(1)
const pageSize = 10
const apps = ref<AppInfo[]>([])
const subscribeMap = ref<Record<number, number>>({})
const subscribeIdMap = ref<Record<number, number>>({})
const subscribeVisible = ref(false)
const currentInterface = ref<InterfaceInfo | null>(null)
const selectedAppId = ref<number | null>(null)
const subscribing = ref(false)

const detailVisible = ref(false)
const detailTab = ref('info')
const debugInterface = ref<InterfaceInfo | null>(null)
const debugAppId = ref<number | null>(null)
const debugParams = ref<Record<string, string>>({})
const debugResult = ref('')
const debugLoading = ref(false)
const debugParamKeys = computed(() => Object.keys(debugParams.value))

const filteredInterfaces = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return interfaces.value
  return interfaces.value.filter(
    (i) => i.name.toLowerCase().includes(kw) || i.url.toLowerCase().includes(kw)
  )
})

const pagedInterfaces = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredInterfaces.value.slice(start, start + pageSize)
})

watch(filteredInterfaces, () => {
  const max = Math.max(1, Math.ceil(filteredInterfaces.value.length / pageSize))
  if (currentPage.value > max) {
    currentPage.value = max
  }
})

const formVisible = ref(false)
const editingId = ref<number | null>(null)
const savingForm = ref(false)
const interfaceForm = ref<InterfaceForm>({
  name: '',
  description: '',
  method: 'GET',
  url: '',
  requestParams: '',
  responseExample: ''
})
const selectedInterfaces = ref<InterfaceInfo[]>([])

function openCreateForm() {
  editingId.value = null
  interfaceForm.value = { name: '', description: '', method: 'GET', url: '', requestParams: '', responseExample: '' }
  formVisible.value = true
}

function openEditForm(row: InterfaceInfo) {
  editingId.value = row.id
  interfaceForm.value = {
    name: row.name,
    description: row.description ?? '',
    method: row.method,
    url: row.url,
    requestParams: row.requestParams ?? '',
    responseExample: row.responseExample ?? ''
  }
  formVisible.value = true
}

async function handleSaveForm() {
  if (!interfaceForm.value.name.trim() || !interfaceForm.value.url.trim()) {
    ElMessage.warning('请填写名称和路径')
    return
  }
  savingForm.value = true
  try {
    const data = { ...interfaceForm.value }
    if (editingId.value) {
      await updateInterface(editingId.value, data)
      ElMessage.success('已保存')
    } else {
      await createInterface(data)
      ElMessage.success('已创建（默认下线，可上线发布）')
    }
    formVisible.value = false
    await load()
  } finally {
    savingForm.value = false
  }
}

async function handleDeleteInterface(row: InterfaceInfo) {
  await ElMessageBox.confirm(`确定删除接口「${row.name}」吗？`, '删除接口', {
    type: 'warning'
  })
  await deleteInterface(row.id)
  ElMessage.success('已删除')
  await load()
}

function handleSelectionChange(rows: InterfaceInfo[]) {
  selectedInterfaces.value = rows
}

async function batchStatus(status: number) {
  const ops = selectedInterfaces.value.map((i) =>
    status === 1 ? onlineInterface(i.id) : offlineInterface(i.id)
  )
  await Promise.all(ops)
  ElMessage.success(status === 1 ? '已批量上线' : '已批量下线')
  await load()
}

async function batchDelete() {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedInterfaces.value.length} 个接口吗？`, '批量删除', {
    type: 'warning'
  })
  await Promise.all(selectedInterfaces.value.map((i) => deleteInterface(i.id)))
  ElMessage.success('已批量删除')
  await load()
}

async function load() {
  interfaces.value = isAdmin ? await listAllInterfaces() : await listInterfaces()
  const subscribes = await mySubscribes()
  const map: Record<number, number> = {}
  const idMap: Record<number, number> = {}
  subscribes.forEach((s) => {
    map[s.interfaceId] = s.status
    idMap[s.interfaceId] = s.id
  })
  subscribeMap.value = map
  subscribeIdMap.value = idMap
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

async function handleUnsubscribe(row: InterfaceInfo) {
  const subscribeId = subscribeIdMap.value[row.id]
  if (!subscribeId) {
    return
  }
  await ElMessageBox.confirm(`确定取消订阅「${row.name}」吗？`, '取消订阅', {
    type: 'warning'
  })
  await unsubscribe(subscribeId)
  ElMessage.success('已取消订阅')
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

function paramDescription(key: string): string {
  const raw = debugInterface.value?.requestParams
  if (!raw) return '请输入参数值'
  try {
    const obj = JSON.parse(raw)
    return String(obj[key] ?? '请输入参数值')
  } catch {
    return '请输入参数值'
  }
}

async function openDetail(row: InterfaceInfo) {
  const detail = await interfaceDetail(row.id)
  debugInterface.value = detail
  debugParams.value = {}
  const keys = Object.keys(parseRequestParams(detail.requestParams))
  keys.forEach((k) => (debugParams.value[k] = ''))
  debugAppId.value = apps.value[0]?.id ?? null
  debugResult.value = ''
  detailTab.value = 'info'
  detailVisible.value = true
}

function parseRequestParams(json?: string): Record<string, string> {
  if (!json) return {}
  try {
    return JSON.parse(json)
  } catch {
    return {}
  }
}

async function handleDebug() {
  const info = debugInterface.value
  const app = apps.value.find((a) => a.id === debugAppId.value)
  if (!info || !app) {
    ElMessage.warning('请选择调试应用')
    return
  }
  const params: SignParams = {}
  Object.keys(debugParams.value).forEach((k) => {
    const v = (debugParams.value[k] ?? '').trim()
    if (v !== '') params[k] = v
  })
  const timestamp = String(Date.now())
  const nonce = Math.random().toString(36).slice(2, 10)
  const signParams: SignParams = { ...params, timestamp, nonce }
  const content = buildSignContent(info.method, info.url, signParams)
  const signature = await hmacSha256Hex(content, app.secretKey)
  const headers: Record<string, string> = {
    'X-Access-Key': app.accessKey,
    'X-Timestamp': timestamp,
    'X-Nonce': nonce,
    'X-Signature': signature
  }
  debugLoading.value = true
  try {
    let resp: Response
    const qs = new URLSearchParams(params).toString()
    if (info.method === 'GET') {
      resp = await fetch(info.url + (qs ? '?' + qs : ''), { headers })
    } else {
      resp = await fetch(info.url, {
        method: 'POST',
        headers: { ...headers, 'Content-Type': 'application/x-www-form-urlencoded' },
        body: qs
      })
    }
    debugResult.value = `HTTP ${resp.status}\n${await resp.text()}`
  } catch (e) {
    debugResult.value = `请求失败: ${(e as Error).message}`
  } finally {
    debugLoading.value = false
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
.batch-bar {
  display: flex;
  align-items: center;
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
  max-height: 260px;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
}
</style>
