# 首页概览重设计（数据大盘）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `/dashboard` 首页从"统计卡 + 指引"改造成角色感知的数据大盘（统计卡、调用趋势三条序列、TOP 10 排行、最近调用动态），纯前端、零新依赖。

**Architecture:** 容器（`DashboardView.vue`）负责角色分支与数据编排，展示组件（`src/components/dashboard/` 下 5 个小组件）只收 props、发事件。纯数据映射逻辑抽到 `dashboard-model.ts`，用 Node 内置 test runner 做零依赖 TDD。ECharts 按需引入，颜色读取 Element Plus CSS 变量以自动适配暗色模式。

**Tech Stack:** Vue 3.5（`<script setup>` + TS strict）、Element Plus 2.9、ECharts 6（`echarts/core` 按需）、Vite 5、Node 24（`node --test` + 类型剥离，无新依赖）。

## Global Constraints

- 纯前端改动，零后端改动；不改 `AdminLayout.vue` 与路由。
- 不新增任何依赖（runtime 或 devDependency）。
- 复用 `src/api/index.ts` 现有函数，不新增 API。
- 普通用户不调用任何 `/v1/stats/**` 接口（后端仅 admin）。
- 排行固定 TOP 10；趋势图三条序列：调用量 / 成功量 / 失败量（`fail = total - ok`）。
- 颜色只用语义 token（`--el-color-*`、`--el-text-color-*`、`--el-border-color`），不写裸色值。
- 单文件不超过 200 行；组件在 `src/components/dashboard/`，容器在 `src/views/DashboardView.vue`。
- 提交粒度：每个任务一个 commit，只暂存本任务涉及的文件。
- 所有命令在 `openapi-web/` 目录执行。

---

### Task 1: 首页数据模型 + 零依赖单元测试（TDD）

**Files:**
- Create: `src/components/dashboard/dashboard-model.ts`
- Create: `tests/dashboard-model.test.ts`（放 `src/` 外，避免 vue-tsc 因缺少 `@types/node` 报错；Node 24 类型剥离直接运行）

**Interfaces:**
- Consumes: 无（纯函数，类型自包含）。
- Produces:
  - `interface DashboardStatItem { key: string; label: string; value: number; display: string; route: string; tone: 'primary' | 'success' | 'warning' }`
  - `interface OverviewSeries { days: string[]; total: number[]; ok: number[]; fail: number[] }`
  - `interface DashboardInput { appCount: number; subscribeCount: number; interfaceCount: number; total: number; successRate: number; pendingCount: number; isAdmin: boolean }`
  - `function buildOverviewSeries(daily: { day: string; total: number; ok: number }[]): OverviewSeries`
  - `function roundRate(ok: number, total: number): number`
  - `function buildStatCards(input: DashboardInput): DashboardStatItem[]`

- [ ] **Step 1: 写失败测试**

`tests/dashboard-model.test.ts`:

```ts
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { buildOverviewSeries, roundRate, buildStatCards } from '../src/components/dashboard/dashboard-model.ts'

test('buildOverviewSeries computes fail = total - ok', () => {
  const s = buildOverviewSeries([
    { day: '2026-08-01', total: 10, ok: 7 },
    { day: '2026-08-02', total: 0, ok: 0 }
  ])
  assert.deepEqual(s.days, ['2026-08-01', '2026-08-02'])
  assert.deepEqual(s.total, [10, 0])
  assert.deepEqual(s.ok, [7, 0])
  assert.deepEqual(s.fail, [3, 0])
})

test('buildOverviewSeries never returns negative fail', () => {
  const s = buildOverviewSeries([{ day: '2026-08-01', total: 5, ok: 9 }])
  assert.deepEqual(s.fail, [0])
})

test('roundRate guards zero total and rounds', () => {
  assert.equal(roundRate(0, 0), 0)
  assert.equal(roundRate(1, 3), 33)
})

test('buildStatCards: admin gets 6 cards in order', () => {
  const cards = buildStatCards({
    appCount: 4, subscribeCount: 4, interfaceCount: 3,
    total: 23, successRate: 100, pendingCount: 0, isAdmin: true
  })
  assert.equal(cards.length, 6)
  assert.deepEqual(cards.map((c) => c.key), ['apps', 'subscribes', 'interfaces', 'total', 'rate', 'pending'])
  assert.equal(cards[3].display, '23')
  assert.equal(cards[4].display, '100%')
  assert.equal(cards[4].tone, 'success')
  assert.equal(cards[5].tone, 'warning')
})

test('buildStatCards: regular user gets 3 cards and no stats', () => {
  const cards = buildStatCards({
    appCount: 1, subscribeCount: 2, interfaceCount: 3,
    total: 0, successRate: 0, pendingCount: 0, isAdmin: false
  })
  assert.equal(cards.length, 3)
  assert.deepEqual(cards.map((c) => c.route), ['/apps', '/subscribes', '/interfaces'])
})
```

