<template>
  <div class="login-page">
    <div class="login-bg"></div>
    <div class="login-wrap">
      <div class="brand">
        <div class="brand-logo">OP</div>
        <h1>OpenAPI 开放平台</h1>
        <p>接口开放 · 订阅审批 · 调用统计 · 限流防护</p>
      </div>
      <el-card class="login-card">
        <h2 class="title">{{ mode === 'login' ? '管理后台登录' : '注册账号' }}</h2>
        <el-form :model="form" label-position="top">
          <el-form-item label="账号">
            <el-input v-model="form.userAccount" placeholder="请输入账号" size="large" />
          </el-form-item>
          <el-form-item v-if="mode === 'register'" label="昵称">
            <el-input v-model="form.userName" placeholder="请输入昵称（选填）" size="large" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="form.userPassword"
              type="password"
              placeholder="请输入密码"
              show-password
              size="large"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item v-if="mode === 'register'" label="确认密码">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              show-password
              size="large"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-button type="primary" class="submit" size="large" :loading="loading" @click="handleLogin">
            {{ mode === 'login' ? '登 录' : '注 册' }}
          </el-button>
          <div class="switch-mode">
            <el-link type="primary" @click="switchMode">
              {{ mode === 'login' ? '没有账号？去注册' : '已有账号？去登录' }}
            </el-link>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { register as registerApi } from '@/api'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const mode = ref<'login' | 'register'>('login')
const form = reactive({
  userAccount: '',
  userPassword: '',
  userName: '',
  confirmPassword: ''
})

async function handleLogin() {
  if (mode.value === 'register') {
    await handleRegister()
    return
  }
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

async function handleRegister() {
  if (!form.userAccount.trim() || !form.userPassword) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  if (form.userPassword !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    await registerApi({
      userAccount: form.userAccount.trim(),
      userPassword: form.userPassword,
      userName: form.userName.trim() || undefined
    })
    ElMessage.success('注册成功，正在登录…')
    await userStore.login(form.userAccount.trim(), form.userPassword)
    router.push('/')
  } catch {
    // 错误提示已在请求拦截器处理
  } finally {
    loading.value = false
  }
}

function switchMode() {
  mode.value = mode.value === 'login' ? 'register' : 'login'
  form.userPassword = ''
  form.confirmPassword = ''
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-bg-color-page, #eef2f7);
  position: relative;
  overflow: hidden;
}
.login-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #304156 0%, #409eff 55%, #79bbff 100%);
  opacity: 0.92;
}
.login-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.18), transparent 40%);
}
.login-wrap {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 48px;
}
.brand {
  color: #fff;
  max-width: 320px;
}
.dark .brand {
  color: #fff;
}
.brand-logo {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  color: #409eff;
  font-size: 24px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}
.brand h1 {
  margin: 0 0 10px;
  font-size: 30px;
}
.brand p {
  margin: 0;
  opacity: 0.85;
  font-size: 14px;
}
.login-card {
  width: 400px;
  padding: 20px 12px;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
}
.title {
  text-align: center;
  margin: 0 0 24px;
}
.submit {
  width: 100%;
  margin-top: 8px;
}
.switch-mode {
  margin-top: 12px;
  text-align: center;
}
</style>
