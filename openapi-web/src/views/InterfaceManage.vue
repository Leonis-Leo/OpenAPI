<template>
  <div class="interface-layout">
    <aside v-show="groupPanelVisible" class="group-panel" :style="{ width: panelWidth + 'px' }">
      <div class="group-panel-head">
        <span>接口分组</span>
        <div class="group-panel-actions">
          <el-button v-if="isAdmin" text type="primary" size="small" @click="openGroupTagManage">管理</el-button>
          <button
            type="button"
            class="panel-toggle"
            aria-label="收起分组面板"
            title="收起分组面板"
            @click="toggleGroupPanel"
          >
            <el-icon><Fold /></el-icon>
          </button>
        </div>
      </div>
      <el-input
        v-model="groupKeyword"
        placeholder="搜索分组"
        clearable
        size="small"
        class="group-search"
        @input="onGroupSearch"
      />
      <ul class="group-tree-static">
        <li>
          <button
            type="button"
            :class="{ active: groupFilter === undefined }"
            @click="selectGroup(undefined)"
          >
            <span>全部接口</span>
            <span class="group-count">{{ totalAll }}</span>
          </button>
        </li>
        <li>
          <button
            type="button"
            :class="{ active: groupFilter === null }"
            @click="selectGroup(null)"
          >
            <span>未分组</span>
            <span class="group-count">{{ ungroupedCount }}</span>
          </button>
        </li>
      </ul>
      <el-tree
        ref="groupTreeRef"
        class="group-tree"
        :data="groups"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        highlight-current
        default-expand-all
        :expand-on-click-node="false"
        :filter-node-method="filterGroupNode"
        :current-node-key="groupFilter ?? undefined"
        @node-click="onGroupNodeClick"
      >
        <template #default="{ data }">
          <span class="tree-node">
            <span class="tree-node-name">{{ data.name }}</span>
            <span class="group-count">{{ data.interfaceCount }}</span>
          </span>
        </template>
      </el-tree>
    </aside>
    <div v-show="groupPanelVisible" class="panel-resizer" aria-hidden="true" @mousedown="startResize">
      <span class="resizer-grip" />
    </div>
    <button
      v-if="!groupPanelVisible"
      type="button"
      class="panel-expand"
      aria-label="展开分组面板"
      title="展开分组面板"
      @click="toggleGroupPanel"
    >
      <el-icon><Expand /></el-icon>
    </button>

    <section class="interface-content">
      <div class="page-header">
        <h2 class="page-title">接口管理</h2>
        <div class="page-actions">
          <el-button v-if="isAdmin" text type="primary" @click="openGroupTagManage">分组标签</el-button>
          <el-button v-if="isAdmin" plain @click="importVisible = true">导入 OpenAPI</el-button>
          <el-dropdown v-if="isAdmin" @command="handleExportCommand">
            <el-button plain>
              导出
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="openapi-json">OpenAPI JSON</el-dropdown-item>
                <el-dropdown-item command="openapi-yaml">OpenAPI YAML</el-dropdown-item>
                <el-dropdown-item command="csv">接口 CSV</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button v-if="isAdmin" type="primary" @click="openCreateForm">新增接口</el-button>
        </div>
      </div>

      <div class="filter-bar">
        <el-radio-group v-model="filterStatus" size="small" @change="onFilterChange">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="online">已上线</el-radio-button>
          <el-radio-button value="subscribed">已订阅</el-radio-button>
          <el-radio-button value="unsubscribed">未订阅</el-radio-button>
        </el-radio-group>
        <div class="filter-right">
          <el-input
            v-model="keywordInput"
            placeholder="搜索名称 / 路径"
            clearable
            style="width: 200px"
            @input="onKeywordInput"
          />
          <el-select v-model="tagFilter" clearable placeholder="全部标签" style="width: 120px" @change="onFilterChange">
            <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </div>
      </div>

      <div class="row-actions">
        <div class="row-actions-left">
          <el-button size="small" type="primary" plain :disabled="!selectedRow" @click="openDetail(selectedRow)">
            详情/调试
          </el-button>
          <el-button
            size="small"
            type="primary"
            plain
            :disabled="subscribeAction.disabled"
            @click="handleSubscribeToggle"
          >
            {{ subscribeAction.label }}
          </el-button>
          <template v-if="isAdmin">
            <el-button
              size="small"
              :type="statusAction.target === 0 ? 'warning' : 'success'"
              plain
              :disabled="statusAction.disabled"
              @click="toggleStatus(statusAction.target)"
            >
              {{ statusAction.label }}
            </el-button>
            <el-button size="small" type="primary" plain :disabled="!selectedRow" @click="openEditForm(selectedRow)">
              编辑
            </el-button>
            <el-button size="small" type="primary" plain :disabled="!selectedRow" @click="openVersionHistory(selectedRow)">
              版本历史
            </el-button>
          </template>
          <span v-if="selected.length" class="selected-tip">已选 {{ selected.length }} 项</span>
          <el-button v-if="isAdmin" size="small" type="danger" plain :disabled="selected.length === 0" @click="handleDeleteInterface">
            删除
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
          @current-change="handlePageChange"
          @size-change="handlePageChange"
        />
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
          <el-tag :type="methodTagType(row.method)">
            {{ row.method }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="url" label="路径" min-width="180" />
      <el-table-column label="分组" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.groupName" type="info" size="small" effect="plain">{{ row.groupName }}</el-tag>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="标签" min-width="140">
        <template #default="{ row }">
          <el-tag
            v-for="tag in row.tags ?? []"
            :key="tag.id"
            size="small"
            effect="light"
            class="tag-chip"
            :style="{ color: tag.color, borderColor: tag.color + '55', background: tag.color + '14' }"
          >
            {{ tag.name }}
          </el-tag>
          <span v-if="!(row.tags ?? []).length" class="text-muted">-</span>
        </template>
      </el-table-column>
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

    </section>

    <el-dialog v-model="detailVisible" class="interface-detail-dialog" :title="`接口详情 - ${debugInterface?.name ?? ''}`" width="1120px" top="6vh">
      <el-tabs v-model="detailTab" class="interface-detail-tabs">
        <el-tab-pane label="接口信息" name="info">
          <el-descriptions class="interface-summary" :column="2" border>
            <el-descriptions-item label="名称">{{ debugInterface?.name }}</el-descriptions-item>
            <el-descriptions-item label="方式">{{ debugInterface?.method }}</el-descriptions-item>
            <el-descriptions-item label="路径" :span="2">{{ debugInterface?.url }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ debugInterface?.description }}</el-descriptions-item>
          </el-descriptions>
          <div class="block-toolbar">
            <span>请求参数说明</span>
            <el-button size="small" plain @click="copyText(debugInterface?.requestParams)">复制</el-button>
          </div>
          <pre class="json-block code-panel" v-html="highlightJson(prettyJson(debugInterface?.requestParams))"></pre>
          <div class="block-toolbar">
            <span>响应示例</span>
            <el-button size="small" plain @click="copyText(debugInterface?.responseExample)">复制</el-button>
          </div>
          <pre class="json-block code-panel" v-html="highlightJson(prettyJson(debugInterface?.responseExample))"></pre>
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
          <pre class="json-block code-panel">{{ javaSample || '-' }}</pre>
          <div class="block-toolbar">
            <span>curl</span>
            <el-button size="small" plain @click="copyText(curlSample)">复制</el-button>
          </div>
          <pre class="json-block code-panel">{{ curlSample || '-' }}</pre>
        </el-tab-pane>
        <el-tab-pane label="在线调试" name="debug">
          <div class="debug-pane">
          <div class="debug-header">
            <el-tag :type="methodTagType(debugInterface?.method)" size="small">
              {{ debugInterface?.method }}
            </el-tag>
            <span class="debug-url">{{ debugInterface?.url }}</span>
            <el-button class="debug-header-send" type="primary" :loading="debugLoading" @click="handleDebug">发送请求</el-button>
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
            <el-option label="PUT" value="PUT" />
            <el-option label="PATCH" value="PATCH" />
            <el-option label="DELETE" value="DELETE" />
          </el-select>
        </el-form-item>
        <el-form-item label="路径"><el-input v-model="interfaceForm.url" placeholder="/api/xxx" /></el-form-item>
        <el-form-item label="分组">
          <el-select v-model="interfaceForm.groupId" clearable placeholder="选择分组" style="width: 100%">
            <el-option v-for="g in groups" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="interfaceForm.tags" multiple clearable placeholder="选择标签" style="width: 100%">
            <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
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

    <el-dialog v-model="groupTagVisible" title="分组与标签管理" width="720px">
      <el-tabs v-model="groupTagTab">
        <el-tab-pane label="分组" name="groups">
          <div class="meta-bar">
            <el-input v-model="newGroupName" placeholder="新分组名称" style="width: 200px" @keyup.enter="handleCreateGroup" />
            <el-select v-model="newGroupParentId" clearable placeholder="顶级分组" style="width: 150px">
              <el-option v-for="g in flatGroups" :key="g.id" :label="g.name" :value="g.id" />
            </el-select>
            <el-button type="primary" plain @click="handleCreateGroup">新增分组</el-button>
          </div>
          <el-table :data="flatGroups" border stripe size="small" v-loading="metaLoading">
            <el-table-column prop="name" label="分组名称" min-width="130" />
            <el-table-column label="父分组" width="120">
              <template #default="{ row }">{{ row.parentName ?? '—' }}</template>
            </el-table-column>
            <el-table-column prop="interfaceCount" label="接口数" width="80" />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button size="small" text type="primary" @click="startRenameGroup(row)">重命名</el-button>
                <el-button size="small" text type="danger" @click="handleDeleteGroup(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="标签" name="tags">
          <div class="meta-bar">
            <el-input v-model="newTagName" placeholder="新标签名称" style="width: 220px" @keyup.enter="handleCreateTag" />
            <el-button type="primary" plain @click="handleCreateTag">新增标签</el-button>
          </div>
          <el-table :data="tags" border stripe size="small" v-loading="metaLoading">
            <el-table-column label="标签" min-width="140">
              <template #default="{ row }">
                <el-tag size="small" :style="{ color: row.color, borderColor: row.color + '55', background: row.color + '14' }">
                  {{ row.name }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="interfaceCount" label="接口数" width="80" />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button size="small" text type="danger" @click="handleDeleteTag(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog v-model="importVisible" title="导入 OpenAPI" width="640px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="支持 OpenAPI 3.0 JSON / YAML"
        description="按 paths 中的接口逐个导入（默认下线），自动生成请求参数与响应示例；同名路径+方法已存在的接口会跳过。"
        style="margin-bottom: 12px"
      />
      <el-input
        v-model="importSpec"
        type="textarea"
        :rows="10"
        placeholder='粘贴 OpenAPI 内容，例如：
openapi: 3.0.1
paths:
  /api/demo/hello:
    get:
      summary: 你好
      description: 示例接口
      parameters:
        - name: name
          in: query
          required: false
          description: 名称
          schema:
            type: string
      responses:
        "200":
          description: 成功
          content:
            application/json:
              example: {"code":0,"data":"Hello"}' />
      <div class="import-actions">
        <el-button @click="pickImportFile">选择文件</el-button>
        <span v-if="importFileName" class="import-file-name">{{ importFileName }}</span>
        <input ref="importFileRef" type="file" accept=".json,.yaml,.yml" style="display: none" @change="onImportFileChange" />
        <el-button type="primary" :loading="importing" @click="handleImportOpenApi">开始导入</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="versionVisible" :title="`版本历史 - ${versionInterface?.name ?? ''}`" width="880px" top="6vh">
      <el-table
        :data="versions"
        border
        stripe
        v-loading="versionLoading"
        highlight-current-row
        @current-change="(row?: InterfaceVersionInfo) => (diffTarget = row ?? null)"
      >
        <el-table-column label="版本" width="80">
          <template #default="{ row }">
            <el-tag :type="row.id === currentVersionId ? 'success' : 'info'" size="small">
              v{{ row.versionNo }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="changeNote" label="变更说明" min-width="130" />
        <el-table-column label="发布状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '上线' : '下线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="当前" width="70">
          <template #default="{ row }">
            <span v-if="row.id === currentVersionId" class="current-badge">当前</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="diffTarget = row">差异</el-button>
            <el-button
              size="small"
              text
              type="danger"
              :disabled="row.id === currentVersionId"
              @click="handleRollback(row)"
            >
              回滚
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="diffTarget && diffRows.length" class="version-diff">
        <h4>v{{ diffTarget.versionNo }} 与当前接口差异</h4>
        <el-table :data="diffRows" border size="small">
          <el-table-column prop="field" label="字段" width="110" />
          <el-table-column prop="current" label="当前值" show-overflow-tooltip />
          <el-table-column prop="version" label="vN 值" show-overflow-tooltip />
        </el-table>
      </div>
      <el-empty v-else-if="diffTarget" description="与当前版本内容一致" :image-size="50" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onActivated, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableInstance } from 'element-plus'
import { ArrowDown, Expand, Fold } from '@element-plus/icons-vue'
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
  listInterfaceVersions,
  rollbackInterface,
  listInterfaceGroups,
  createInterfaceGroup,
  updateInterfaceGroup,
  deleteInterfaceGroup,
  listInterfaceTags,
  createInterfaceTag,
  deleteInterfaceTag,
  importOpenApi,
  exportOpenApi,
  subscribe,
  mySubscribes,
  unsubscribe,
  type InterfaceInfo,
  type InterfaceVersionInfo,
  type InterfaceGroupInfo,
  type InterfaceTagInfo
} from '@/api'
import { hmacSha256Hex, buildSignContent, type SignParams } from '@/utils/sign'

