<template>
  <div>
    <h2>接口管理</h2>
    <el-table :data="interfaces" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="description" label="描述" min-width="160" />
      <el-table-column prop="method" label="方式" width="90">
        <template #default="{ row }">
          <el-tag :type="row.method === 'GET' ? 'success' : 'warning'">
            {{ row.method }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="url" label="路径" min-width="180" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '上线' : '下线' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listInterfaces, type InterfaceInfo } from '@/api'

const interfaces = ref<InterfaceInfo[]>([])

onMounted(async () => {
  interfaces.value = await listInterfaces()
})
</script>
