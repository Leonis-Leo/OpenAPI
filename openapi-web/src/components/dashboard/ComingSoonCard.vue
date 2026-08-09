<script setup lang="ts">
defineProps<{
  icon: unknown
  title: string
  desc: string
}>()
</script>

<template>
  <div class="coming-card">
    <span class="coming-icon">
      <el-icon><component :is="icon" /></el-icon>
    </span>
    <div class="coming-copy">
      <div class="coming-title-row">
        <strong class="coming-title">{{ title }}</strong>
        <el-tag type="info" size="small" effect="plain">即将上线</el-tag>
      </div>
      <p class="coming-desc">{{ desc }}</p>
    </div>
  </div>
</template>

<style scoped>
.coming-card {
  display: flex;
  gap: 12px;
  padding: 16px;
  border: 1px dashed var(--app-border);
  border-radius: var(--app-radius);
  background: var(--app-surface);
}
.coming-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex: none;
  border-radius: 10px;
  font-size: 19px;
  color: var(--app-muted);
  background: var(--el-fill-color-light, #f5f7fa);
}
.coming-copy {
  min-width: 0;
}
.coming-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.coming-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text);
}
.coming-desc {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--app-muted);
}
</style>
