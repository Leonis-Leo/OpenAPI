<template>
  <div class="coming-soon">
    <el-card class="cs-card" shadow="never">
      <div class="cs-icon"><el-icon :size="30"><Tools /></el-icon></div>
      <h2>{{ name }}</h2>
      <p class="cs-desc">{{ desc || '该功能正在规划中，后续版本上线，敬请期待。' }}</p>
      <el-tag type="info" effect="plain">规划中</el-tag>
      <div class="cs-actions">
        <el-button @click="router.push('/dashboard')">返回工作台</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Tools } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const name = computed(() => String(route.query.name || '功能'))
const desc = computed(() => String(route.query.desc || ''))
</script>

<style scoped>
.coming-soon {
  min-height: 60vh;
  display: grid;
  place-items: center;
}
.cs-card {
  width: min(100%, 480px);
  text-align: center;
  padding: 12px 0 20px;
}
.cs-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  color: var(--app-primary, #2563eb);
  background: #eff6ff;
}
.cs-card h2 {
  margin: 0;
  font-size: 20px;
  color: var(--app-text, #172033);
}
.cs-desc {
  margin: 10px auto 16px;
  max-width: 360px;
  color: var(--app-muted, #64748b);
  font-size: 13px;
  line-height: 1.7;
}
.cs-actions {
  margin-top: 18px;
}
</style>
