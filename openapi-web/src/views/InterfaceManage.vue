<template>
  <div>
    <div class="toolbar">
      <h2>接口管理</h2>
      <div class="toolbar-right">
        <el-radio-group v-model="filterStatus" size="default" @change="onFilterChange">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="online">已上线</el-radio-button>
          <el-radio-button value="subscribed">已订阅</el-radio-button>
          <el-radio-button value="unsubscribed">未订阅</el-radio-button>
        </el-radio-group>
        <el-input
          v-model="keywordInput"
          placeholder="搜索名称 / 路径"
          clearable
          style="width: 220px"
          @input="onKeywordInput"
        />
        <el-button v-if="isAdmin" type="primary" @click="openCreateForm">新增接口</el-button>
      </div>
    </div>

            <div class="action-bar">
      <el-button class="action-secondary" size="small" type="primary" plain :disabled="!selectedRow" @click="openDetail(selectedRow)">
        详情/调试
      </el-button>
      <el-button class="action-primary" size="small" type="primary" plain :disabled="selected.length === 0" @click="openSubscribe">
        订阅
      </el-button>
      <el-button
        size="small"
        type="danger"
        plain
        :disabled="selected.length === 0"
        @click="handleUnsubscribe"
      >
        取消订阅
      </el-button>
      <template v-if="isAdmin">
        <el-button size="small" type="success" :disabled="selected.length === 0" @click="toggleStatus(1)">
          上线
        </el-button>
        <el-button size="small" type="warning" :disabled="selected.length === 0" @click="toggleStatus(0)">
          下线
        </el-button>
        <el-button class="action-secondary" size="small" type="primary" plain :disabled="!selectedRow" @click="openEditForm(selectedRow)">编辑</el-button>
        <el-button class="danger-right" size="small" type="danger" plain :disabled="selected.length === 0" @click="handleDeleteInterface">
          删除
        </el-button>
      </template>
      <span v-if="selected.length" class="batch-tip">已选 {{ selected.length }} 项</span>
    </div>

    <el-table
      ref="tableRef"
      :data="pagedInterfaces"
      border
      stripe
      v-loading="loading"
      @row-click="handleRowClick"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column type="index" label="#" width="60" :index="indexMethod" />
      <el-table-column label="名称">
        <template #default="{ row }">
          <el-link type="primary" @click="openDetail(row)">{{ row.name }}</el-link>
        </template>
      </el-table-column>
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
    </el-table>

    <el-pagination
      class="pagination"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      @current-change="handlePageChange"
      @size-change="handlePageChange"
    />

    <el-dialog v-model="detailVisible" :title="`接口详情 - ${debugInterface?.name ?? ''}`" width="720px">
      <el-tabs v-model="detailTab">
        <el-tab-pane label="接口信息" name="info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="名称">{{ debugInterface?.name }}</el-descriptions-item>
            <el-descriptions-item label="方式">{{ debugInterface?.method }}</el-descriptions-item>
            <el-descriptions-item label="路径" :span="2">{{ debugInterface?.url }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ debugInterface?.description }}</el-descriptions-item>
          </el-descriptions>
          <div class="block-toolbar">
            <span>请求参数说明</span>
            <el-button size="small" plain @click="copyText(debugInterface?.requestParams)">复制</el-button>
          </div>
          <pre class="json-block" v-html="highlightJson(prettyJson(debugInterface?.requestParams))"></pre>
          <div class="block-toolbar">
            <span>响应示例</span>
            <el-button size="small" plain @click="copyText(debugInterface?.responseExample)">复制</el-button>
          </div>
          <pre class="json-block" v-html="highlightJson(prettyJson(debugInterface?.responseExample))"></pre>
        </el-tab-pane>
        <el-tab-pane label="调用示例" name="sample">
          <el-form label-width="90px">
            <el-form-item label="示例应用">
              <el-select v-model="sampleAppId" placeholder="选择应用" style="width: 100%">
                <el-option v-for="app in apps" :key="app.id" :label="app.appName" :value="app.id" />
              </el-select>
            </el-form-item>
          </el-form>
          <div class="block-toolbar">
            <span>Java (Hutool + HMAC-SHA256)</span>
            <el-button size="small" plain @click="copyText(javaSample)">复制</el-button>
          </div>
          <pre class="json-block">{{ javaSample || '-' }}</pre>
          <div class="block-toolbar">
            <span>curl</span>
            <el-button size="small" plain @click="copyText(curlSample)">复制</el-button>
          </div>
          <pre class="json-block">{{ curlSample || '-' }}</pre>
        </el-tab-pane>
        <el-tab-pane label="在线调试" name="debug">
          <div class="debug-header">
            <el-tag :type="debugInterface?.method === 'GET' ? 'success' : 'warning'" size="small">
              {{ debugInterface?.method }}
            </el-tag>
            <span class="debug-url">{{ debugInterface?.url }}</span>
          </div>
          <el-form label-width="90px" class="debug-form">
            <el-form-item label="调试应用">
              <el-select v-model="debugAppId" placeholder="选择应用（需已订阅）" style="width: 100%">
                <el-option v-for="app in apps" :key="app.id" :label="app.appName" :value="app.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="请求参数">
              <div class="debug-param-head">
                <el-radio-group v-model="debugBodyMode" size="small">
                  <el-radio-button value="form">表单参数</el-radio-button>
                  <el-radio-button value="json">JSON Body</el-radio-button>
                </el-radio-group>
                <el-select v-model="selectedHistoryId" clearable size="small" placeholder="请求历史" class="history-select" @change="loadHistoryItem">
                  <el-option v-for="item in debugHistory" :key="item.id" :label="item.label" :value="item.id" />
                </el-select>
              </div>
              <el-input
                v-model="debugParamsJson"
                type="textarea"
                :rows="4"
                :placeholder="debugPlaceholder"
              />
            </el-form-item>
            <el-form-item label="请求头">
              <el-input
                v-model="debugHeadersJson"
                type="textarea"
                :rows="3"
                placeholder='JSON 格式，如 {"X-Custom":"1"}；签名相关请求头会自动附加'
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="debugLoading" @click="handleDebug">
                发送请求
              </el-button>
              <el-button v-if="debugBody || debugStatus !== null" @click="clearDebug">清空</el-button>
              <el-button v-if="debugHistory.length" text type="danger" @click="clearHistory">清空历史</el-button>
            </el-form-item>
          </el-form>
          <div v-if="debugBody || debugStatus !== null" class="debug-result">
            <div class="debug-result-head">
              <el-tag v-if="debugStatus !== null" :type="debugStatus < 400 ? 'success' : 'danger'" size="small">
                HTTP {{ debugStatus }}
              </el-tag>
              <el-tag v-else type="danger" size="small">请求失败</el-tag>
              <span v-if="debugCostMs !== null" class="debug-cost">{{ debugCostMs }} ms</span>
              <el-button size="small" plain class="debug-copy" @click="copyCurl">复制 Curl</el-button>
            </div>
            <pre class="debug-body" v-html="debugBodyHtml"></pre>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog v-model="subscribeVisible" title="订阅接口" width="420px">
      <el-form label-width="80px">
        <el-form-item label="接口">
          <el-input :model-value="subscribeTargets.map((t) => t.name).join('、')" disabled />
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableInstance } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  listAppsForDebug,
  type AppInfo,
  listInterfaces,
  listAllInterfaces,
  pageInterfaces,
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
const loading = ref(false)
const keyword = ref('')
const keywordInput = ref('')
const filterStatus = ref<'all' | 'online' | 'subscribed' | 'unsubscribed'>('all')
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const apps = ref<AppInfo[]>([])
const subscribeMap = ref<Record<number, number>>({})
const subscribeIdMap = ref<Record<number, number>>({})
const subscribeVisible = ref(false)
const currentInterface = ref<InterfaceInfo | null>(null)
const subscribeTargets = ref<InterfaceInfo[]>([])
const selectedAppId = ref<number | null>(null)
const subscribing = ref(false)
const selected = ref<InterfaceInfo[]>([])
const tableRef = ref<TableInstance>()

