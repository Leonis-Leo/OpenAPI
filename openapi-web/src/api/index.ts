import request from '@/utils/request'

export interface LoginParams {
  userAccount: string
  userPassword: string
}

export interface UserInfo {
  id: number
  userAccount: string
  userName: string
  userRole: string
}

export interface LoginResult {
  token: string
  user: UserInfo
}

export interface AppInfo {
  id: number
  appName: string
  accessKey: string
  secretKey: string
  userId: number
  status: number
  createTime: string
}

export interface InterfaceInfo {
  id: number
  name: string
  description: string
  method: string
  url: string
  status: number
}

export const login = (params: LoginParams) =>
  request.post<unknown, LoginResult>('/user/login', null, { params })

export const createApp = (appName: string, userId: number) =>
  request.post<unknown, AppInfo>('/app/create', null, { params: { appName, userId } })

export const listApps = (userId: number) =>
  request.get<unknown, AppInfo[]>('/app/list', { params: { userId } })

export const listInterfaces = () =>
  request.get<unknown, InterfaceInfo[]>('/interface/list')
