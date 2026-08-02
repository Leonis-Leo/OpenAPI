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
