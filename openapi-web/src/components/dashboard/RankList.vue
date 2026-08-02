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
