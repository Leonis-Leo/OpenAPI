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
      <div ref="chartRef" class="chart"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { statsOverview, statsDaily, type StatsOverview } from '@/api'

const overview = ref<StatsOverview>({ total: 0, success: 0, fail: 0, successRate: 0 })
const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

onMounted(async () => {
  overview.value = await statsOverview()
  const daily = await statsDaily(7)
  if (chartRef.value) {
    chart = echarts.init(chartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['调用量', '成功量'] },
      grid: { left: 40, right: 20, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: daily.map((d) => d.day) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        {
          name: '调用量',
          type: 'line',
          smooth: true,
          data: daily.map((d) => d.total)
        },
        {
          name: '成功量',
          type: 'line',
          smooth: true,
          data: daily.map((d) => d.ok)
        }
      ]
    })
  }
})

onBeforeUnmount(() => {
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
