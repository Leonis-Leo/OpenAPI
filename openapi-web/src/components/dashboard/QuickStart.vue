<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Box,
  CircleCheck,
  Clock,
  Connection,
  Key,
  Link,
  Stamp,
  TrendCharts
} from '@element-plus/icons-vue'

const props = defineProps<{
  appCount: number
  interfaceCount: number
  subscribeCount: number
  pendingCount: number
  isAdmin: boolean
  loading?: boolean
}>()

const router = useRouter()

type StepState = 'done' | 'pending' | 'todo'

interface LifecycleStep {
  key: string
  icon: unknown
  title: string
  desc: string
  route: string
  state: StepState
  badge: string
  badgeType: 'success' | 'warning' | 'info'
  adminOnly?: boolean
}

const steps = computed<LifecycleStep[]>(() => {
  const admin = props.isAdmin
  return [
    {
      key: 'app',
      icon: Box,
      title: '创建应用',
      desc: '获得 AccessKey / SecretKey，标识你的调用方身份',
      route: '/apps',
      state: props.appCount > 0 ? 'done' : 'todo',
      badge: props.appCount > 0 ? `${props.appCount} 个应用` : '',
      badgeType: 'success'
    },
    {
      key: 'publish',
      icon: Connection,
      title: admin ? '发布接口' : '浏览接口',
      desc: admin ? '创建接口并上线，向订阅者开放能力' : '查看已上线的开放接口',
      route: '/interfaces',
      state: props.interfaceCount > 0 ? 'done' : 'todo',
      badge: props.interfaceCount > 0 ? `${props.interfaceCount} 个接口` : '',
      badgeType: 'success'
    },
    {
      key: 'subscribe',
      icon: Link,
      title: '订阅申请',
      desc: '选择应用，申请订阅所需接口',
      route: '/interfaces',
      state: props.subscribeCount > 0 ? 'done' : 'todo',
      badge: props.subscribeCount > 0 ? `${props.subscribeCount} 个已订阅` : '',
      badgeType: 'success'
    },
    {
      key: 'approve',
      icon: Stamp,
      title: admin ? '审批通过' : '等待审批',
      desc: admin ? '审批订阅申请，通过后调用方即可使用' : '管理员审批通过后即可发起调用',
      route: '/subscribes',
      state: props.pendingCount > 0 ? 'pending' : props.subscribeCount > 0 ? 'done' : 'todo',
      badge: props.pendingCount > 0 ? `${props.pendingCount} 条待审批` : props.subscribeCount > 0 ? '审批已完成' : '',
      badgeType: props.pendingCount > 0 ? 'warning' : 'success'
    },
    {
      key: 'invoke',
      icon: Key,
      title: '签名调用',
      desc: 'HMAC-SHA256 签名鉴权，安全发起 API 调用',
      route: '/interfaces',
      state: 'todo',
      badge: '在线调试',
      badgeType: 'info'
    },
    {
      key: 'ops',
      icon: TrendCharts,
      title: '运营分析',
      desc: admin ? '调用统计、API 日志与限流治理' : '管理员专属：调用统计与 API 日志',
      route: '/stats',
      state: 'todo',
      badge: admin ? '查看数据' : '管理员',
      badgeType: 'info',
      adminOnly: !admin
    }
  ]
})

const stateIcon = (state: StepState) =>
  state === 'done' ? CircleCheck : state === 'pending' ? Clock : null

function go(route: string) {
  router.push(route)
}
</script>

