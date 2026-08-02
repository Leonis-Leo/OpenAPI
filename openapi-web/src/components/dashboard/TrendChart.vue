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

function onDaysChange(value: string | number | boolean) {
  emit('changeDays', Number(value))
}

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
          @update:model-value="onDaysChange"
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
