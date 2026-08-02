<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
const links = [
  { label: '应用管理', route: '/apps' },
  { label: '接口管理', route: '/interfaces' },
  { label: '调用统计', route: '/stats' },
  { label: 'API 日志', route: '/logs' }
]
</script>

<template>
  <el-card class="quick-card">
    <h3 class="quick-title">快速开始</h3>
    <ol class="quick-steps">
      <li>在「应用管理」创建应用，获得 AccessKey / SecretKey</li>
      <li>在「接口管理」查看可调用的开放接口</li>
      <li>使用 SDK 或带签名请求调用接口（详见项目知识库）</li>
    </ol>
    <el-divider />
    <h3 class="quick-title">快捷入口</h3>
    <div class="quick-links">
      <button v-for="l in links" :key="l.route" type="button" class="quick-link" @click="router.push(l.route)">
        {{ l.label }}
      </button>
    </div>
  </el-card>
</template>

<style scoped>
.quick-title { margin: 0 0 12px; font-size: 15px; font-weight: 600; }
.quick-steps { margin: 0; padding-left: 20px; color: var(--el-text-color-regular); line-height: 1.9; }
.quick-links { display: flex; flex-wrap: wrap; gap: 16px; }
.quick-link {
  padding: 4px 2px;
  border: 0;
  background: transparent;
  color: var(--el-color-primary);
  cursor: pointer;
  font: inherit;
}
.quick-link:hover { text-decoration: underline; }
.quick-link:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; border-radius: 4px; }
</style>
