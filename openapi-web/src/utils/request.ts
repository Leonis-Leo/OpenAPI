import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { AxiosResponse } from 'axios'

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

function redirectToLogin() {
  localStorage.removeItem('openapi_user')
  window.location.href = '/login'
}

request.interceptors.response.use(
  (response) => {
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
    if (error.response?.status === 401) {
      redirectToLogin()
    }
    ElMessage.error(error.response?.data?.message || '网络异常')
    return Promise.reject(error)
  }
)

export default request