const userStore = useUserStore()
const isAdmin = userStore.user?.userRole === 'admin'
const interfaces = ref<InterfaceInfo[]>([])
const loading = ref(false)
const keyword = ref('')
const keywordInput = ref('')
const filterStatus = ref<'all' | 'online' | 'subscribed' | 'unsubscribed'>('all')
const groups = ref<InterfaceGroupInfo[]>([])
const tags = ref<InterfaceTagInfo[]>([])
const groupFilter = ref<number | null | undefined>(undefined)
const totalAll = ref(0)
const ungroupedCount = ref(0)
const groupKeyword = ref('')
const groupTreeRef = ref()
const newGroupParentId = ref<number>()
const panelWidth = ref(Number(localStorage.getItem('openapi-group-panel-width')) || 210)
const groupPanelVisible = ref(localStorage.getItem('openapi-group-panel-visible') !== '0')
const tagFilter = ref<number>()
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
const groupTagVisible = ref(false)
const groupTagTab = ref('groups')
const newGroupName = ref('')
const newTagName = ref('')
const metaLoading = ref(false)
const importVisible = ref(false)
const importSpec = ref('')
const importFileName = ref('')
const importFileRef = ref<HTMLInputElement>()
const importing = ref(false)
let metaLoaded = false
const versionVisible = ref(false)
const versionInterface = ref<InterfaceInfo | null>(null)
const versions = ref<InterfaceVersionInfo[]>([])
const versionLoading = ref(false)
const diffTarget = ref<InterfaceVersionInfo | null>(null)
const currentVersionId = ref<number | null>(null)

