<template>
  <div class="table-skeleton" role="status" aria-label="加载中">
    <div v-for="i in rows" :key="i" class="sk-row">
      <span
        v-for="j in cols"
        :key="j"
        class="sk-cell"
        :style="{ width: `${Math.min(96, 40 + ((i * 17 + j * 23) % 45))}%` }"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ rows?: number; cols?: number }>(), {
  rows: 6,
  cols: 6
})
</script>

<style scoped>
.table-skeleton {
  padding: 4px 16px 16px;
  border: 1px solid var(--app-border, #e7edf4);
  border-radius: 10px;
  background: var(--app-surface, #fff);
}
.sk-row {
  display: flex;
  gap: 16px;
  padding: 13px 0;
  border-bottom: 1px solid var(--el-border-color-lighter, #ebeef5);
}
.sk-row:last-child {
  border-bottom: 0;
}
.sk-cell {
  height: 13px;
  flex: 1;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f2f5 25%, #e6e9ef 37%, #f0f2f5 63%);
  background-size: 400% 100%;
  animation: sk-pulse 1.4s ease infinite;
}
@keyframes sk-pulse {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}
</style>
