<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps<{
  interfaceTotal: number
  interfaceConfigured: number
  appTotal: number
  appConfigured: number
  loading?: boolean
}>()

const router = useRouter()

const interfaceRate = computed(() =>
  props.interfaceTotal ? Math.round((props.interfaceConfigured / props.interfaceTotal) * 100) : 0
)
const appRate = computed(() =>
  props.appTotal ? Math.round((props.appConfigured / props.appTotal) * 100) : 0
)
</script>

<template>
  <el-card class="rl-card">
    <template #header>
      <span class="rl-title">限流状态</span>
    </template>

    <div v-if="loading" class="rl-skeleton">
      <div v-for="i in 2" :key="i" class="skel-row" />
    </div>
    <div v-else class="rl-body">
      <div class="rl-row">
        <div class="rl-label">
          <span>接口限流</span>
          <span class="rl-count">{{ interfaceConfigured }} / {{ interfaceTotal }}</span>
        </div>
        <div class="rl-bar">
          <i :style="{ width: `${interfaceRate}%` }" />
        </div>
      </div>
      <div class="rl-row">
        <div class="rl-label">
          <span>应用限流</span>
          <span class="rl-count">{{ appConfigured }} / {{ appTotal }}</span>
        </div>
        <div class="rl-bar">
          <i :style="{ width: `${appRate}%` }" />
        </div>
      </div>
      <button type="button" class="rl-link" @click="router.push('/ratelimit')">
        去配置限流
        <span aria-hidden="true">→</span>
      </button>
    </div>
  </el-card>
</template>

<style scoped>
.rl-card {
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  background: var(--app-surface);
}
.rl-card :deep(.el-card__header) {
  padding: 16px 18px 12px;
  border-bottom-color: var(--app-border);
}
.rl-card :deep(.el-card__body) {
  padding: 14px 18px 16px;
}
.rl-title {
  font-weight: 600;
  color: var(--app-text);
}
.rl-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.rl-row {
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.rl-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: var(--app-text);
}
.rl-count {
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 12px;
  color: var(--app-muted);
}
.rl-bar {
  height: 7px;
  overflow: hidden;
  border-radius: 99px;
  background: var(--el-fill-color, #f0f2f5);
}
.rl-bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--app-primary);
  transition: width .25s ease;
}
.rl-link {
  width: 100%;
  margin-top: 4px;
  padding: 8px;
  border: 0;
  border-radius: 8px;
  background: #f8fafc;
  color: var(--app-primary);
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
}
.rl-link:hover {
  background: #eff6ff;
}
.rl-link:focus-visible {
  outline: 2px solid var(--app-primary);
  outline-offset: 2px;
}
.rl-skeleton {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.skel-row {
  height: 44px;
  border-radius: 6px;
  background: var(--el-fill-color);
}
</style>
