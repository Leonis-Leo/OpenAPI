<template>
  <div>
    <div class="toolbar">
      <h2>调用统计</h2>
      <el-button @click="reload">刷新</el-button>
    </div>
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="card-head">
            <el-icon class="card-icon"><Odometer /></el-icon>
            <span>调用总量</span>
          </div>
          <p class="stat-value">{{ overview.total }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="card-head">
            <el-icon class="card-icon success"><CircleCheck /></el-icon>
            <span>成功</span>
          </div>
          <p class="stat-value success">{{ overview.success }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="card-head">
            <el-icon class="card-icon fail"><CircleClose /></el-icon>
            <span>失败</span>
          </div>
          <p class="stat-value fail">{{ overview.fail }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="card-head">
            <el-icon class="card-icon"><DataLine /></el-icon>
            <span>成功率</span>
          </div>
          <p class="stat-value">{{ overview.successRate }}%</p>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="chart-card">
      <template #header>
        <div class="chart-header">
          <span>调用趋势</span>
          <el-radio-group v-model="days" size="small" @change="loadChart">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
            <el-radio-button :value="90">近 90 天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-empty v-if="!daily.length" description="暂无调用数据，调用接口后即可查看趋势" />
      <div v-else ref="chartRef" class="chart"></div>
    </el-card>
    <el-row :gutter="16" class="rank-row">
      <el-col :span="12">
        <el-card>
          <template #header>接口调用排行</template>
          <el-table :data="topInterfaces" border stripe size="small" v-loading="rankLoading">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="interfaceName" label="接口" min-width="140" />
            <el-table-column prop="total" label="调用量" width="90" sortable />
            <el-table-column label="成功率" width="90">
              <template #default="{ row }">
                <el-tag :type="row.total ? 'success' : 'info'" size="small">
                  {{ row.total ? Math.round((row.ok / row.total) * 100) : 0 }}%
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!topInterfaces.length && !rankLoading" description="暂无数据" :image-size="60" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>应用调用排行</template>
          <el-table :data="topApps" border stripe size="small" v-loading="rankLoading">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="appName" label="应用" min-width="140" />
            <el-table-column prop="total" label="调用量" width="90" sortable />
            <el-table-column label="成功率" width="90">
              <template #default="{ row }">
                <el-tag :type="row.total ? 'success' : 'info'" size="small">
                  {{ row.total ? Math.round((row.ok / row.total) * 100) : 0 }}%
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!topApps.length && !rankLoading" description="暂无数据" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onActivated, onBeforeUnmount, onMounted, ref } from 'vue'
import { CircleCheck, CircleClose, DataLine, Odometer } from '@element-plus/icons-vue'
import { init, use, type ECharts } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import {
  statsOverview,
  statsDaily,
  statsTopInterfaces,
  statsTopApps,
  type DailyStat,
  type StatsOverview,
  type TopStat
} from '@/api'

use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const overview = ref<StatsOverview>({ total: 0, success: 0, fail: 0, successRate: 0 })
const daily = ref<DailyStat[]>([])
const days = ref(7)
const topInterfaces = ref<TopStat[]>([])
const topApps = ref<TopStat[]>([])
const rankLoading = ref(false)
const chartRef = ref<HTMLDivElement>()
let chart: ECharts | null = null

const onResize = () => chart?.resize()

async function loadChart() {
  daily.value = await statsDaily(days.value)
  await nextTick()
  if (!chartRef.value) return
  if (!chart) {
    chart = init(chartRef.value)
    window.addEventListener('resize', onResize)
  }
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['调用量', '成功量'], top: 0, right: 10 },
    grid: { left: 40, right: 20, top: 40, bottom: 40 },
    xAxis: {
      type: 'category',
      data: daily.value.map((d) => d.day),
      axisLabel: { margin: 12 }
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '调用量',
        type: 'line',
        smooth: true,
        data: daily.value.map((d) => d.total)
      },
      {
        name: '成功量',
        type: 'line',
        smooth: true,
        data: daily.value.map((d) => d.ok)
      }
    ]
  })
}

async function loadRanks() {
  rankLoading.value = true
  try {
    ;[topInterfaces.value, topApps.value] = await Promise.all([
      statsTopInterfaces(10),
      statsTopApps(10)
    ])
  } finally {
    rankLoading.value = false
  }
}

async function reload() {
  overview.value = await statsOverview()
  await Promise.all([loadChart(), loadRanks()])
}

onMounted(async () => {
  await reload()
})

onActivated(() => {
  reload()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
})
</script>

<style scoped>
.stat-label {
  margin: 0;
  color: #909399;
}
.stat-value {
  margin: 8px 0 0;
  font-size: 26px;
  font-weight: 600;
}
.stat-value.success {
  color: #67c23a;
}
.stat-value.fail {
  color: #f56c6c;
}
.card-head {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}
.card-icon {
  font-size: 18px;
  color: #409eff;
}
.card-icon.success {
  color: #67c23a;
}
.card-icon.fail {
  color: #f56c6c;
}
.chart-card {
  margin-top: 16px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.chart {
  height: 360px;
}
.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.rank-row {
  margin-top: 16px;
}
</style>