const selectedRow = computed(() => (selected.value.length === 1 ? selected.value[0] : null))
const indexMethod = (i: number) => (currentPage.value - 1) * pageSize.value + i + 1
let keywordTimer: ReturnType<typeof setTimeout> | undefined

function onKeywordInput() {
  clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => {
    keyword.value = keywordInput.value
    currentPage.value = 1
    load()
  }, 300)
}

function onFilterChange() {
  currentPage.value = 1
  load()
}

function handlePageChange() {
  clearSelection()
  load()
}

const detailVisible = ref(false)
const detailTab = ref('info')
const debugInterface = ref<InterfaceInfo | null>(null)
const debugAppId = ref<number | null>(null)
const sampleAppId = ref<number | null>(null)
const debugParamsJson = ref('')
const debugHeadersJson = ref('')
const debugBodyMode = ref<'form' | 'json'>('form')
const selectedHistoryId = ref<string>()
interface DebugHistoryItem {
  id: string
  label: string
  mode: 'form' | 'json'
  params: string
  headers: string
}
const debugHistory = ref<DebugHistoryItem[]>([])
const debugPlaceholder = computed(() => debugBodyMode.value === 'json'
  ? 'JSON Body，例如 {"name":"Alice"}'
  : 'JSON 格式，例如 {"name":"Alice"}；GET 拼接为查询参数，POST 作为表单参数')
