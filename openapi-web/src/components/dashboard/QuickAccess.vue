<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps<{ isAdmin: boolean }>()

const router = useRouter()

const guideSteps = [
  {
    text: '创建应用获取密钥',
    action: '去创建',
    route: '/apps'
  },
  {
    text: '订阅所需接口',
    action: '去订阅',
    route: '/interfaces'
  },
  {
    text: '在线调试签名调用',
    action: '去调试',
    route: '/interfaces'
  }
]

const links = computed(() => {
  const base = [
    { label: '应用管理', route: '/apps' },
    { label: '接口管理', route: '/interfaces' },
    { label: '订阅审批', route: '/subscribes' }
  ]
  return props.isAdmin
    ? [...base, { label: '调用统计', route: '/stats' }, { label: 'API 日志', route: '/logs' }, { label: '限流配置', route: '/ratelimit' }]
    : base
})

function go(route: string) {
  router.push(route)
}
</script>

<template>
  <el-card class="quick-access-card" shadow="never">
    <div class="qa-body">
      <div class="qa-head">
        <h3 class="qa-title">快速使用</h3>
        <p class="qa-subtitle">三步完成 API 接入</p>
      </div>

      <ol class="qa-steps">
        <li v-for="(guide, index) in guideSteps" :key="index" class="qa-step">
          <span class="qa-index">{{ index + 1 }}</span>
          <span class="qa-text">{{ guide.text }}</span>
          <el-button size="small" type="primary" plain @click="go(guide.route)">
            {{ guide.action }}
          </el-button>
        </li>
      </ol>

      <div class="qa-links">
        <span class="qa-links-label">快捷入口</span>
        <div class="qa-links-list">
          <button
            v-for="link in links"
            :key="link.route"
            type="button"
            class="qa-link"
            @click="go(link.route)"
          >
            {{ link.label }}
          </button>
        </div>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.quick-access-card {
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  background: var(--app-surface);
}
.quick-access-card :deep(.el-card__body) {
  padding: 14px 20px;
}
.qa-body {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px 24px;
}
.qa-head {
  flex: 0 0 auto;
  min-width: 120px;
}
.qa-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text);
}
.qa-subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--app-muted);
}
.qa-steps {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  flex: 1;
  min-width: 320px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.qa-step {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 8px 12px;
  border: 1px solid var(--app-border);
  border-radius: 10px;
  background: #fbfcfe;
}
.qa-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  flex: none;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: var(--app-primary);
}
.qa-text {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  white-space: nowrap;
  color: var(--app-text);
}
.qa-links {
  flex: 0 0 auto;
}
.qa-links-label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--app-muted);
}
.qa-links-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 14px;
}
.qa-link {
  padding: 4px 2px;
  border: 0;
  background: transparent;
  color: var(--el-color-primary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
}
.qa-link:hover {
  text-decoration: underline;
}
.qa-link:focus-visible {
  outline: 2px solid var(--app-primary);
  outline-offset: 2px;
  border-radius: 4px;
}

@media (max-width: 900px) {
  .qa-links {
    width: 100%;
  }
}
</style>
