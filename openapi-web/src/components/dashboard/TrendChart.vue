<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { init, use, type ECharts } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { OverviewSeries } from '@/components/dashboard/dashboard-model'

use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{ series: OverviewSeries; days: number; loading?: boolean }>()
const emit = defineEmits<{ (e: 'changeDays', days: number): void }>()
const hasData = computed(() => props.series.days.length > 0)
const hasEnoughData = computed(() => props.series.days.length > 1)
const latest = computed(() => {
  const index = props.series.days.length - 1
  return index >= 0 ? { total: props.series.total[index] ?? 0, ok: props.series.ok[index] ?? 0, fail: props.series.fail[index] ?? 0 } : null
})

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
  if (!chart || !hasEnoughData.value) return
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
    <div v-if="!hasData && !loading" class="chart-empty">
      <el-empty description="暂无调用数据，调用接口后即可查看趋势" :image-size="60" />
    </div>
    <div v-else-if="!hasEnoughData && !loading" class="chart-sparse">
      <el-empty description="当前时间范围只有 1 天数据" :image-size="54" />
      <div v-if="latest" class="sparse-summary">
        <span><strong>{{ latest.total }}</strong> 总调用</span>
        <span class="success"><strong>{{ latest.ok }}</strong> 成功</span>
        <span class="danger"><strong>{{ latest.fail }}</strong> 失败</span>
      </div>
    </div>
    <div v-else ref="chartRef" v-loading="loading" class="chart" :aria-label="`调用趋势图（近 ${days} 天，含调用量、成功量、失败量）`" />
  </el-card>
</template>

<style scoped>
.chart-header { display: flex; align-items: center; justify-content: space-between; }
.chart-title { font-weight: 600; }
.chart { height: 280px; }
.chart-empty, .chart-sparse { min-height: 280px; display: grid; place-items: center; align-content: center; }
.sparse-summary { display: flex; gap: 24px; margin-top: -18px; color: var(--el-text-color-secondary); font-size: 13px; }
.sparse-summary strong { color: var(--el-text-color-primary); font-size: 20px; margin-right: 4px; }
.sparse-summary .success strong { color: var(--el-color-success); }
.sparse-summary .danger strong { color: var(--el-color-danger); }
</style>
