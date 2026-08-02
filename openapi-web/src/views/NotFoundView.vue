<template>
  <div class="not-found">
    <div class="code">404</div>
    <h2 class="title">页面不存在</h2>
    <p class="desc">您访问的页面不存在或已被移除，请检查地址是否正确</p>
    <div class="actions">
      <el-button type="primary" @click="router.push('/')">返回首页</el-button>
      <el-button @click="router.back()">返回上一页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.not-found {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--el-bg-color-page, #f0f2f5);
}
.code {
  font-size: 96px;
  font-weight: 700;
  line-height: 1;
  color: var(--el-color-primary, #409eff);
  opacity: 0.35;
}
.title {
  margin: 12px 0 8px;
  font-size: 24px;
}
.desc {
  color: #909399;
  margin: 0 0 24px;
}
.actions {
  display: flex;
  gap: 12px;
}
</style>
