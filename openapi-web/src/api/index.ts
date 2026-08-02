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
  requestParams?: string
  responseExample?: string
}

export interface SubscribeInfo {
  id: number
  interfaceId: number
  interfaceName: string
  interfaceUrl: string
  appId: number
  appName: string
  userId?: number
  userAccount?: string
  status: number
  createTime: string
}

export const login = (params: LoginParams) =>
  request.post<unknown, LoginResult>('/user/login', null, { params })

export const createApp = (appName: string, userId: number) =>
  request.post<unknown, AppInfo>('/app/create', null, { params: { appName, userId } })

export const listApps = (userId: number) =>
  request.get<unknown, AppInfo[]>('/app/list', { params: { userId } })

export const updateAppName = (id: number, appName: string) =>
  request.post<unknown, void>('/app/update', null, { params: { id, appName } })

export const resetAppSecret = (id: number) =>
  request.post<unknown, AppInfo>('/app/reset-secret', null, { params: { id } })

export const updateAppStatus = (id: number, enabled: boolean) =>
  request.post<unknown, void>('/app/update-status', null, { params: { id, enabled } })

export const deleteApp = (id: number) =>
  request.post<unknown, void>('/app/delete', null, { params: { id } })

export const listInterfaces = () =>
  request.get<unknown, InterfaceInfo[]>('/interface/list')

export const interfaceDetail = (id: number) =>
  request.get<unknown, InterfaceInfo>(`/interface/${id}`)

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

export const deleteSubscribeRecord = (id: number) =>
  request.post<unknown, void>('/interface/subscribe-delete', null, { params: { id } })

export interface InterfaceForm {
  name: string
  description: string
  method: string
  url: string
  requestParams?: string
  responseExample?: string
}

export const createInterface = (data: InterfaceForm) =>
  request.post<unknown, InterfaceInfo>('/interface/create', null, { params: data })

export const updateInterface = (id: number, data: Partial<InterfaceForm>) =>
  request.post<unknown, void>('/interface/update', null, { params: { id, ...data } })

export const deleteInterface = (id: number) =>
  request.post<unknown, void>('/interface/delete', null, { params: { id } })

export interface StatsOverview {
  total: number
  success: number
  fail: number
  successRate: number
}

export interface DailyStat {
  day: string
  total: number
  ok: number
}

export const statsOverview = () =>
  request.get<unknown, StatsOverview>('/stats/overview')

export const statsDaily = (days: number) =>
  request.get<unknown, DailyStat[]>('/stats/daily', { params: { days } })

export interface TopStat {
  total: number
  ok: number
  interfaceId?: number
  interfaceName?: string
  appId?: number
  appName?: string
}

export const statsTopInterfaces = (limit = 10) =>
  request.get<unknown, TopStat[]>('/stats/top-interfaces', { params: { limit } })

export const statsTopApps = (limit = 10) =>
  request.get<unknown, TopStat[]>('/stats/top-apps', { params: { limit } })

export interface UserInfo {
  id: number
  userAccount: string
  userName: string
  userRole: string
  status: number
  createTime: string
}

export const listUsers = (keyword?: string) =>
  request.get<unknown, UserInfo[]>('/user/list', { params: { keyword } })

export const updateUserRole = (id: number, role: string) =>
  request.post<unknown, void>('/user/update-role', null, { params: { id, role } })

export const updateUserStatus = (id: number, enabled: boolean) =>
  request.post<unknown, void>('/user/update-status', null, { params: { id, enabled } })

export const createUser = (data: {
  userAccount: string
  userPassword: string
  userName?: string
  role: string
}) => request.post<unknown, UserInfo>('/user/create', null, { params: data })

export const updateUser = (id: number, data: { userName?: string; userPassword?: string; role?: string }) =>
  request.post<unknown, void>('/user/update', null, { params: { id, ...data } })

export const selfUpdate = (data: { userName?: string; userPassword?: string }) =>
  request.post<unknown, void>('/user/self-update', null, { params: data })

export const deleteUser = (id: number) =>
  request.post<unknown, void>('/user/delete', null, { params: { id } })

export interface RateLimitConfig {
  interfaceId: number
  interfaceName: string
  url: string
  method: string
  capacity: number
  refillRate: number
  enabled: boolean
  configured?: boolean
}

export const listRateLimitConfigs = () =>
  request.get<unknown, RateLimitConfig[]>('/ratelimit/list')

export const saveRateLimitConfig = (data: {
  interfaceId: number
  capacity: number
  refillRate: number
  enabled: boolean
}) => request.post<unknown, void>('/ratelimit/save', null, { params: data })

export const deleteRateLimitConfig = (interfaceId: number) =>
  request.post<unknown, void>('/ratelimit/delete', null, { params: { interfaceId } })

export interface AppRateLimitConfig {
  appId: number
  appName: string
  accessKey: string
  capacity: number
  refillRate: number
  enabled: boolean
  configured?: boolean
}

export const listAppRateLimitConfigs = () =>
  request.get<unknown, AppRateLimitConfig[]>('/ratelimit/apps')

export const saveAppRateLimitConfig = (data: {
  appId: number
  capacity: number
  refillRate: number
  enabled: boolean
}) => request.post<unknown, void>('/ratelimit/app/save', null, { params: data })

export const deleteAppRateLimitConfig = (appId: number) =>
  request.post<unknown, void>('/ratelimit/app/delete', null, { params: { appId } })

export interface ApiLog {
  id: number
  interfaceId: number
  interfaceName: string
  appId: number
  appName: string
  userId: number
  userAccount: string
  ip: string
  method: string
  path: string
  requestParams?: string
  responseBody?: string
  statusCode: number
  success: number
  costMs: number
  createTime: string
}

export interface ApiLogPage {
  records: ApiLog[]
  total: number
}

export const listApiLogs = (params: {
  current: number
  size: number
  keyword?: string
  success?: number
  statusCode?: number
  startTime?: string
  endTime?: string
}) =>
  request.get<unknown, ApiLogPage>('/log/list', { params })

export const getApiLog = (id: number) =>
  request.get<unknown, ApiLog>(`/log/${id}`)

export const deleteApiLog = (id: number) =>
  request.post<unknown, void>('/log/delete', null, { params: { id } })

export const deleteApiLogs = (ids: number[]) =>
  request.post<unknown, void>('/log/delete-batch', null, { params: { ids: ids.join(',') } })

export const clearApiLogs = () =>
  request.post<unknown, void>('/log/clear')