- [ ] **Step 2: 运行测试，验证失败**

Run: `node --test tests/dashboard-model.test.ts`
Expected: FAIL——`ERR_MODULE_NOT_FOUND`，找不到 `dashboard-model.ts`（模块尚不存在）。

- [ ] **Step 3: 写最小实现**

`src/components/dashboard/dashboard-model.ts`:

```ts
export interface DashboardStatItem {
  key: string
  label: string
  value: number
  display: string
  route: string
  tone: 'primary' | 'success' | 'warning'
}

export interface OverviewSeries {
  days: string[]
  total: number[]
  ok: number[]
  fail: number[]
}

export interface DashboardInput {
  appCount: number
  subscribeCount: number
  interfaceCount: number
  total: number
  successRate: number
  pendingCount: number
  isAdmin: boolean
}

export function buildOverviewSeries(
  daily: { day: string; total: number; ok: number }[]
): OverviewSeries {
  return {
    days: daily.map((d) => d.day),
    total: daily.map((d) => d.total),
    ok: daily.map((d) => d.ok),
    fail: daily.map((d) => Math.max(0, d.total - d.ok))
  }
}

export function roundRate(ok: number, total: number): number {
  if (!total) return 0
  return Math.round((ok / total) * 100)
}

export function buildStatCards(input: DashboardInput): DashboardStatItem[] {
  const cards: DashboardStatItem[] = [
    { key: 'apps', label: '我的应用', value: input.appCount, display: String(input.appCount), route: '/apps', tone: 'primary' },
    { key: 'subscribes', label: '已订阅接口', value: input.subscribeCount, display: String(input.subscribeCount), route: '/subscribes', tone: 'primary' },
    { key: 'interfaces', label: '可调用接口', value: input.interfaceCount, display: String(input.interfaceCount), route: '/interfaces', tone: 'primary' }
  ]
  if (!input.isAdmin) return cards
  cards.push(
    { key: 'total', label: '累计调用', value: input.total, display: String(input.total), route: '/stats', tone: 'primary' },
    { key: 'rate', label: '成功率', value: input.successRate, display: `${input.successRate}%`, route: '/stats', tone: 'success' },
    { key: 'pending', label: '待审批订阅', value: input.pendingCount, display: String(input.pendingCount), route: '/subscribes', tone: 'warning' }
  )
  return cards
}
```

- [ ] **Step 4: 运行测试，验证通过**

Run: `node --test tests/dashboard-model.test.ts`
Expected: PASS，5 个测试全绿。

- [ ] **Step 5: 提交**

```bash
git add openapi-web/src/components/dashboard/dashboard-model.ts openapi-web/tests/dashboard-model.test.ts
git commit -m "feat(web): 首页数据模型与零依赖单元测试"
```

---

### Task 2: StatCard 统计卡组件

**Files:**
- Create: `src/components/dashboard/StatCard.vue`

**Interfaces:**
- Consumes: `DashboardStatItem`（Task 1）。
- Produces: `<StatCard :item="DashboardStatItem" :loading="boolean" />`；点击后组件内部 `router.push(item.route)`。

- [ ] **Step 1: 写组件**

`src/components/dashboard/StatCard.vue`:

