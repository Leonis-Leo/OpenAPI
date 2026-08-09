<script setup lang="ts">
import { Comment, computed, ref, useSlots } from 'vue'
import { ArrowDown, ArrowUp, Search } from '@element-plus/icons-vue'

const emit = defineEmits<{ (e: 'search'): void; (e: 'reset'): void }>()
const expanded = ref(false)
const slots = useSlots()
const hasChips = computed(() =>
  (slots.chips?.() ?? []).some((node) => node.type !== Comment)
)
</script>

<template>
  <div class="cf">
    <div class="cf-main">
      <div class="cf-fields">
        <slot />
        <template v-if="expanded">
          <slot name="more" />
        </template>
      </div>
      <div class="cf-actions">
        <el-button v-if="$slots.more" size="small" text class="cf-more" @click="expanded = !expanded">
          {{ expanded ? '收起' : '更多筛选' }}
          <el-icon class="el-icon--right"><ArrowUp v-if="expanded" /><ArrowDown v-else /></el-icon>
        </el-button>
        <el-button size="small" plain @click="emit('reset')">重置</el-button>
        <el-button size="small" type="primary" @click="emit('search')">
          <el-icon><Search /></el-icon>查询
        </el-button>
      </div>
    </div>
    <div v-if="hasChips" class="cf-chips">
      <slot name="chips" />
    </div>
  </div>
</template>

<style scoped>
.cf {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
  padding: 12px 14px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  background: var(--app-surface);
  box-shadow: 0 1px 3px rgba(16, 24, 40, .04);
}
.cf-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.cf-fields {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
  min-width: 0;
}
.cf-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: none;
}
.cf-more {
  height: 28px;
  padding: 0 6px;
}
.cf-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 10px;
}
</style>
