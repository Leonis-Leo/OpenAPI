<script setup lang="ts">
import { onActivated, onMounted, reactive, ref } from 'vue'
import { useUserStore } from '@/store/user'
import {
  listApps, mySubscribes, listInterfaces, listSubscribes,
  statsOverview, statsDaily, statsTopInterfaces, statsTopApps, listApiLogs,
  listRateLimitConfigs, listAppRateLimitConfigs, pageNotifications, unreadNotificationCount,
  type ApiLog, type TopStat, type SubscribeInfo, type NotificationItem
} from '@/api'
import { DataLine, Document, Odometer } from '@element-plus/icons-vue'
import {
  buildOverviewSeries, buildStatCards,
  type DashboardStatItem, type OverviewSeries
} from '@/components/dashboard/dashboard-model'
import StatCard from '@/components/dashboard/StatCard.vue'
import TrendChart from '@/components/dashboard/TrendChart.vue'
import RankList from '@/components/dashboard/RankList.vue'
import RecentLogs from '@/components/dashboard/RecentLogs.vue'
import QuickStart from '@/components/dashboard/QuickStart.vue'
import QuickAccess from '@/components/dashboard/QuickAccess.vue'
import TodoPanel from '@/components/dashboard/TodoPanel.vue'
import LatestNotifications from '@/components/dashboard/LatestNotifications.vue'
import RateLimitStatus from '@/components/dashboard/RateLimitStatus.vue'
import ComingSoonCard from '@/components/dashboard/ComingSoonCard.vue'

const userStore = useUserStore()
const isAdmin = userStore.user?.userRole === 'admin'
const today = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric', month: 'long', day: 'numeric', weekday: 'long'
})

const cards = ref<DashboardStatItem[]>([])
const appCount = ref(0)
const interfaceCount = ref(0)
const subscribeCount = ref(0)
const pendingCount = ref(0)
const series = ref<OverviewSeries>({ days: [], total: [], ok: [], fail: [] })
const chartDays = ref(7)
const topInterfaces = ref<TopStat[]>([])
const topApps = ref<TopStat[]>([])
const logs = ref<ApiLog[]>([])
const pendingList = ref<SubscribeInfo[]>([])
const notifications = ref<NotificationItem[]>([])
const unreadNotifications = ref(0)
const rateLimit = reactive({ interfaceTotal: 0, interfaceConfigured: 0, appTotal: 0, appConfigured: 0 })
const loading = reactive({ cards: false, trend: false, ranks: false, logs: false })
const loadingExtras = reactive({ todo: false, notifications: false, ratelimit: false })
const failed = reactive({ trend: false, ranks: false, logs: false })

async function loadCards() {
  loading.cards = true
  try {
    const apps = userStore.user ? await listApps() : []
    const subscribes = await mySubscribes()
    const infos = await listInterfaces()
    let total = 0
    let successRate = 0
    let pending = 0
    if (isAdmin) {
      const overview = await statsOverview()
      total = overview.total
      successRate = overview.successRate
      pendingList.value = await listSubscribes(0)
      pending = pendingList.value.length
    }
    appCount.value = apps.length
    interfaceCount.value = infos.length
    subscribeCount.value = subscribes.filter((s) => s.status === 1).length
    pendingCount.value = pending
    cards.value = buildStatCards({
      appCount: appCount.value,
      subscribeCount: subscribeCount.value,
      interfaceCount: interfaceCount.value,
      total,
      successRate,
      pendingCount: pendingCount.value,
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

async function loadTodo() {
  if (!isAdmin) return
  loadingExtras.todo = true
  try {
    unreadNotifications.value = await unreadNotificationCount()
  } finally {
    loadingExtras.todo = false
  }
}

async function loadNotifications() {
  loadingExtras.notifications = true
  try {
    notifications.value = (await pageNotifications({ current: 1, size: 3 })).records
  } finally {
    loadingExtras.notifications = false
  }
}

async function loadRateLimit() {
  if (!isAdmin) return
  loadingExtras.ratelimit = true
  try {
    const [interfaces, apps] = await Promise.all([listRateLimitConfigs(), listAppRateLimitConfigs()])
    rateLimit.interfaceTotal = interfaces.length
    rateLimit.interfaceConfigured = interfaces.filter((c) => c.configured).length
    rateLimit.appTotal = apps.length
    rateLimit.appConfigured = apps.filter((c) => c.configured).length
  } finally {
    loadingExtras.ratelimit = false
  }
}

async function reload() {
  await Promise.allSettled([
    loadCards(), loadTodo(), loadNotifications(), loadRateLimit(),
    loadTrend(), loadRanks(), loadLogs()
  ])
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

    <div class="quick-access-section">
      <QuickAccess :is-admin="isAdmin" />
    </div>

    <div class="card-grid">
      <StatCard v-for="item in cards" :key="item.key" :item="item" :loading="loading.cards" />
    </div>

    <div class="lifecycle-section">
      <QuickStart
        :app-count="appCount"
        :interface-count="interfaceCount"
        :subscribe-count="subscribeCount"
        :pending-count="pendingCount"
        :is-admin="isAdmin"
        :loading="loading.cards"
      />
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
          <TodoPanel
            :pending="pendingList"
            :unread-notifications="unreadNotifications"
            :loading="loadingExtras.todo"
          />
        </div>
      </div>

      <div class="info-row">
        <LatestNotifications
          :notifications="notifications"
          :loading="loadingExtras.notifications"
        />
        <RateLimitStatus
          :interface-total="rateLimit.interfaceTotal"
          :interface-configured="rateLimit.interfaceConfigured"
          :app-total="rateLimit.appTotal"
          :app-configured="rateLimit.appConfigured"
          :loading="loadingExtras.ratelimit"
        />
      </div>
    </template>

    <div v-else class="notify-section">
      <LatestNotifications
        :notifications="notifications"
        :loading="loadingExtras.notifications"
      />
    </div>

    <div class="coming-row">
      <ComingSoonCard
        :icon="Odometer"
        title="监控告警"
        desc="JVM 指标、调用量波动与限流命中率监控"
      />
      <ComingSoonCard
        :icon="Document"
        title="API 文档与 SDK"
        desc="接入文档、多语言 SDK 与示例代码下载"
      />
      <ComingSoonCard
        :icon="DataLine"
        title="压测报告"
        desc="QPS / 响应时间基准与容量评估报告"
      />
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
.lifecycle-section {
  margin-top: 16px;
}
.quick-access-section {
  margin-bottom: 16px;
}
.info-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
  margin-top: 16px;
}
.notify-section {
  margin-top: 16px;
}
.coming-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
  margin-top: 16px;
}
@media (min-width: 1200px) {
  .info-row {
    grid-template-columns: 1fr 1fr;
  }
  .coming-row {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
