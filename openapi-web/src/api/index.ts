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

export interface SubscribeInfo {
  id: number
  interfaceId: number
  interfaceName: string
  interfaceUrl: string
  appId: number
  appName: string
  status: number
  createTime: string
}

export const login = (params: LoginParams) =>
  request.post<unknown, LoginResult>('/user/login', null, { params })

export const createApp = (appName: string, userId: number) =>
  request.post<unknown, AppInfo>('/app/create', null, { params: { appName, userId } })

export const listApps = (userId: number) =>
  request.get<unknown, AppInfo[]>('/app/list', { params: { userId } })

export const listInterfaces = () =>
  request.get<unknown, InterfaceInfo[]>('/interface/list')

export const listAllInterfaces = () =>
  request.get<unknown, InterfaceInfo[]>('/interface/list-all')

export const onlineInterface = (id: number) =>
  request.post<unknown, void>('/interface/online', null, { params: { id } })

export const offlineInterface = (id: number) =>
  request.post<unknown, void>('/interface/offline', null, { params: { id } })

export const subscribe = (interfaceId: number, appId: number) =>
  request.post<unknown, SubscribeInfo>('/interface/subscribe', null, { params: { interfaceId, appId } })

export const mySubscribes = () =>
  request.get<unknown, SubscribeInfo[]>('/interface/my-subscribes')

export const listSubscribes = (status?: number) =>
  request.get<unknown, SubscribeInfo[]>('/interface/subscribes', { params: { status } })

export const approve = (id: number, approved: boolean) =>
  request.post<unknown, void>('/interface/approve', null, { params: { id, approved } })

export const unsubscribe = (id: number) =>
  request.post<unknown, void>('/interface/unsubscribe', null, { params: { id } })