```vue
<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Box, CircleCheck, Clock, Connection, DocumentChecked, Odometer } from '@element-plus/icons-vue'
import type { DashboardStatItem } from '@/components/dashboard/dashboard-model'

const props = defineProps<{ item: DashboardStatItem; loading?: boolean }>()
const router = useRouter()

const iconMap = {
  apps: Box,
  subscribes: DocumentChecked,
  interfaces: Connection,
  total: Odometer,
  rate: CircleCheck,
  pending: Clock
} as const

const icon = computed(() => iconMap[props.item.key as keyof typeof iconMap] ?? Box)
</script>

<template>
  <el-card class="stat-card" :class="`tone-${item.tone}`" shadow="hover">
    <el-skeleton v-if="loading" animated>
      <template #template>
        <div class="skel-line" />
        <div class="skel-block" />
      </template>
    </el-skeleton>
    <button v-else type="button" class="stat-body" @click="router.push(item.route)">
      <span class="stat-icon"><el-icon><component :is="icon" /></el-icon></span>
      <span class="stat-meta">
        <span class="stat-label">{{ item.label }}</span>
        <span class="stat-value">{{ item.display }}</span>
      </span>
    </button>
  </el-card>
</template>

<style scoped>
.stat-card { --stat-accent: var(--el-color-primary); }
.stat-card.tone-success { --stat-accent: var(--el-color-success); }
.stat-card.tone-warning { --stat-accent: var(--el-color-warning); }
.stat-body {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 4px 2px;
  border: 0;
  background: transparent;
  cursor: pointer;
  text-align: left;
  font: inherit;
  color: inherit;
}
.stat-body:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
  border-radius: 6px;
}
.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  flex: none;
  border-radius: var(--el-border-radius-base);
  font-size: 22px;
  color: var(--stat-accent);
  background: color-mix(in srgb, var(--stat-accent) 12%, transparent);
}
.stat-label { display: block; font-size: 13px; color: var(--el-text-color-secondary); }
.stat-value {
  display: block;
  margin-top: 4px;
  font-size: 26px;
  font-weight: 600;
  line-height: 1.2;
  color: var(--el-text-color-primary);
}
.tone-success .stat-value { color: var(--el-color-success); }
.tone-warning .stat-value { color: var(--el-color-warning); }
.skel-line { width: 70px; height: 13px; margin-bottom: 12px; border-radius: 4px; background: var(--el-fill-color); }
.skel-block { width: 90px; height: 28px; border-radius: 4px; background: var(--el-fill-color); }
</style>
```

- [ ] **Step 2: 构建验证（类型 + 打包）**

Run: `npm run build`
Expected: `vue-tsc --noEmit` 无错误，`vite build` 成功。

- [ ] **Step 3: 提交**

```bash
git add openapi-web/src/components/dashboard/StatCard.vue
git commit -m "feat(web): 首页统计卡组件 StatCard"
```

---

### Task 3: TrendChart 调用趋势图组件

**Files:**
- Create: `src/components/dashboard/TrendChart.vue`

**Interfaces:**
- Consumes: `OverviewSeries`（Task 1）；props `{ series: OverviewSeries; days: number; loading?: boolean }`。
- Produces: emit `(e: 'changeDays', days: number)`；容器据此重新拉取 `statsDaily`。

- [ ] **Step 1: 写组件**

`src/components/dashboard/TrendChart.vue`:

