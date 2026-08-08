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
  background: #f7f9fc;
  position: relative;
  overflow: hidden;
}
.login-bg {
  position: absolute;
  inset: 0;
  background: #172b4d;
  opacity: 1;
  clip-path: polygon(0 0, 57% 0, 47% 100%, 0 100%);
}
.login-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 28% 36%, rgba(37, 99, 235, .45), transparent 30%), linear-gradient(135deg, transparent 45%, rgba(255,255,255,.04) 45%, transparent 46%);
}
.login-wrap {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 96px;
  width: min(960px, calc(100% - 40px));
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
  background: #2563eb;
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}
.brand h1 {
  margin: 0 0 10px;
  font-size: 32px;
  letter-spacing: -.03em;
}
.brand p {
  margin: 0;
  opacity: 0.85;
  font-size: 14px;
}
.login-card {
  width: 400px;
  padding: 24px 20px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 24px 60px rgba(15, 23, 42, .12);
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
@media (max-width: 760px) {
  .login-bg { clip-path: polygon(0 0, 100% 0, 100% 38%, 0 52%); }
  .login-wrap { flex-direction: column; gap: 24px; width: min(400px, calc(100% - 32px)); }
  .brand { text-align: center; }.brand-logo { margin: 0 auto 12px; }.brand h1 { font-size: 24px; }.brand p { display: none; }
  .login-card { width: 100%; box-sizing: border-box; }
}
</style>
