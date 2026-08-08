<template>
  <main class="network-error" role="main">
    <section class="error-card" aria-labelledby="network-error-title">
      <div class="error-icon" aria-hidden="true">
        <el-icon :size="34"><Connection /></el-icon>
      </div>
      <p class="eyebrow">CONNECTION LOST</p>
      <h1 id="network-error-title">网络连接异常</h1>
      <p class="description">
        暂时无法连接到 OpenAPI 服务，请检查网络或确认后端服务是否正常运行。
      </p>
      <div class="actions">
        <el-button type="primary" size="large" @click="retry">重新连接</el-button>
        <el-button size="large" @click="goHome">返回首页</el-button>
      </div>
      <p class="hint">如果问题持续存在，请联系系统管理员。</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { Connection } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()

function retry() {
  window.location.reload()
}

function goHome() {
  router.push('/')
}
</script>

<style scoped>
.network-error {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px;
  background: #f5f7fb;
}

.error-card {
  width: min(100%, 520px);
  padding: 48px 40px 40px;
  text-align: center;
  background: #fff;
  border: 1px solid #e6ebf2;
  border-radius: 16px;
  box-shadow: 0 18px 45px rgba(20, 42, 76, 0.08);
}

.error-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 20px;
  display: grid;
  place-items: center;
  color: #e85d68;
  background: #fff0f1;
  border-radius: 20px;
}

.eyebrow {
  margin: 0 0 10px;
  color: #9aa6b8;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

h1 {
  margin: 0;
  color: #172b4d;
  font-size: 28px;
  line-height: 1.3;
}

.description {
  margin: 14px auto 28px;
  max-width: 380px;
  color: #68758a;
  font-size: 15px;
  line-height: 1.8;
}

.actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.hint {
  margin: 24px 0 0;
  color: #a3adbb;
  font-size: 13px;
}

@media (max-width: 520px) {
  .network-error { padding: 20px; }
  .error-card { padding: 36px 24px 30px; }
  .actions { flex-direction: column; }
}
</style>
