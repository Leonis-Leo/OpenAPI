<template>
  <div>
    <h2>限流配置</h2>
    <p class="tip">
      为接口配置令牌桶限流（容量 + 每秒补充速率）。启用后网关按接口限流；
      未配置的接口使用全局按应用限流（容量 20 / 每秒 5）。
    </p>
    <el-card class="global-card">
      <div class="global-row">
        <span class="global-title">全局限流（按应用）</span>
        <span class="label">容量</span>
        <el-input-number v-model="global.capacity" :min="1" :max="10000" />
        <span class="label">补充速率(个/秒)</span>
        <el-input-number v-model="global.refillRate" :min="1" :max="1000" />
        <el-button type="primary" :loading="globalSaving" @click="handleSaveGlobal">保存全局配置</el-button>
      </div>
    </el-card>
    <el-table :data="list" border stripe>
      <el-table-column prop="interfaceId" label="ID" width="70" />
      <el-table-column prop="interfaceName" label="接口名称" />
      <el-table-column prop="method" label="方式" width="80">
        <template #default="{ row }">
          <el-tag :type="row.method === 'GET' ? 'success' : 'warning'">{{ row.method }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="url" label="路径" min-width="180" />
      <el-table-column label="配置状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.configured" type="success" size="small">已配置</el-tag>
          <el-tag v-else type="info" size="small">未配置</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="90">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" />
        </template>
      </el-table-column>
      <el-table-column label="容量" width="140">
        <template #default="{ row }">
          <el-input-number v-model="row.capacity" :min="1" :max="10000" />
        </template>
      </el-table-column>
      <el-table-column label="补充速率(个/秒)" width="160">
        <template #default="{ row }">
          <el-input-number v-model="row.refillRate" :min="1" :max="1000" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="handleSave(row)">保存</el-button>
          <el-button size="small" @click="handleDelete(row)">删除配置</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listRateLimitConfigs,
  saveRateLimitConfig,
  deleteRateLimitConfig,
  getGlobalRateLimit,
  saveGlobalRateLimit,
  type RateLimitConfig
} from '@/api'

const list = ref<RateLimitConfig[]>([])
const global = ref({ capacity: 20, refillRate: 5 })
const globalSaving = ref(false)

async function load() {
  list.value = await listRateLimitConfigs()
  global.value = await getGlobalRateLimit()
}

async function handleSave(row: RateLimitConfig) {
  await saveRateLimitConfig({
    interfaceId: row.interfaceId,
    capacity: row.capacity,
    refillRate: row.refillRate,
    enabled: row.enabled
  })
  ElMessage.success('已保存')
  await load()
}

async function handleDelete(row: RateLimitConfig) {
  await ElMessageBox.confirm(`确定删除「${row.interfaceName}」的限流配置吗？（恢复全局限流）`, '删除配置', {
    type: 'warning'
  })
  await deleteRateLimitConfig(row.interfaceId)
  ElMessage.success('已删除')
  await load()
}

async function handleSaveGlobal() {
  globalSaving.value = true
  try {
    await saveGlobalRateLimit(global.value.capacity, global.value.refillRate)
    ElMessage.success('全局配置已保存')
  } finally {
    globalSaving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.tip {
  color: #909399;
  font-size: 13px;
}
.global-card {
  margin-bottom: 14px;
}
.global-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.global-title {
  font-weight: 600;
}
.label {
  color: #909399;
  font-size: 13px;
}
</style>
