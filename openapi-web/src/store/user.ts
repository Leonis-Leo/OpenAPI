import { defineStore } from 'pinia'
import { login as loginApi, type LoginResult } from '@/api'

export interface UserInfo {
  id: number
  userAccount: string
  userName: string
  userRole: string
}

function readUser(): UserInfo | null {
  try {
    return JSON.parse(localStorage.getItem('openapi_user') || 'null')
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('openapi_token') || '',
    user: readUser()
  }),
  actions: {
    async login(userAccount: string, userPassword: string) {
      const result: LoginResult = await loginApi({ userAccount, userPassword })
      this.token = result.token
      this.user = result.user
      localStorage.setItem('openapi_token', result.token)
      localStorage.setItem('openapi_user', JSON.stringify(result.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('openapi_token')
      localStorage.removeItem('openapi_user')
    }
  }
})