```vue
<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { init, use, type ECharts } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { OverviewSeries } from '@/components/dashboard/dashboard-model'

use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{ series: OverviewSeries; days: number; loading?: boolean }>()
const emit = defineEmits<{ (e: 'changeDays', days: number): void }>()

const chartRef = ref<HTMLDivElement>()
let chart: ECharts | null = null
let observer: MutationObserver | null = null
const onResize = () => chart?.resize()

function cssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || '#409eff'
}

function render() {
  if (!chart || !props.series.days.length) return
  chart.setOption({
    color: [cssVar('--el-color-primary'), cssVar('--el-color-success'), cssVar('--el-color-danger')],
    tooltip: { trigger: 'axis' },
    legend: { data: ['调用量', '成功量', '失败量'], top: 0, right: 10 },
    grid: { left: 48, right: 24, top: 44, bottom: 40 },
    xAxis: { type: 'category', data: props.series.days, axisLabel: { margin: 12 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '调用量', type: 'line', smooth: true, areaStyle: { opacity: 0.08 }, data: props.series.total },
      { name: '成功量', type: 'line', smooth: true, areaStyle: { opacity: 0.08 }, data: props.series.ok },
      { name: '失败量', type: 'line', smooth: true, areaStyle: { opacity: 0.08 }, data: props.series.fail }
    ]
  })
}

function ensureChart() {
  if (chartRef.value && !chart) {
    chart = init(chartRef.value)
    window.addEventListener('resize', onResize)
  }
}

function syncTheme() {
  observer?.disconnect()
  observer = new MutationObserver(render)
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
}

watch(() => props.series, async () => {
  ensureChart()
  await nextTick()
  render()
})

onMounted(async () => {
  ensureChart()
  await nextTick()
  render()
  syncTheme()
})

onBeforeUnmount(() => {
  observer?.disconnect()
  window.removeEventListener('resize', onResize)
  chart?.dispose()
  chart = null
})
</script>

<template>
  <el-card class="chart-card">
    <template #header>
      <div class="chart-header">
        <span class="chart-title">调用趋势</span>
        <el-radio-group
          :model-value="days"
          size="small"
          @update:model-value="(v) => emit('changeDays', Number(v))"
        >
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
          <el-radio-button :value="90">近 90 天</el-radio-button>
        </el-radio-group>
      </div>
    </template>
    <div v-if="!series.days.length && !loading" class="chart-empty">
      <el-empty description="暂无调用数据，调用接口后即可查看趋势" :image-size="60" />
    </div>
    <div v-else ref="chartRef" v-loading="loading" class="chart" :aria-label="`调用趋势图（近 ${days} 天，含调用量、成功量、失败量）`" />
  </el-card>
</template>

<style scoped>
.chart-header { display: flex; align-items: center; justify-content: space-between; }
.chart-title { font-weight: 600; }
.chart { height: 340px; }
</style>
```

- [ ] **Step 2: 构建验证**

Run: `npm run build`
Expected: 无类型错误，构建成功。

- [ ] **Step 3: 提交**

```bash
git add openapi-web/src/components/dashboard/TrendChart.vue
git commit -m "feat(web): 首页调用趋势图组件 TrendChart"
```

---

### Task 4: RankList 调用排行组件（TOP 10）

**Files:**
- Create: `src/components/dashboard/RankList.vue`

**Interfaces:**
- Consumes: `TopStat`（`src/api/index.ts`，字段 `total/ok/interfaceName?/appName?`）；props `{ interfaces: TopStat[]; apps: TopStat[]; loading?: boolean }`。
- Produces: 行点击 `router.push('/stats')`。

- [ ] **Step 1: 写组件**

`src/components/dashboard/RankList.vue`:

