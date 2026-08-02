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
