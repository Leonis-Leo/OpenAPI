<template>
  <div>
    <h2>调用统计</h2>
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card>
          <p class="stat-label">调用总量</p>
          <p class="stat-value">{{ overview.total }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <p class="stat-label">成功</p>
          <p class="stat-value success">{{ overview.success }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <p class="stat-label">失败</p>
          <p class="stat-value fail">{{ overview.fail }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <p class="stat-label">成功率</p>
          <p class="stat-value">{{ overview.successRate }}%</p>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="chart-card">
      <template #header>近 7 天调用趋势</template>
      <el-empty v-if="!daily.length" description="暂无调用数据，调用接口后即可查看趋势" />
      <div v-else ref="chartRef" class="chart"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { init, use, type ECharts } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { statsOverview, statsDaily, type DailyStat, type StatsOverview } from '@/api'

use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const overview = ref<StatsOverview>({ total: 0, success: 0, fail: 0, successRate: 0 })
const daily = ref<DailyStat[]>([])
const chartRef = ref<HTMLDivElement>()
let chart: ECharts | null = null

const onResize = () => chart?.resize()

onMounted(async () => {
  overview.value = await statsOverview()
  daily.value = await statsDaily(7)
  await nextTick()
  if (chartRef.value) {
    chart = init(chartRef.value)
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
    window.addEventListener('resize', onResize)
  }
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
.chart-card {
  margin-top: 16px;
}
.chart {
  height: 360px;
}
</style>
