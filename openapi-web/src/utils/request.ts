import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { AxiosResponse, AxiosRequestConfig } from 'axios'

declare module 'axios' {
  export interface AxiosRequestConfig {
    /** 后台轮询等静默请求：网络异常时不跳转异常页、不弹提示 */
    skipNetworkRedirect?: boolean
  }
}

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

const request = axios.create({
  baseURL: '/v1',
  timeout: 10000,
  withCredentials: true
})

let networkErrorRedirecting = false
let lastErrorToast = ''
let lastErrorToastTime = 0

function redirectToLogin() {
  localStorage.removeItem('openapi_user')
  window.location.href = '/login'
}

function showErrorOnce(message: string) {
  const now = Date.now()
  if (message && message === lastErrorToast && now - lastErrorToastTime < 3000) return
  lastErrorToast = message
  lastErrorToastTime = now
  ElMessage.error(message)
}

function redirectToNetworkError() {
  if (networkErrorRedirecting) return
  if (window.location.pathname === '/network-error') return
  networkErrorRedirecting = true
  window.location.assign('/network-error')
}

request.interceptors.request.use((config) => {
  const match = document.cookie.match(/(?:^|; )openapi_csrf=([^;]*)/)
  const method = (config.method || 'get').toLowerCase()
  if (match && !['get', 'head', 'options'].includes(method)) {
    config.headers['X-CSRF-Token'] = decodeURIComponent(match[1])
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response.data as AxiosResponse
    }
    const res = response.data as ApiResponse<unknown>
    if (res.code !== 0) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 40105) {
        redirectToLogin()
      }
      return Promise.reject(new Error(res.message))
    }
    return res.data as AxiosResponse
  },
  (error) => {
    const config = error.config as (AxiosRequestConfig & { skipNetworkRedirect?: boolean }) | undefined
    const rawData = error.response?.data
    // 后端不可达 / 代理返回非 JSON 错误体（如网关 HTML）→ 视为网络异常
    const proxyError = typeof rawData === 'string' && /^\s*</.test(rawData)
    const isNetworkError =
      (!error.response &&
        (error.code === 'ERR_NETWORK' ||
          error.code === 'ECONNABORTED' ||
          error.message === 'Network Error' ||
          error.message?.toLowerCase().includes('timeout'))) ||
      proxyError
    if (isNetworkError) {
      if (!config?.skipNetworkRedirect) {
        redirectToNetworkError()
      }
      return Promise.reject(error)
    }
    if (error.response?.status === 401) {
      redirectToLogin()
    }
    showErrorOnce(
      error.response?.data?.message ||
        (error.response?.status ? `请求失败（${error.response.status}）` : '网络异常')
    )
    return Promise.reject(error)
  }
)

export default request