const debugStatus = ref<number | null>(null)
const debugCostMs = ref<number | null>(null)
const debugBody = ref('')
const debugLoading = ref(false)

const debugBodyHtml = computed(() => {
  const body = debugBody.value
  if (!body) return ''
  const json = prettyJson(body)
  return highlightJson(json)
})

const javaSample = computed(() => {
  const info = debugInterface.value
  const app = apps.value.find((a) => a.id === sampleAppId.value)
  if (!info || !app) return ''
  const paramLines = sampleParamKeys(info.requestParams)
    .map((k) => `        params.put("${k}", "");`)
    .join('\n')
  const isGet = info.method === 'GET'
  const requestLine = isGet
    ? `        String url = "${info.url}" + (params.isEmpty() ? "" : "?" + HttpUtil.toParams(params));
        HttpRequest request = HttpRequest.get(url)`
    : `        String url = "${info.url}";
        HttpRequest request = HttpRequest.post(url)
                .form(params)`
  return `// 依赖：hutool-all 5.8.x（或使用 JDK 自带 HttpURLConnection）
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.http.HttpRequest;
import java.util.Map;
import java.util.TreeMap;

public class OpenApiDemo {
    public static void main(String[] args) {
        String accessKey = "${app.accessKey}";
        String secretKey = "${app.secretKey}";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = String.valueOf((int) (Math.random() * 1000000));

        // 请求参数（按 key 排序后拼接参与签名）
        Map<String, String> params = new TreeMap<>();
${paramLines || '        // params.put("name", "Alice");'}

        params.put("timestamp", timestamp);
        params.put("nonce", nonce);
        StringBuilder content = new StringBuilder();
        content.append("${info.method}").append("\\n");
        content.append("${info.url}").append("\\n");
        params.forEach((k, v) -> content.append(k).append("=").append(v).append("&"));
        String sign = new HMac(HmacAlgorithm.HmacSHA256, secretKey.getBytes())
                .digestHex(content.substring(0, content.length() - 1));

${requestLine}
                .header("X-Access-Key", accessKey)
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signature", sign);
        System.out.println(request.execute().body());
    }
}`
})