const diffRows = computed(() => {
  const target = diffTarget.value
  const current = versionInterface.value
  if (!target || !current) return []
  type CompareField = 'name' | 'description' | 'method' | 'url' | 'requestParams' | 'responseExample'
  const fields: { key: CompareField; label: string }[] = [
    { key: 'name', label: '名称' },
    { key: 'description', label: '描述' },
    { key: 'method', label: '方式' },
    { key: 'url', label: '路径' },
    { key: 'requestParams', label: '请求参数' },
    { key: 'responseExample', label: '响应示例' }
  ]
  return fields
    .filter(({ key }) => String(target[key] ?? '') !== String(current[key] ?? ''))
    .map(({ key, label }) => ({
      field: label,
      current: String(current[key] ?? ''),
      version: String(target[key] ?? '')
    }))
})

async function openVersionHistory(row: InterfaceInfo | null) {
  if (!row) return
  versionInterface.value = row
  diffTarget.value = null
  versionVisible.value = true
  versionLoading.value = true
  try {
    versions.value = await listInterfaceVersions(row.id)
    currentVersionId.value = versions.value[0]?.id ?? null
  } finally {
    versionLoading.value = false
  }
}

async function handleRollback(version: InterfaceVersionInfo) {
  const info = versionInterface.value
  if (!info) return
  await ElMessageBox.confirm(
    `确定将接口「${info.name}」回滚到 v${version.versionNo} 吗？当前内容将被替换。`,
    '一键回滚',
    { type: 'warning' }
  )
  try {
    await rollbackInterface(info.id, version.id)
    ElMessage.success(`已回滚到 v${version.versionNo}`)
    await load()
    await openVersionHistory(info)
  } catch {
    // 错误提示已由拦截器处理
  }
}

