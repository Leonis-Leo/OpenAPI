<template>
  <div>
    <h2>调用统计</h2>
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
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { CircleCheck, CircleClose, DataLine, Odometer } from '@element-plus/icons-vue'
import { init, use, type ECharts } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { statsOverview, statsDaily, type DailyStat, type StatsOverview } from '@/api'

use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const overview = ref<StatsOverview>({ total: 0, success: 0, fail: 0, successRate: 0 })
const daily = ref<DailyStat[]>([])
const days = ref(7)
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
    legend: { data: ['调用量', '成功量'] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: daily.value.map((d) => d.day) },
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

onMounted(async () => {
  overview.value = await statsOverview()
  await loadChart()
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
.chart {
  height: 360px;
}
.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
