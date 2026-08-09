<script setup lang="ts">
import { useRouter } from 'vue-router'
import { readNotification, type NotificationItem } from '@/api'

const props = defineProps<{
  notifications: NotificationItem[]
  loading?: boolean
}>()

const router = useRouter()

async function openItem(item: NotificationItem) {
  if (item.isRead === 0) {
    try {
      await readNotification(item.id)
      item.isRead = 1
    } catch {
      // 标记失败不阻塞跳转
    }
  }
  if (item.link) {
    router.push(item.link)
  }
}
</script>

<template>
  <el-card class="notify-card">
    <template #header>
      <div class="notify-header">
        <span class="notify-title">最新通知</span>
        <button type="button" class="notify-more" @click="router.push('/notifications')">查看全部</button>
      </div>
    </template>

    <div v-if="loading" class="notify-skeleton">
      <div v-for="i in 3" :key="i" class="skel-row" />
    </div>
    <el-empty v-else-if="!notifications.length" description="暂无通知" :image-size="56" />
    <ul v-else class="notify-list">
      <li
        v-for="item in notifications"
        :key="item.id"
        class="notify-item"
        :class="{ unread: item.isRead === 0 }"
      >
        <button type="button" class="notify-main" @click="openItem(item)">
          <span v-if="item.isRead === 0" class="notify-dot" aria-hidden="true"></span>
          <span class="notify-copy">
            <span class="notify-name">{{ item.title }}</span>
            <span class="notify-content">{{ item.content }}</span>
            <span class="notify-time">{{ item.createTime }}</span>
          </span>
        </button>
      </li>
    </ul>
  </el-card>
</template>

<style scoped>
.notify-card {
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  background: var(--app-surface);
}
.notify-card :deep(.el-card__header) {
  padding: 16px 18px 12px;
  border-bottom-color: var(--app-border);
}
.notify-card :deep(.el-card__body) {
  padding: 12px 18px 16px;
}
.notify-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.notify-title {
  font-weight: 600;
  color: var(--app-text);
}
.notify-more {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--app-primary);
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
}
.notify-more:hover {
  text-decoration: underline;
}
.notify-more:focus-visible {
  outline: 2px solid var(--app-primary);
  outline-offset: 2px;
  border-radius: 4px;
}
.notify-list {
  margin: 0;
  padding: 0;
  list-style: none;
}
.notify-item + .notify-item {
  border-top: 1px solid var(--app-border);
}
.notify-main {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  width: 100%;
  padding: 9px 4px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  text-align: left;
}
.notify-main:hover {
  background: var(--el-fill-color-light, #f5f7fa);
}
.notify-main:focus-visible {
  outline: 2px solid var(--app-primary);
  outline-offset: 1px;
}
.notify-item.unread .notify-main {
  background: #eff6ff;
}
.notify-item.unread .notify-main:hover {
  background: #dbeafe;
}
.notify-dot {
  width: 7px;
  height: 7px;
  flex: none;
  margin-top: 6px;
  border-radius: 50%;
  background: #2563eb;
}
.notify-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.notify-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text);
}
.notify-content {
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--app-muted);
}
.notify-time {
  margin-top: 4px;
  font-size: 11px;
  color: var(--app-muted);
}
.notify-skeleton {
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