<template>
  <el-card class="lifecycle-card" shadow="never">
    <template #header>
      <div class="lifecycle-head">
        <div>
          <h3 class="lifecycle-title">API 生命周期</h3>
          <p class="lifecycle-subtitle">从创建应用到运营分析，覆盖 API 开放与使用的完整流程</p>
        </div>
        <div v-if="loading" class="lifecycle-head-skel" aria-hidden="true" />
      </div>
    </template>

    <el-skeleton v-if="loading" animated>
      <template #template>
        <div class="flow-skel">
          <div v-for="i in 6" :key="i" class="flow-skel-item">
            <el-skeleton-item variant="circle" style="width: 44px; height: 44px" />
            <el-skeleton-item variant="h3" style="width: 72px; margin-top: 12px" />
            <el-skeleton-item variant="text" style="width: 100%; margin-top: 8px" />
            <el-skeleton-item variant="text" style="width: 70%; margin-top: 6px" />
          </div>
        </div>
      </template>
    </el-skeleton>

    <div v-else>
      <ol class="lifecycle-flow">
        <li
          v-for="(step, index) in steps"
          :key="step.key"
          class="lifecycle-step"
          :class="`state-${step.state}`"
        >
          <div class="step-node">
            <span class="step-icon">
              <el-icon><component :is="step.icon" /></el-icon>
              <span v-if="step.state !== 'todo'" class="step-state-icon">
                <el-icon><component :is="stateIcon(step.state)" /></el-icon>
              </span>
            </span>
            <span v-if="index < steps.length - 1" class="step-arrow" aria-hidden="true">
              <el-icon><ArrowRight /></el-icon>
            </span>
          </div>
          <div class="step-copy">
            <div class="step-title-row">
              <strong class="step-title">{{ step.title }}</strong>
              <el-tag v-if="step.badge" :type="step.badgeType" size="small" effect="light" class="step-badge">
                {{ step.badge }}
              </el-tag>
            </div>
            <p class="step-desc">{{ step.desc }}</p>
            <button
              v-if="step.adminOnly"
              type="button"
              class="step-link"
              :disabled="true"
              aria-disabled="true"
              title="仅管理员可访问"
            >
              仅管理员
            </button>
            <button v-else type="button" class="step-link" @click="go(step.route)">
              去操作
              <el-icon class="step-link-icon"><ArrowRight /></el-icon>
            </button>
          </div>
        </li>
      </ol>

    </div>
  </el-card>
</template>

<style scoped>
.lifecycle-card {
  --flow-gap: 18px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  background: var(--app-surface);
}
.lifecycle-card :deep(.el-card__header) {
  padding: 18px 22px 14px;
  border-bottom-color: var(--app-border);
}
.lifecycle-card :deep(.el-card__body) {
  padding: 20px 22px 22px;
}
.lifecycle-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.lifecycle-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text);
}
.lifecycle-subtitle {
  margin: 5px 0 0;
  font-size: 12px;
  color: var(--app-muted);
}
.lifecycle-head-skel {
  width: 120px;
  height: 28px;
  border-radius: 8px;
  background: var(--el-fill-color-light, #f5f7fa);
  animation: skel-pulse 1.4s ease-in-out infinite;
}
.flow-skel {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: var(--flow-gap);
}
.flow-skel-item {
  padding: 4px 8px;
}
@keyframes skel-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .45; }
}

.lifecycle-flow {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: var(--flow-gap);
  margin: 0;
  padding: 0;
  list-style: none;
}
.lifecycle-step {
  position: relative;
  min-width: 0;
  padding: 4px 8px;
}
.step-node {
  position: relative;
  display: inline-flex;
  align-items: center;
}
.step-icon {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  font-size: 20px;
  color: var(--app-muted);
  background: #f1f5f9;
  transition: color .18s ease, background-color .18s ease, transform .18s ease;
}
.lifecycle-step.state-done .step-icon {
  color: #047857;
  background: #ecfdf5;
}
.lifecycle-step.state-pending .step-icon {
  color: #b45309;
  background: #fffbeb;
}
.lifecycle-step:hover .step-icon {
  transform: translateY(-1px);
}
.step-state-icon {
  position: absolute;
  right: -5px;
  bottom: -5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  font-size: 12px;
  color: #fff;
  background: #10b981;
  border: 2px solid var(--app-surface);
}
.lifecycle-step.state-pending .step-state-icon {
  background: #f59e0b;
}
.step-arrow {
  display: none;
  align-items: center;
  margin: 0 6px;
  font-size: 14px;
  color: #cbd5e1;
}
.step-copy {
  margin-top: 12px;
}
.step-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.step-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text);
}
.step-badge {
  border-radius: 6px;
  font-weight: 600;
}
.step-desc {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--app-muted);
}
.step-link {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  margin-top: 8px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--app-primary);
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
}
.step-link-icon {
  font-size: 12px;
  transition: transform .16s ease;
}
.step-link:hover .step-link-icon {
  transform: translateX(2px);
}
.step-link:disabled {
  color: var(--app-muted);
  cursor: not-allowed;
}
.step-link:focus-visible {
  outline: 2px solid var(--app-primary);
  outline-offset: 3px;
  border-radius: 4px;
}

@media (min-width: 1200px) and (max-width: 1399px) {
  .lifecycle-flow {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (min-width: 1400px) {
  .lifecycle-flow {
    grid-template-columns: repeat(6, 1fr);
  }
  .step-arrow {
    display: inline-flex;
  }
}
@media (max-width: 760px) {
  .lifecycle-card :deep(.el-card__body) {
    padding: 16px;
  }
  .quick-step {
    flex: 1 1 100%;
  }
}
</style>
