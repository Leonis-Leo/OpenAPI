<script setup lang="ts">
import { ref } from 'vue'
import { ArrowDown, ArrowUp, Search } from '@element-plus/icons-vue'

const emit = defineEmits<{ (e: 'search'): void; (e: 'reset'): void }>()
const expanded = ref(false)
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
        <el-button v-if="$slots.more" text class="cf-more" @click="expanded = !expanded">
          {{ expanded ? '收起' : '更多筛选' }}
          <el-icon class="el-icon--right"><ArrowUp v-if="expanded" /><ArrowDown v-else /></el-icon>
        </el-button>
        <el-button plain @click="emit('reset')">重置</el-button>
        <el-button type="primary" @click="emit('search')">
          <el-icon><Search /></el-icon>查询
        </el-button>
      </div>
    </div>
    <div v-if="$slots.chips" class="cf-chips">
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
  border-top: 1px dashed var(--app-border);
}
</style>