```vue
<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { TopStat } from '@/api'

const props = defineProps<{ interfaces: TopStat[]; apps: TopStat[]; loading?: boolean }>()
const router = useRouter()
const tab = ref<'interfaces' | 'apps'>('interfaces')
const rows = computed(() => (tab.value === 'interfaces' ? props.interfaces : props.apps))

function rate(row: TopStat): number {
  return row.total ? Math.round((row.ok / row.total) * 100) : 0
}
</script>

<template>
  <el-card class="rank-card">
    <template #header>
      <div class="rank-header">
        <span class="rank-title">调用排行 TOP 10</span>
        <el-radio-group v-model="tab" size="small">
          <el-radio-button value="interfaces">接口</el-radio-button>
          <el-radio-button value="apps">应用</el-radio-button>
        </el-radio-group>
      </div>
    </template>
    <div v-if="loading" class="rank-skeleton">
      <div v-for="i in 5" :key="i" class="skel-row" />
    </div>
    <el-empty v-else-if="!rows.length" description="暂无可调用记录" :image-size="60" />
    <ol v-else class="rank-list">
      <li v-for="(row, i) in rows" :key="i" class="rank-item">
        <button type="button" class="rank-main" @click="router.push('/stats')">
          <span class="rank-index" :class="{ top: i < 3 }">{{ i + 1 }}</span>
          <span class="rank-name">{{ row.interfaceName ?? row.appName ?? '-' }}</span>
          <span class="rank-calls">{{ row.total }} 次</span>
          <el-tag :type="rate(row) >= 90 ? 'success' : row.total ? 'warning' : 'info'" size="small">
            {{ rate(row) }}%
          </el-tag>
        </button>
      </li>
    </ol>
  </el-card>
</template>

<style scoped>
.rank-header { display: flex; align-items: center; justify-content: space-between; }
.rank-title { font-weight: 600; }
.rank-list { margin: 0; padding: 0; list-style: none; }
.rank-item + .rank-item { margin-top: 4px; }
.rank-main {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 8px;
  border: 0;
  border-radius: var(--el-border-radius-base);
  background: transparent;
  cursor: pointer;
  font: inherit;
  color: inherit;
}
.rank-main:hover { background: var(--el-fill-color-light); }
.rank-main:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 1px; }
.rank-index {
  width: 22px;
  height: 22px;
  flex: none;
  border-radius: 6px;
  background: var(--el-fill-color);
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 22px;
  text-align: center;
}
.rank-index.top { background: var(--el-color-primary); color: #fff; }
.rank-name { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rank-calls { flex: none; color: var(--el-text-color-secondary); font-size: 13px; }
.rank-skeleton { display: flex; flex-direction: column; gap: 8px; }
.skel-row { height: 38px; border-radius: 6px; background: var(--el-fill-color); }
</style>
```

- [ ] **Step 2: 构建验证**

Run: `npm run build`
Expected: 无类型错误，构建成功。

- [ ] **Step 3: 提交**

```bash
git add openapi-web/src/components/dashboard/RankList.vue
git commit -m "feat(web): 首页调用排行组件 RankList"
```

---

### Task 5: RecentLogs 最近调用动态组件

**Files:**
- Create: `src/components/dashboard/RecentLogs.vue`

**Interfaces:**
- Consumes: `ApiLog`（`src/api/index.ts`，字段 `id/method/path/appName/statusCode/success/costMs/createTime`）；props `{ logs: ApiLog[]; loading?: boolean }`。
- Produces: 行点击 `router.push('/logs')`。

- [ ] **Step 1: 写组件**

`src/components/dashboard/RecentLogs.vue`:

```vue
<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { ApiLog } from '@/api'

const props = defineProps<{ logs: ApiLog[]; loading?: boolean }>()
const router = useRouter()
</script>

<template>
  <el-card class="logs-card">
    <template #header><span class="logs-title">最近调用动态</span></template>
    <div v-if="loading" class="logs-skeleton">
      <div v-for="i in 4" :key="i" class="skel-row" />
    </div>
    <el-empty v-else-if="!logs.length" description="暂无调用日志" :image-size="60" />
    <ul v-else class="logs-list">
      <li v-for="log in logs" :key="log.id" class="log-item">
        <button type="button" class="log-main" @click="router.push('/logs')">
          <span class="log-path">{{ log.method }} {{ log.path }}</span>
          <span class="log-meta">
            {{ log.appName }} · {{ log.costMs }}ms ·
            <span :class="log.success ? 'log-ok' : 'log-fail'">{{ log.success ? '成功' : '失败' }}</span>
            · {{ log.createTime }}
          </span>
          <el-tag :type="log.success ? 'success' : 'danger'" size="small">{{ log.statusCode }}</el-tag>
        </button>
      </li>
    </ul>
  </el-card>
</template>

<style scoped>
.logs-title { font-weight: 600; }
.logs-list { margin: 0; padding: 0; list-style: none; }
.log-item + .log-item { margin-top: 4px; }
.log-main {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 8px;
  border: 0;
  border-radius: var(--el-border-radius-base);
  background: transparent;
  cursor: pointer;
  font: inherit;
  color: inherit;
}
.log-main:hover { background: var(--el-fill-color-light); }
.log-main:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 1px; }
.log-path {
  flex: none;
  max-width: 45%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}
.log-meta {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.log-ok { color: var(--el-color-success); }
.log-fail { color: var(--el-color-danger); }
.logs-skeleton { display: flex; flex-direction: column; gap: 8px; }
.skel-row { height: 38px; border-radius: 6px; background: var(--el-fill-color); }
</style>
```