const selectedRow = computed(() => (selected.value.length === 1 ? selected.value[0] : null))
const flatGroups = computed<InterfaceGroupInfo[]>(() => {
  const result: InterfaceGroupInfo[] = []
  const walk = (list: InterfaceGroupInfo[]) => {
    list.forEach((g) => {
      result.push(g)
      if (g.children?.length) walk(g.children)
    })
  }
  walk(groups.value)
  return result
})
function selectGroup(id: number | null | undefined) {
  groupFilter.value = id
  currentPage.value = 1
  clearSelection()
  load()
}

function onGroupNodeClick(data: InterfaceGroupInfo) {
  selectGroup(data.id)
}

function filterGroupNode(value: string, data: InterfaceGroupInfo) {
  if (!value) return true
  return data.name.toLowerCase().includes(value.toLowerCase())
}

function collectMatchingGroups(list: InterfaceGroupInfo[], kw: string): InterfaceGroupInfo[] {
  const matches: InterfaceGroupInfo[] = []
  const walk = (items: InterfaceGroupInfo[]) => {
    items.forEach((g) => {
      if (g.name.toLowerCase().includes(kw)) matches.push(g)
      if (g.children?.length) walk(g.children)
    })
  }
  walk(list)
  return matches
}

function onGroupSearch() {
  const tree = groupTreeRef.value
  if (!tree) return
  const kw = groupKeyword.value.trim().toLowerCase()
  tree.filter(kw || '')
  if (!kw) {
    tree.setCurrentKey(null)
    return
  }
  nextTick(() => {
    const matches = collectMatchingGroups(groups.value, kw)
    const leaf = matches.find((g) => !g.children?.length) ?? matches[0]
    if (!leaf) return
    const node = tree.getNode(leaf.id)
    let cur = node?.parent
    while (cur && cur.data && cur.data.id != null) {
      cur.expanded = true
      cur = cur.parent
    }
    tree.setCurrentKey(leaf.id)
    nextTick(() => {
      const container = tree.$el as HTMLElement
      const current = container.querySelector('.el-tree-node.is-current') as HTMLElement | null
      if (container && current) {
        const top = current.getBoundingClientRect().top - container.getBoundingClientRect().top
        container.scrollTop = Math.max(0, top - 12)
      }
    })
  })
}

