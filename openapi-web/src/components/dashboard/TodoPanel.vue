<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Stamp } from '@element-plus/icons-vue'
import type { SubscribeInfo } from '@/api'

const props = defineProps<{
  pending: SubscribeInfo[]
  unreadNotifications: number
  loading?: boolean
}>()

const router = useRouter()
</script>

<template>
  <el-card class="todo-card">
    <template #header>
      <div class="todo-header">
        <span class="todo-title">待办事项</span>
        <el-tag v-if="pending.length" type="warning" size="small">
          {{ pending.length }} 条待审批
        </el-tag>
      </div>
    </template>

    <div v-if="loading" class="todo-skeleton">
      <div v-for="i in 3" :key="i" class="skel-row" />
    </div>
    <el-empty
      v-else-if="!pending.length && !unreadNotifications"
      description="暂无待办，一切顺利"
      :image-size="56"
    />
    <ul v-else class="todo-list">
      <li v-for="item in pending.slice(0, 4)" :key="item.id" class="todo-item">
        <span class="todo-icon"><el-icon><Stamp /></el-icon></span>
        <button type="button" class="todo-main" @click="router.push('/subscribes')">
          <span class="todo-name">{{ item.interfaceName }}</span>
          <span class="todo-meta">{{ item.appName }} · {{ item.userAccount }} · {{ item.createTime }}</span>
        </button>
        <el-button size="small" type="warning" plain @click="router.push('/subscribes')">处理</el-button>
      </li>
      <li v-if="!pending.length && unreadNotifications" class="todo-item">
        <span class="todo-icon"><el-icon><Stamp /></el-icon></span>
        <button type="button" class="todo-main" @click="router.push('/notifications')">
          <span class="todo-name">有 {{ unreadNotifications }} 条未读通知</span>
          <span class="todo-meta">点击查看通知中心</span>
        </button>
        <el-button size="small" type="primary" plain @click="router.push('/notifications')">查看</el-button>
      </li>
    </ul>
    <button v-if="pending.length" type="button" class="todo-footer" @click="router.push('/subscribes')">
      进入订阅审批
      <span aria-hidden="true">→</span>
    </button>
  </el-card>
</template>

<style scoped>
.todo-card {
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  background: var(--app-surface);
}
.todo-card :deep(.el-card__header) {
  padding: 16px 18px 12px;
  border-bottom-color: var(--app-border);
}
.todo-card :deep(.el-card__body) {
  padding: 12px 18px 16px;
}
.todo-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.todo-title {
  font-weight: 600;
  color: var(--app-text);
}
.todo-list {
  margin: 0;
  padding: 0;
  list-style: none;
}
.todo-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--app-border);
}
.todo-item:last-child {
  border-bottom: 0;
}
.todo-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex: none;
  border-radius: 8px;
  color: #b45309;
  background: #fffbeb;
}
.todo-main {
  flex: 1;
  min-width: 0;
  padding: 2px 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  text-align: left;
}
.todo-main:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
  border-radius: 4px;
}
.todo-name {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text);
}
.todo-meta {
  display: block;
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--app-muted);
}
.todo-footer {
  display: block;
  width: 100%;
  margin-top: 12px;
  padding: 8px;
  border: 0;
  border-radius: 8px;
  background: #f8fafc;
  color: var(--app-primary);
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
}
.todo-footer:hover {
  background: #eff6ff;
}
.todo-footer:focus-visible {
  outline: 2px solid var(--app-primary);
  outline-offset: 2px;
}
.todo-skeleton {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.skel-row {
  height: 40px;
  border-radius: 6px;
  background: var(--el-fill-color);
}
</style>