- [ ] **Step 2: 构建验证**

Run: `npm run build`
Expected: 无类型错误，构建成功。

- [ ] **Step 3: 提交**

```bash
git add openapi-web/src/components/dashboard/RecentLogs.vue
git commit -m "feat(web): 首页最近调用动态组件 RecentLogs"
```

---

### Task 6: QuickStart 快速开始 + 快捷入口组件

**Files:**
- Create: `src/components/dashboard/QuickStart.vue`

**Interfaces:**
- Consumes: 无 props（静态内容）。
- Produces: 链接点击 `router.push(route)`。

- [ ] **Step 1: 写组件**

`src/components/dashboard/QuickStart.vue`:

```vue
<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
const links = [
  { label: '应用管理', route: '/apps' },
  { label: '接口管理', route: '/interfaces' },
  { label: '调用统计', route: '/stats' },
  { label: 'API 日志', route: '/logs' }
]
</script>

<template>
  <el-card class="quick-card">
    <h3 class="quick-title">快速开始</h3>
    <ol class="quick-steps">
      <li>在「应用管理」创建应用，获得 AccessKey / SecretKey</li>
      <li>在「接口管理」查看可调用的开放接口</li>
      <li>使用 SDK 或带签名请求调用接口（详见项目知识库）</li>
    </ol>
    <el-divider />
    <h3 class="quick-title">快捷入口</h3>
    <div class="quick-links">
      <button v-for="l in links" :key="l.route" type="button" class="quick-link" @click="router.push(l.route)">
        {{ l.label }}
      </button>
    </div>
  </el-card>
</template>

<style scoped>
.quick-title { margin: 0 0 12px; font-size: 15px; font-weight: 600; }
.quick-steps { margin: 0; padding-left: 20px; color: var(--el-text-color-regular); line-height: 1.9; }
.quick-links { display: flex; flex-wrap: wrap; gap: 16px; }
.quick-link {
  padding: 4px 2px;
  border: 0;
  background: transparent;
  color: var(--el-color-primary);
  cursor: pointer;
  font: inherit;
}
.quick-link:hover { text-decoration: underline; }
.quick-link:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; border-radius: 4px; }
</style>
```

- [ ] **Step 2: 构建验证**

Run: `npm run build`
Expected: 无类型错误，构建成功。

- [ ] **Step 3: 提交**

```bash
git add openapi-web/src/components/dashboard/QuickStart.vue
git commit -m "feat(web): 首页快速开始组件 QuickStart"
```

---

### Task 7: DashboardView 容器重写

**Files:**
- Rewrite: `src/views/DashboardView.vue`（整文件替换）

**Interfaces:**
- Consumes: `buildStatCards/buildOverviewSeries/DashboardStatItem/OverviewSeries`（Task 1）、`StatCard`（Task 2）、`TrendChart`（Task 3）、`RankList`（Task 4）、`RecentLogs`（Task 5）、`QuickStart`（Task 6）、`src/api/index.ts` 全部数据函数。
- Produces: 无（路由组件终点）。

- [ ] **Step 1: 重写容器**

`src/views/DashboardView.vue`:

