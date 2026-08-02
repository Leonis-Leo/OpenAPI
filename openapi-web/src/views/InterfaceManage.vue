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
        </template>
      </el-table-column>
    </el-table>

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
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  listApps,
  type AppInfo,
  listInterfaces,
  listAllInterfaces,
  interfaceDetail,
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