function startResize(event: MouseEvent) {
  event.preventDefault()
  const startX = event.clientX
  const startWidth = panelWidth.value
  const onMove = (ev: MouseEvent) => {
    const width = Math.min(360, Math.max(160, startWidth + ev.clientX - startX))
    panelWidth.value = width
  }
  const onUp = () => {
    localStorage.setItem('openapi-group-panel-width', String(panelWidth.value))
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

function toggleGroupPanel() {
  groupPanelVisible.value = !groupPanelVisible.value
  localStorage.setItem('openapi-group-panel-visible', groupPanelVisible.value ? '1' : '0')
}
const subscribeAction = computed(() => {
  const rows = selected.value
  if (rows.length === 0) return { label: '订阅', disabled: true, type: 'subscribe' as const }
  const subscribable = rows.filter(
    (r) => subscribeMap.value[r.id] === undefined || subscribeMap.value[r.id] === 2
  )
  const subscribed = rows.filter((r) => subscribeMap.value[r.id] === 1)
  if (subscribable.length === rows.length) {
    return { label: '订阅', disabled: false, type: 'subscribe' as const }
  }
  if (subscribed.length === rows.length) {
    return { label: '取消订阅', disabled: false, type: 'unsubscribe' as const }
  }
  return { label: '订阅/取消', disabled: true, type: 'subscribe' as const }
})
const statusAction = computed(() => {
  const rows = selected.value
  if (rows.length === 0) return { label: '上线', disabled: true, target: 1 }
  const online = rows.filter((r) => r.status === 1)
  if (online.length === rows.length) return { label: '下线', disabled: false, target: 0 }
  if (online.length === 0) return { label: '上线', disabled: false, target: 1 }
  return { label: '上线/下线', disabled: true, target: 1 }
})
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

function methodTagType(method?: string) {
  if (method === 'GET') return 'success'
  if (method === 'DELETE') return 'danger'
  if (method === 'PATCH') return 'warning'
  return 'primary'
}

function usesQueryParams(method?: string) {
  return method === 'GET' || method === 'DELETE'
}

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
  const isQuery = usesQueryParams(info.method)
  const hutoolMethod = info.method.toLowerCase()
  const requestLine = isQuery
    ? `        String url = "${info.url}" + (params.isEmpty() ? "" : "?" + HttpUtil.toParams(params));
        HttpRequest request = HttpRequest.${hutoolMethod}(url)`
    : `        String url = "${info.url}";
        HttpRequest request = HttpRequest.${hutoolMethod}(url)
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
  if (usesQueryParams(info.method)) {
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
    if (groupFilter.value !== undefined) {
      if (groupFilter.value === null ? i.groupId != null : i.groupId !== groupFilter.value) return false
    }
    if (tagFilter.value && !(i.tags ?? []).some((t) => t.id === tagFilter.value)) return false
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
    if (!metaLoaded) {
      metaLoaded = true
      try {
        const [groupList, tagList] = await Promise.all([listInterfaceGroups(), listInterfaceTags()])
        groups.value = groupList.tree
        totalAll.value = groupList.total
        ungroupedCount.value = groupList.ungrouped
        tags.value = tagList
      } catch {
        metaLoaded = false
      }
    }
    if (filterStatus.value === 'all' || filterStatus.value === 'online') {
      const result = await pageInterfaces({
        current: currentPage.value,
        size: pageSize.value,
        keyword: keyword.value || undefined,
        status: filterStatus.value === 'online' ? 1 : undefined,
        groupId: groupFilter.value === null ? undefined : groupFilter.value,
        ungrouped: groupFilter.value === null ? true : undefined,
        tagId: tagFilter.value
      })
      interfaces.value = result.records
      total.value = Number(result.total)
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

async function handleSubscribeToggle() {
  if (subscribeAction.value.type === 'unsubscribe') {
    await handleUnsubscribe()
  } else {
    openSubscribe()
  }
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
    if (usesQueryParams(info.method)) {
      resp = await fetch(info.url + (qs ? '?' + qs : ''), { method: info.method, headers })
    } else {
      resp = await fetch(info.url, {
        method: info.method,
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
  interfaceForm.value = {
    name: '',
    description: '',
    method: 'GET',
    url: '',
    requestParams: '',
    responseExample: '',
    groupId: undefined,
    tags: []
  }
  formVisible.value = true
}

function exportInterfaces() {
  const header = ['名称', '描述', '方式', '路径', '状态', '订阅状态']
  const rows = interfaces.value.map((info) => [
    info.name,
    info.description ?? '',
    info.method,
    info.url,
    info.status === 1 ? '上线' : '下线',
    subscribeMap.value[info.id] === 1
      ? '已订阅'
      : subscribeMap.value[info.id] === 0
        ? '待审批'
        : subscribeMap.value[info.id] === 2
          ? '已拒绝'
          : '未订阅'
  ])
  const escape = (value: string) => `"${String(value).replace(/"/g, '""')}"`
  const csv = '\uFEFF' + [header, ...rows].map((row) => row.map(escape).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `interfaces-${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('CSV 已导出当前页数据')
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
    responseExample: row.responseExample ?? '',
    groupId: row.groupId,
    tags: (row.tags ?? []).map((t) => t.id)
  }
  formVisible.value = true
}

async function openGroupTagManage() {
  groupTagVisible.value = true
  await refreshMeta()
}

async function refreshMeta() {
  metaLoading.value = true
  try {
    const [groupList, tagList] = await Promise.all([listInterfaceGroups(), listInterfaceTags()])
    groups.value = groupList.tree
    totalAll.value = groupList.total
    ungroupedCount.value = groupList.ungrouped
    tags.value = tagList
  } finally {
    metaLoading.value = false
  }
}

async function handleCreateGroup() {
  const name = newGroupName.value.trim()
  if (!name) return
  try {
    await createInterfaceGroup(name, newGroupParentId.value)
    newGroupName.value = ''
    newGroupParentId.value = undefined
    ElMessage.success('分组已创建')
    await refreshMeta()
  } catch {
    // 拦截器已提示
  }
}

async function startRenameGroup(row: InterfaceGroupInfo) {
  try {
    const { value } = await ElMessageBox.prompt('输入新的分组名称', '重命名分组', {
      inputValue: row.name,
      inputValidator: (v: string) => (v?.trim() ? true : '分组名称不能为空')
    })
    await updateInterfaceGroup(row.id, value.trim())
    ElMessage.success('已重命名')
    await refreshMeta()
  } catch {
    // 取消或失败
  }
}

async function handleDeleteGroup(row: InterfaceGroupInfo) {
  await ElMessageBox.confirm(`确定删除分组「${row.name}」吗？`, '删除分组', { type: 'warning' })
  try {
    await deleteInterfaceGroup(row.id)
    ElMessage.success('分组已删除')
    await refreshMeta()
    await load()
  } catch {
    // 拦截器已提示（分组下有接口会拒绝）
  }
}

async function handleCreateTag() {
  const name = newTagName.value.trim()
  if (!name) return
  try {
    await createInterfaceTag(name)
    newTagName.value = ''
    ElMessage.success('标签已创建')
    await refreshMeta()
  } catch {
    // 拦截器已提示
  }
}

async function handleDeleteTag(row: InterfaceTagInfo) {
  await ElMessageBox.confirm(`确定删除标签「${row.name}」吗？`, '删除标签', { type: 'warning' })
  try {
    await deleteInterfaceTag(row.id)
    ElMessage.success('标签已删除')
    await refreshMeta()
    await load()
  } catch {
    // 拦截器已提示
  }
}

function pickImportFile() {
  importFileRef.value?.click()
}

function onImportFileChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  importFileName.value = file.name
  const reader = new FileReader()
  reader.onload = () => {
    importSpec.value = String(reader.result ?? '')
  }
  reader.readAsText(file)
  ;(event.target as HTMLInputElement).value = ''
}

async function handleImportOpenApi() {
  if (!importSpec.value.trim()) {
    ElMessage.warning('请粘贴 OpenAPI 内容或选择文件')
    return
  }
  importing.value = true
  try {
    const result = await importOpenApi(importSpec.value)
    ElMessage.success(`导入完成：新增 ${result.created} 个，跳过 ${result.skipped} 个`)
    importVisible.value = false
    importSpec.value = ''
    importFileName.value = ''
    metaLoaded = false
    await load()
  } finally {
    importing.value = false
  }
}

async function handleExportOpenApi(format: string) {
  try {
    const spec = await exportOpenApi(format as 'json' | 'yaml')
    const type = format === 'yaml' ? 'text/yaml;charset=utf-8' : 'application/json;charset=utf-8'
    const url = URL.createObjectURL(new Blob([spec], { type }))
    const link = document.createElement('a')
    link.href = url
    link.download = `openapi-${new Date().toISOString().slice(0, 10)}.${format}`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success(`OpenAPI ${format.toUpperCase()} 已导出`)
  } catch {
    // 拦截器已提示
  }
}

async function handleExportCommand(command: string) {
  if (command === 'csv') {
    exportInterfaces()
    return
  }
  await handleExportOpenApi(command === 'openapi-yaml' ? 'yaml' : 'json')
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
.code-panel {
  min-height: 92px;
  margin: 0;
  border: 1px solid #e6ebf2;
  border-radius: 10px;
  background: #f8fafc;
  font-family: 'JetBrains Mono', Consolas, Monaco, monospace;
  line-height: 1.7;
}
.interface-summary {
  margin-bottom: 22px;
}
.interface-detail-tabs {
  min-height: 560px;
}
.debug-pane {
  display: flex;
  flex-direction: column;
  gap: 14px;
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
  margin-bottom: 0;
  padding: 8px 12px;
  background: var(--el-fill-color-light, #f5f7fa);
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 10px;
}
.debug-url {
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
  color: var(--el-text-color-regular, #606266);
  word-break: break-all;
}
.debug-header-send {
  margin-left: auto;
  min-width: 96px;
}
.debug-result {
  margin-top: 0;
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 10px;
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
.version-diff {
  margin-top: 16px;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 10px;
  background: #fbfcfe;
}
.version-diff h4 {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text, #172033);
}
.current-badge {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 6px;
  background: #ecfdf5;
  color: #047857;
  font-size: 12px;
  font-weight: 600;
}
.tag-chip {
  margin: 2px 6px 2px 0;
  border-radius: 6px;
  font-weight: 600;
}
.text-muted {
  color: var(--app-muted, #94a3b8);
  font-size: 12px;
}
.meta-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.import-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
}
.import-file-name {
  color: var(--app-muted, #64748b);
  font-size: 12px;
}
.interface-layout {
  display: flex;
  align-items: stretch;
  gap: 0;
}
.group-panel {
  width: 210px;
  flex: none;
  display: flex;
  flex-direction: column;
  min-height: 480px;
  max-height: calc(100vh - 170px);
  overflow: hidden;
  padding: 12px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  background: var(--app-surface);
}
.group-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 4px 10px;
  font-size: 14px;
  font-weight: 700;
  color: var(--app-text);
}
.group-panel-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}
.panel-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--app-muted);
  cursor: pointer;
  font-size: 15px;
}
.panel-toggle:hover {
  background: var(--el-fill-color-light, #f1f5f9);
  color: var(--app-text);
}
.panel-toggle:focus-visible {
  outline: 2px solid var(--app-primary);
  outline-offset: 1px;
}
.group-search {
  margin-bottom: 8px;
}
.group-tree-static {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin: 0 0 8px;
  padding: 0 0 8px;
  border-bottom: 1px solid var(--app-border);
  list-style: none;
}
.group-tree-static button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  padding: 7px 10px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--app-text);
  cursor: pointer;
  font-size: 13px;
  text-align: left;
}
.group-tree-static button:hover {
  background: var(--el-fill-color-light, #f1f5f9);
}
.group-tree-static button.active {
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 600;
}
.group-tree-static button:focus-visible {
  outline: 2px solid var(--app-primary);
  outline-offset: 1px;
}
.group-tree {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
.group-tree :deep(.el-tree-node__content) {
  height: 30px;
  border-radius: 8px;
}
.group-tree :deep(.el-tree-node__content:hover) {
  background: var(--el-fill-color-light, #f1f5f9);
}
.group-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 600;
}
.group-tree :deep(.el-tree-node.is-current > .el-tree-node__content .group-count) {
  color: #1d4ed8;
}
.group-count {
  flex: none;
  color: var(--app-muted);
  font-size: 12px;
}
.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex: 1;
  min-width: 0;
  padding-right: 6px;
}
.tree-node-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.panel-resizer {
  position: relative;
  flex: none;
  align-self: stretch;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  margin: 0 4px;
  cursor: col-resize;
}
.panel-resizer::before {
  content: '';
  width: 2px;
  height: 36px;
  border-radius: 2px;
  background: var(--app-border);
  transition: background-color .16s ease, height .16s ease;
}
.panel-resizer:hover::before {
  height: 44px;
  background: var(--app-primary);
}
.resizer-grip {
  position: absolute;
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 4px 2px;
  border-radius: 6px;
  background: var(--app-surface);
}
.resizer-grip::before,
.resizer-grip::after {
  content: '';
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--app-muted);
}
.panel-expand {
  flex: none;
  align-self: stretch;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  margin-right: 12px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  background: var(--app-surface);
  color: var(--app-muted);
  cursor: pointer;
  font-size: 16px;
}
.panel-expand:hover {
  color: var(--app-primary);
  background: #eff6ff;
}
.panel-expand:focus-visible {
  outline: 2px solid var(--app-primary);
  outline-offset: 1px;
}
.interface-content {
  flex: 1;
  min-width: 0;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -.02em;
  color: var(--app-text);
}
.page-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.filter-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.row-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 8px 10px;
  margin-bottom: 12px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  background: var(--app-surface);
}
.row-actions-left,
.row-actions-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.selected-tip {
  color: var(--app-primary);
  font-size: 12px;
  font-weight: 600;
}
@media (max-width: 900px) {
  .interface-layout {
    flex-direction: column;
  }
  .group-panel {
    width: 100%;
    min-height: 0;
    max-height: none;
  }
  .panel-resizer {
    display: none;
  }
}
.debug-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  column-gap: 20px;
  padding: 18px 20px 4px;
  border: 1px solid #e6ebf2;
  border-radius: 10px;
  background: #fbfcfe;
}
:global(.debug-form > .el-form-item:nth-child(1)),
:global(.debug-form > .el-form-item:nth-child(4)) {
  grid-column: 1 / -1;
}
:global(.debug-form > .el-form-item:nth-child(2) .el-textarea__inner),
:global(.debug-form > .el-form-item:nth-child(3) .el-textarea__inner) {
  min-height: 150px;
  padding: 12px 14px;
  border-radius: 8px;
  font-family: 'JetBrains Mono', Consolas, Monaco, monospace;
  line-height: 1.6;
}
:global(.debug-form > .el-form-item:nth-child(4) .el-button--primary) {
  display: none;
}
:global(.interface-detail-dialog.el-dialog) {
  max-width: calc(100vw - 48px);
  border-radius: 16px;
  overflow: hidden;
}
:global(.interface-detail-dialog .el-dialog__header) {
  margin-right: 0;
  padding: 22px 28px 16px;
  border-bottom: 1px solid #edf1f5;
}
:global(.interface-detail-dialog .el-dialog__title) {
  color: #172b4d;
  font-size: 20px;
  font-weight: 700;
}
:global(.interface-detail-dialog .el-dialog__body) {
  padding: 18px 28px 28px;
}
:global(.interface-detail-dialog .el-tabs__item) {
  height: 44px;
  font-weight: 600;
}
@media (max-width: 860px) {
  .debug-pane {
    display: flex;
  }
  .debug-form {
    grid-template-columns: 1fr;
  }
  :global(.debug-form > .el-form-item:nth-child(1)),
  :global(.debug-form > .el-form-item:nth-child(2)),
  :global(.debug-form > .el-form-item:nth-child(3)),
  :global(.debug-form > .el-form-item:nth-child(4)) {
    grid-column: 1;
  }
  :global(.interface-detail-dialog.el-dialog) {
    max-width: calc(100vw - 24px);
  }
  :global(.interface-detail-dialog .el-dialog__body) {
    padding: 12px 16px 20px;
  }
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