```vue
<script setup lang="ts">
import { onActivated, onMounted, reactive, ref } from 'vue'
import { useUserStore } from '@/store/user'
import {
  listApps, mySubscribes, listInterfaces, listSubscribes,
  statsOverview, statsDaily, statsTopInterfaces, statsTopApps, listApiLogs,
  type ApiLog, type TopStat
} from '@/api'
import {
  buildOverviewSeries, buildStatCards,
  type DashboardStatItem, type OverviewSeries
} from '@/components/dashboard/dashboard-model'
import StatCard from '@/components/dashboard/StatCard.vue'
import TrendChart from '@/components/dashboard/TrendChart.vue'
import RankList from '@/components/dashboard/RankList.vue'
import RecentLogs from '@/components/dashboard/RecentLogs.vue'
import QuickStart from '@/components/dashboard/QuickStart.vue'

const userStore = useUserStore()
const isAdmin = userStore.user?.userRole === 'admin'
const today = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric', month: 'long', day: 'numeric', weekday: 'long'
})

const cards = ref<DashboardStatItem[]>([])
const series = ref<OverviewSeries>({ days: [], total: [], ok: [], fail: [] })
const chartDays = ref(7)
const topInterfaces = ref<TopStat[]>([])
const topApps = ref<TopStat[]>([])
const logs = ref<ApiLog[]>([])
const loading = reactive({ cards: false, trend: false, ranks: false, logs: false })
const failed = reactive({ trend: false, ranks: false, logs: false })

async function loadCards() {
  loading.cards = true
  try {
    const apps = userStore.user ? await listApps(userStore.user.id) : []
    const subscribes = await mySubscribes()
    const infos = await listInterfaces()
    let total = 0
    let successRate = 0
    let pending = 0
    if (isAdmin) {
      const overview = await statsOverview()
      total = overview.total
      successRate = overview.successRate
      pending = (await listSubscribes(0)).length
    }
    cards.value = buildStatCards({
      appCount: apps.length,
      subscribeCount: subscribes.filter((s) => s.status === 1).length,
      interfaceCount: infos.length,
      total,
      successRate,
      pendingCount: pending,
      isAdmin
    })
  } finally {
    loading.cards = false
  }
}

async function loadTrend() {
  if (!isAdmin) return
  loading.trend = true
  failed.trend = false
  try {
    series.value = buildOverviewSeries(await statsDaily(chartDays.value))
  } catch {
    failed.trend = true
  } finally {
    loading.trend = false
  }
}

async function loadRanks() {
  if (!isAdmin) return
  loading.ranks = true
  failed.ranks = false
  try {
    const [interfaces, apps] = await Promise.all([statsTopInterfaces(10), statsTopApps(10)])
    topInterfaces.value = interfaces
    topApps.value = apps
  } catch {
    failed.ranks = true
  } finally {
    loading.ranks = false
  }
}

async function loadLogs() {
  if (!isAdmin) return
  loading.logs = true
  failed.logs = false
  try {
    logs.value = (await listApiLogs({ current: 1, size: 8 })).records
  } catch {
    failed.logs = true
  } finally {
    loading.logs = false
  }
}

async function reload() {
  await Promise.allSettled([loadCards(), loadTrend(), loadRanks(), loadLogs()])
}

function onDaysChange(days: number) {
  chartDays.value = days
  loadTrend()
}

onMounted(reload)
onActivated(reload)
</script>

<template>
  <div class="dashboard">
    <div class="page-head">
      <div>
        <h2 class="page-title">概览</h2>
        <p class="page-greet">你好，{{ userStore.user?.userName || userStore.user?.userAccount || '用户' }} 👋 · {{ today }}</p>
      </div>
      <el-button type="primary" plain @click="reload">刷新</el-button>
    </div>

    <div class="card-grid">
      <StatCard v-for="item in cards" :key="item.key" :item="item" :loading="loading.cards" />
    </div>

    <template v-if="isAdmin">
      <div class="row">
        <div class="col-main">
          <TrendChart :series="series" :days="chartDays" :loading="loading.trend" @change-days="onDaysChange" />
          <div v-if="failed.trend" class="module-error">
            <span>趋势加载失败</span>
            <el-button size="small" @click="loadTrend">重试</el-button>
          </div>
        </div>
        <div class="col-side">
          <RankList :interfaces="topInterfaces" :apps="topApps" :loading="loading.ranks" />
          <div v-if="failed.ranks" class="module-error">
            <span>排行加载失败</span>
            <el-button size="small" @click="loadRanks">重试</el-button>
          </div>
        </div>
      </div>

      <div class="row">
        <div class="col-main">
          <RecentLogs :logs="logs" :loading="loading.logs" />
          <div v-if="failed.logs" class="module-error">
            <span>动态加载失败</span>
            <el-button size="small" @click="loadLogs">重试</el-button>
          </div>
        </div>
        <div class="col-side">
          <QuickStart />
        </div>
      </div>
    </template>

    <div v-else class="row">
      <div class="col-main">
        <QuickStart />
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.page-title { margin: 0; font-size: 20px; font-weight: 600; }
.page-greet { margin: 6px 0 0; color: var(--el-text-color-secondary); font-size: 14px; }
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 16px;
}
.row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
  margin-top: 16px;
}
.col-main, .col-side {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}
@media (min-width: 1200px) {
  .row { grid-template-columns: 14fr 10fr; }
}
.module-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
```

