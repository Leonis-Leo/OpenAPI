<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="title">OpenAPI 开放平台</h2>
      <p class="subtitle">管理后台</p>
      <el-form :model="form" label-position="top">
        <el-form-item label="账号">
          <el-input v-model="form.userAccount" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.userPassword"
            type="password"
            placeholder="请输入密码"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({
  userAccount: 'admin',
  userPassword: '123456'
})

async function handleLogin() {
  if (!form.userAccount.trim() || !form.userPassword) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login(form.userAccount.trim(), form.userPassword)
    router.push('/')
  } catch {
    // 错误提示已在请求拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}
.login-card {
  width: 380px;
  padding: 12px 8px;
}
.title {
  text-align: center;
  margin: 0 0 4px;
}
.subtitle {
  text-align: center;
  color: #909399;
  margin: 0 0 24px;
}
.submit {
  width: 100%;
}
</style>