const curlSample = computed(() => {
  const info = debugInterface.value
  const app = apps.value.find((a) => a.id === sampleAppId.value)
  if (!info || !app) return ''
  const timestamp = String(Date.now())
  const nonce = Math.random().toString(36).slice(2, 10)
  const content = buildSignContent(info.method, info.url, { timestamp, nonce })
  const parts = ['curl -X ' + info.method]
  ;['X-Access-Key', 'X-Timestamp', 'X-Nonce', 'X-Signature'].forEach((k) => {
    const v =
      k === 'X-Signature'
        ? '<计算后的签名>'
        : k === 'X-Access-Key'
          ? app.accessKey
          : k === 'X-Timestamp'
            ? timestamp
            : nonce
    parts.push(`-H '${k}: ${v}'`)
  })
  parts.push(`'${info.url}'`)
  return `${parts.join(' \\\n  ')}

# 签名算法：HMAC-SHA256("${info.method}\\n${info.url}\\nkey1=v1&key2=v2&nonce=${nonce}&timestamp=${timestamp}", secretKey)`
})

async function buildCurl(): Promise<string> {
  const info = debugInterface.value
  const app = apps.value.find((a) => a.id === debugAppId.value)
  if (!info || !app) return ''
  const params = parseJsonObject(debugParamsJson.value, '请求参数') ?? {}
  const customHeaders = parseJsonObject(debugHeadersJson.value, '请求头') ?? {}
  const timestamp = String(Date.now())
  const nonce = Math.random().toString(36).slice(2, 10)
  const signParams: SignParams = { ...params, timestamp, nonce }
  const content = buildSignContent(info.method, info.url, signParams)
  const signature = await hmacSha256Hex(content, app.secretKey!)
  const parts = ['curl -X ' + info.method]
  ;['X-Access-Key', 'X-Timestamp', 'X-Nonce', 'X-Signature'].forEach((k) => {
    const v =
      k === 'X-Signature'
        ? signature
        : k === 'X-Access-Key'
          ? app.accessKey
          : k === 'X-Timestamp'
            ? timestamp
            : nonce
    parts.push(`-H '${k}: ${v}'`)
  })
  Object.entries(customHeaders).forEach(([k, v]) => parts.push(`-H '${k}: ${v}'`))
  const qs = new URLSearchParams(params).toString()
  if (info.method === 'GET') {
    parts.push(`'${info.url}${qs ? '?' + qs : ''}'`)
  } else {
    parts.push(`'${info.url}'`)
    if (debugBodyMode.value === 'json') {
      parts.push(`-H 'Content-Type: application/json'`)
      parts.push(`--data '${JSON.stringify(params)}'`)
    } else if (qs) {
      parts.push(`-H 'Content-Type: application/x-www-form-urlencoded'`)
      parts.push(`--data '${qs}'`)
    }
  }
  return parts.join(' \\\n  ')
}