- [ ] **Step 2: 构建验证**

Run: `npm run build`
Expected: `vue-tsc --noEmit` 无错误，`vite build` 成功。

- [ ] **Step 3: 提交**

```bash
git add openapi-web/src/views/DashboardView.vue
git commit -m "feat(web): 重写首页概览为数据大盘"
```

---

### Task 8: 端到端验证与回归

**Files:** 无新文件（除非修复发现问题）。

- [ ] **Step 1: 单元测试 + 构建全绿**

Run: `node --test tests/dashboard-model.test.ts` 且 `npm run build`
Expected: 测试 5/5 通过；`vue-tsc` 与 `vite build` 零错误。

- [ ] **Step 2: 启动本地栈并做浏览器回归（browser-use / CDP 控制本机 Chrome）**

前置：MySQL/Redis/后端 8101/vite 5173 已启动（沿用现有本地环境；若未启动按项目 README 启动）。

检查清单（管理员账号 admin，普通用户账号取演示数据或新建一个）：
1. `http://localhost:5173/dashboard` 管理员登录：6 张统计卡、调用趋势图三条序列（可切换 7/30/90）、排行 TOP 10（接口/应用两个 tab）、最近动态 8 条、快速开始卡片齐全；
2. 普通用户登录：只显示 3 张统计卡 + 快速开始，无 stats 请求（浏览器 Network 面板无 `/v1/stats` 调用）；
3. 四档分辨率 1440 / 1024 / 768 / 320：卡片网格与双栏正确堆叠，无横向溢出；
4. 亮/暗主题切换后：卡片、图表（颜色随 token 变化）均正常；
5. 控制台无报错；Tab 键可依次聚焦刷新按钮、各统计卡、排行行、动态行、快捷入口；
6. 空态验证（可选）：无日志时"最近调用动态"显示空态提示。

发现缺陷：修复后重新跑 Step 1 与受影响检查项，并以 `fix(web): 修复首页回归问题` 提交；无缺陷则不新增提交。

---

## Self-Review 记录

- **Spec 覆盖**：§4.1 页头/统计卡/趋势/排行/动态/快速开始 → Task 2-7；§4.2 普通用户视图 → Task 7 `v-else` 分支 + Task 1 `buildStatCards`；§5 接口与角色限制 → Task 7；§6 状态处理 → Task 2-7（骨架/错误重试/空态）；§7 视觉 token → 全部组件；§8 响应式/暗色 → Task 3（MutationObserver）+ Task 7（grid + media）；§9 无障碍 → 组件均为原生 button + focus-visible + aria-label；§10 验证 → Task 8。排行 TOP 10 与趋势三序列 → Task 1/3/4/7。
- **占位符扫描**：无 TBD/TODO；每个代码步骤都含完整代码。
- **类型一致性**：`DashboardStatItem`、`OverviewSeries`、`buildOverviewSeries`、`buildStatCards` 在 Task 1 定义并在 Task 2/3/7 按同一签名使用；`TopStat`、`ApiLog` 直接复用 `src/api/index.ts` 既有类型。