async function copyCurl() {
  const curl = await buildCurl()
  if (!curl) return
  try {
    await navigator.clipboard.writeText(curl)
    ElMessage.success('Curl 已复制')
  } catch {
    ElMessage.error('复制失败')
  }
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

const filteredInterfaces = computed(() => {
  if (filterStatus.value === 'all' || filterStatus.value === 'online') return interfaces.value
  const kw = keyword.value.trim().toLowerCase()
  return interfaces.value.filter((i) => {
    const matchKw =
      !kw || i.name.toLowerCase().includes(kw) || i.url.toLowerCase().includes(kw)
    if (!matchKw) return false
    if (filterStatus.value === 'online') return i.status === 1
    if (filterStatus.value === 'subscribed') return subscribeMap.value[i.id] === 1
    if (filterStatus.value === 'unsubscribed') {
      return subscribeMap.value[i.id] === undefined || subscribeMap.value[i.id] === 2
    }
    return true
  })
})

const pagedInterfaces = computed(() => {
  if (filterStatus.value === 'all' || filterStatus.value === 'online') return filteredInterfaces.value
  const start = (currentPage.value - 1) * pageSize.value
  return filteredInterfaces.value.slice(start, start + pageSize.value)
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

async function load() {
  loading.value = true
  try {
    if (filterStatus.value === 'all' || filterStatus.value === 'online') {
      const result = await pageInterfaces({
        current: currentPage.value,
        size: pageSize.value,
        keyword: keyword.value || undefined,
        status: filterStatus.value === 'online' ? 1 : undefined
      })
      interfaces.value = result.records
      total.value = result.total
    } else {
      interfaces.value = isAdmin ? await listAllInterfaces() : await listInterfaces()
      total.value = filteredInterfaces.value.length
    }
    const subscribes = await mySubscribes()
    const map: Record<number, number> = {}
    const idMap: Record<number, number> = {}
    subscribes.forEach((s) => {
      map[s.interfaceId] = s.status
      idMap[s.interfaceId] = s.id
    })
    subscribeMap.value = map
    subscribeIdMap.value = idMap
    if (filterStatus.value === 'subscribed' || filterStatus.value === 'unsubscribed') {
      total.value = filteredInterfaces.value.length
      const maxPage = Math.max(1, Math.ceil(total.value / pageSize.value))
      if (currentPage.value > maxPage) currentPage.value = maxPage
    }
    if (userStore.user) {
      apps.value = await listAppsForDebug()
    }
  } finally {
    loading.value = false
  }
}

function openSubscribe() {
  const rows = selected.value
  if (rows.length === 0) return
  const pending = rows.filter((r) => subscribeMap.value[r.id] === 0)
  const subscribed = rows.filter((r) => subscribeMap.value[r.id] === 1)
  const targets = rows.filter(
    (r) => subscribeMap.value[r.id] === undefined || subscribeMap.value[r.id] === 2
  )
  if (pending.length > 0) {
    ElMessage.warning(`${pending.length} 个接口的订阅申请待审批中，已跳过`)
  }
  if (subscribed.length > 0) {
    ElMessage.info(`${subscribed.length} 个接口已订阅，已跳过`)
  }
  if (targets.length === 0) {
    ElMessage.warning('选中的接口均已订阅或待审批，无可申请项')
    return
  }
  currentInterface.value = targets[0]
  subscribeTargets.value = targets
  selectedAppId.value = apps.value[0]?.id ?? null
  if (apps.value.length === 0) {
    ElMessage.warning('请先在「应用管理」创建应用，再订阅接口')
    return
  }
  subscribeVisible.value = true
}

async function handleSubscribe() {
  if (subscribeTargets.value.length === 0 || !selectedAppId.value) {
    return
  }
  subscribing.value = true
  try {
    const results = await Promise.allSettled(
      subscribeTargets.value.map((t) => subscribe(t.id, selectedAppId.value as number))
    )
    summarizeResults(results, subscribeTargets.value.length, '订阅申请提交')
    subscribeVisible.value = false
    await load()
  } finally {
    subscribing.value = false
  }
}

async function toggleStatus(status: number) {
  const rows = selected.value
  if (rows.length === 0) return
  if (rows.length > 1) {
    await ElMessageBox.confirm(`确定对选中的 ${rows.length} 个接口执行「${status === 1 ? '上线' : '下线'}」吗？`, '操作确认', { type: 'warning' })
  }
  const ops = rows.map((i) => (status === 1 ? onlineInterface(i.id) : offlineInterface(i.id)))
  const results = await Promise.allSettled(ops)
  summarizeResults(results, rows.length, status === 1 ? '上线' : '下线')
  await load()
}

async function handleUnsubscribe() {
  const rows = selected.value
  if (rows.length === 0) return
  const targets = rows.filter((i) => subscribeIdMap.value[i.id])
  if (targets.length === 0) return
  const msg = targets.length > 1
    ? `确定取消选中的 ${targets.length} 个订阅吗？`
    : `确定取消订阅「${targets[0].name}」吗？`
  await ElMessageBox.confirm(msg, '取消订阅', { type: 'warning' })
  const results = await Promise.allSettled(
    targets.map((i) => unsubscribe(subscribeIdMap.value[i.id]))
  )
  summarizeResults(results, targets.length, '取消订阅')
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

async function openDetail(row: InterfaceInfo | null) {
  if (!row) return
  const detail = await interfaceDetail(row.id)
  debugInterface.value = detail
  debugParamsJson.value = buildExampleParams(detail.requestParams)
  debugHeadersJson.value = ''
  debugAppId.value = apps.value[0]?.id ?? null
  sampleAppId.value = apps.value[0]?.id ?? null
  debugStatus.value = null
  debugCostMs.value = null
  debugBody.value = ''
  debugBodyMode.value = 'form'
  loadHistory()
  detailTab.value = 'info'
  detailVisible.value = true
}

function buildExampleParams(requestParams?: string): string {
  if (!requestParams) return ''
  try {
    const obj = JSON.parse(requestParams)
    if (obj === null || typeof obj !== 'object' || Array.isArray(obj)) return ''
    const example: Record<string, string> = {}
    Object.keys(obj).forEach((k) => {
      example[k] = ''
    })
    return JSON.stringify(example, null, 2)
  } catch {
    return ''
  }
}

function sampleParamKeys(requestParams?: string): string[] {
  if (!requestParams) return []
  try {
    const obj = JSON.parse(requestParams)
    if (obj === null || typeof obj !== 'object' || Array.isArray(obj)) return []
    return Object.keys(obj)
  } catch {
    return []
  }
}

function clearDebug() {
  debugStatus.value = null
  debugCostMs.value = null
  debugBody.value = ''
}

function historyKey() {
  return `openapi-debug-history-${debugInterface.value?.id ?? 'unknown'}`
}

function loadHistory() {
  try {
    debugHistory.value = JSON.parse(localStorage.getItem(historyKey()) || '[]')
  } catch {
    debugHistory.value = []
  }
  selectedHistoryId.value = undefined
}

function loadHistoryItem(id?: string) {
  const item = debugHistory.value.find((history) => history.id === id)
  if (!item) return
  debugBodyMode.value = item.mode
  debugParamsJson.value = item.params
  debugHeadersJson.value = item.headers
}

function saveHistory() {
  const item: DebugHistoryItem = {
    id: `${Date.now()}`,
    label: `${debugBodyMode.value === 'json' ? 'JSON' : '表单'} · ${new Date().toLocaleTimeString()}`,
    mode: debugBodyMode.value,
    params: debugParamsJson.value,
    headers: debugHeadersJson.value
  }
  debugHistory.value = [item, ...debugHistory.value].slice(0, 10)
  localStorage.setItem(historyKey(), JSON.stringify(debugHistory.value))
  selectedHistoryId.value = item.id
}

function clearHistory() {
  debugHistory.value = []
  selectedHistoryId.value = undefined
  localStorage.removeItem(historyKey())
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

async function handleDebug() {
  const info = debugInterface.value
  const app = apps.value.find((a) => a.id === debugAppId.value)
  if (!info || !app) {
    ElMessage.warning('请选择调试应用')
    return
  }
  const params = parseJsonObject(debugParamsJson.value, '请求参数')
  if (!params) return
  const customHeaders = parseJsonObject(debugHeadersJson.value, '请求头')
  if (!customHeaders) return
  const timestamp = String(Date.now())
  const nonce = Math.random().toString(36).slice(2, 10)
  const signParams: SignParams = { ...params, timestamp, nonce }
  const content = buildSignContent(info.method, info.url, signParams)
  const signature = await hmacSha256Hex(content, app.secretKey!)
  const headers: Record<string, string> = {
    'X-Access-Key': app.accessKey,
    'X-Timestamp': timestamp,
    'X-Nonce': nonce,
    'X-Signature': signature,
    ...customHeaders
  }
  debugStatus.value = null
  debugCostMs.value = null
  debugBody.value = ''
  debugLoading.value = true
  const start = performance.now()
  try {
    let resp: Response
    const qs = new URLSearchParams(params).toString()
    if (info.method === 'GET') {
      resp = await fetch(info.url + (qs ? '?' + qs : ''), { headers })
    } else {
      resp = await fetch(info.url, {
        method: 'POST',
        headers: {
          ...headers,
          'Content-Type': debugBodyMode.value === 'json' ? 'application/json' : 'application/x-www-form-urlencoded'
        },
        body: debugBodyMode.value === 'json' ? JSON.stringify(params) : qs
      })
    }
    debugStatus.value = resp.status
    debugCostMs.value = Math.round(performance.now() - start)
    debugBody.value = await resp.text()
  } catch (e) {
    debugCostMs.value = Math.round(performance.now() - start)
    debugBody.value = `请求失败: ${(e as Error).message}`
  } finally {
    saveHistory()
    debugLoading.value = false
  }
}

function parseJsonObject(json: string, label: string): Record<string, string> | null {
  const raw = json.trim()
  if (!raw) return {}
  try {
    const obj = JSON.parse(raw)
    if (obj === null || typeof obj !== 'object' || Array.isArray(obj)) {
      ElMessage.error(`${label}需为 JSON 对象`)
      return null
    }
    const flat: Record<string, string> = {}
    Object.entries(obj).forEach(([key, value]) => {
      if (value === null || value === undefined) return
      flat[key] = typeof value === 'string' ? value : JSON.stringify(value)
    })
    return flat
  } catch {
    ElMessage.error(`${label}不是合法的 JSON`)
    return null
  }
}

function openCreateForm() {
  editingId.value = null
  interfaceForm.value = { name: '', description: '', method: 'GET', url: '', requestParams: '', responseExample: '' }
  formVisible.value = true
}

function openEditForm(row: InterfaceInfo | null) {
  if (!row) return
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

async function handleDeleteInterface() {
  const rows = selected.value
  if (rows.length === 0) return
  const msg = rows.length > 1
    ? `确定删除选中的 ${rows.length} 个接口吗？`
    : `确定删除接口「${rows[0].name}」吗？`
  await ElMessageBox.confirm(msg, '删除接口', { type: 'warning' })
  const results = await Promise.allSettled(rows.map((i) => deleteInterface(i.id)))
  summarizeResults(results, rows.length, '删除')
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

function handleSelectionChange(rows: InterfaceInfo[]) {
  selected.value = rows
}

function clearSelection() {
  selected.value = []
  tableRef.value?.clearSelection()
}

function handleRowClick(row: InterfaceInfo) {
  tableRef.value?.toggleRowSelection(row)
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
.json-block {
  background: var(--el-fill-color-light, #f5f7fa);
  color: var(--el-text-color-regular, #303133);
  border-radius: 4px;
  padding: 12px;
  max-height: 260px;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
}
.debug-param-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; width: 100%; margin-bottom: 8px; }
.history-select { width: 150px; }
.block-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 12px 0 4px;
  font-weight: 600;
}
.debug-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: var(--el-fill-color-light, #f5f7fa);
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 6px;
}
.debug-url {
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
  color: var(--el-text-color-regular, #606266);
  word-break: break-all;
}
.debug-result {
  margin-top: 4px;
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 6px;
  overflow: hidden;
}
.debug-result-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: var(--el-fill-color-light, #f5f7fa);
  border-bottom: 1px solid var(--el-border-color-lighter, #ebeef5);
}
.debug-cost {
  margin-left: auto;
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
}
.debug-copy {
  margin-left: 8px;
}
.debug-body {
  margin: 0;
  padding: 12px;
  max-height: 260px;
  overflow: auto;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  background: var(--el-bg-color, #fff);
  color: var(--el-text-color-regular, #303133);
}
.json-block :deep(.json-key),
.debug-body :deep(.json-key) {
  color: var(--el-color-primary, #409eff);
}
.json-block :deep(.json-string),
.debug-body :deep(.json-string) {
  color: var(--el-color-success, #67c23a);
}
.json-block :deep(.json-number),
.debug-body :deep(.json-number) {
  color: var(--el-color-warning, #e6a23c);
}
.json-block :deep(.json-boolean),
.debug-body :deep(.json-boolean) {
  color: var(--el-color-danger, #f56c6c);
}
.json-block :deep(.json-null),
.debug-body :deep(.json-null) {
  color: var(--el-text-color-placeholder, #c0c4cc);
}
</style>
