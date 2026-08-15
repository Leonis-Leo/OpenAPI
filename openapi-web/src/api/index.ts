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
  secretKey?: string
  secretKeyHint: string
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
  groupId?: number
  groupName?: string
  tags?: { id: number; name: string; color: string }[]
  status: number
  requestParams?: string
  responseExample?: string
  upstream?: string
  timeoutMs?: number
  retryCount?: number
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

export const logout = () => request.post<unknown, void>('/user/logout')

export const register = (params: { userAccount: string; userPassword: string; userName?: string }) =>
  request.post<unknown, UserInfo>('/user/register', null, { params })

export const createApp = (appName: string) =>
  request.post<unknown, AppInfo>('/app/create', null, { params: { appName } })

export const listApps = () =>
  request.get<unknown, AppInfo[]>('/app/list')

export interface PageResult<T> {
  records: T[]
  total: number
}

export const pageApps = (params: { current: number; size: number; keyword?: string; status?: number }) =>
  request.get<unknown, PageResult<AppInfo>>('/app/page', { params })

export const listAppsForDebug = () =>
  request.get<unknown, AppInfo[]>('/app/debug-list')

export const listAppsForAdmin = (userId: number) =>
  request.get<unknown, AppInfo[]>('/app/admin-list', { params: { userId } })

export const revealAppSecret = (id: number) =>
  request.post<unknown, AppInfo>('/app/reveal-secret', null, { params: { id } })

export const updateAppName = (id: number, appName: string) =>
  request.post<unknown, void>('/app/update', null, { params: { id, appName } })

export const resetAppSecret = (id: number, currentPassword: string) =>
  request.post<unknown, AppInfo>('/app/reset-secret', null, { params: { id, currentPassword } })

export const updateAppStatus = (id: number, enabled: boolean) =>
  request.post<unknown, void>('/app/update-status', null, { params: { id, enabled } })

export const deleteApp = (id: number) =>
  request.post<unknown, void>('/app/delete', null, { params: { id } })

export const listInterfaces = () =>
  request.get<unknown, InterfaceInfo[]>('/interface/list')

export const pageInterfaces = (params: {
  current: number
  size: number
  keyword?: string
  status?: number
  groupId?: number
  ungrouped?: boolean
  method?: string
  tagId?: number
}) =>
  request.get<unknown, PageResult<InterfaceInfo>>('/interface/page', { params })

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

export const pageMySubscribes = (params: { current: number; size: number }) =>
  request.get<unknown, PageResult<SubscribeInfo>>('/interface/my-subscribes/page', { params })

export const listSubscribes = (status?: number) =>
  request.get<unknown, SubscribeInfo[]>('/interface/subscribes', { params: { status } })

export const pageSubscribes = (params: { current: number; size: number; status?: number }) =>
  request.get<unknown, PageResult<SubscribeInfo>>('/interface/subscribes/page', { params })

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
  upstream?: string
  timeoutMs?: number
  retryCount?: number
  groupId?: number
  tags?: number[]
}

export const createInterface = (data: InterfaceForm) =>
  request.post<unknown, InterfaceInfo>('/interface/create', null, {
    params: { ...data, tags: data.tags?.length ? data.tags.join(',') : undefined }
  })

export const updateInterface = (id: number, data: Partial<InterfaceForm>) =>
  request.post<unknown, void>('/interface/update', null, {
    params: { id, ...data, tags: data.tags?.length ? data.tags.join(',') : undefined }
  })

export const deleteInterface = (id: number) =>
  request.post<unknown, void>('/interface/delete', null, { params: { id } })

export interface InterfaceGroupInfo {
  id: number
  name: string
  parentId?: number
  parentName?: string
  sortOrder?: number
  interfaceCount?: number
  children?: InterfaceGroupInfo[]
}

export interface GroupTreeResult {
  tree: InterfaceGroupInfo[]
  total: number
  ungrouped: number
}

export interface InterfaceTagInfo {
  id: number
  name: string
  color?: string
  interfaceCount?: number
}

export const listInterfaceGroups = () =>
  request.get<unknown, GroupTreeResult>('/interface/groups')

export const createInterfaceGroup = (name: string, parentId?: number) =>
  request.post<unknown, void>('/interface/group/create', null, { params: { name, parentId } })

export const updateInterfaceGroup = (id: number, name: string) =>
  request.post<unknown, void>('/interface/group/update', null, { params: { id, name } })

export const deleteInterfaceGroup = (id: number) =>
  request.post<unknown, void>('/interface/group/delete', null, { params: { id } })

export const listInterfaceTags = () =>
  request.get<unknown, InterfaceTagInfo[]>('/interface/tags')

export const createInterfaceTag = (name: string) =>
  request.post<unknown, InterfaceTagInfo>('/interface/tag/create', null, { params: { name } })

export const deleteInterfaceTag = (id: number) =>
  request.post<unknown, void>('/interface/tag/delete', null, { params: { id } })

export const importOpenApi = (spec: string) =>
  request.post<unknown, { created: number; skipped: number }>('/interface/openapi/import', null, {
    params: { spec }
  })

export const exportOpenApi = (format: 'json' | 'yaml') =>
  request.get<unknown, string>('/interface/openapi/export', { params: { format } })

export interface InterfaceVersionInfo {
  id: number
  interfaceId: number
  versionNo: number
  name: string
  description?: string
  method: string
  url: string
  requestParams?: string
  responseExample?: string
  status: number
  changeNote?: string
  createBy?: number
  createTime: string
}

export const listInterfaceVersions = (interfaceId: number) =>
  request.get<unknown, InterfaceVersionInfo[]>('/interface/versions', { params: { interfaceId } })

export const rollbackInterface = (interfaceId: number, versionId: number) =>
  request.post<unknown, void>('/interface/rollback', null, { params: { interfaceId, versionId } })

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

export interface StatsDetailItem {
  day?: string
  appId?: number
  appName?: string
  interfaceId?: number
  interfaceName?: string
  total: number
  success: number
  fail: number
  successRate: number
  totalCostMs?: number
  avgCostMs?: number
}

export const statsDailyPage = (params: {
  current: number
  size: number
  dimension?: 'day' | 'app' | 'interface'
  startDate?: string
  endDate?: string
  appId?: number
  interfaceId?: number
}) =>
  request.get<unknown, PageResult<StatsDetailItem>>('/stats/daily-page', { params })

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

export const pageUsers = (params: {
  current: number
  size: number
  keyword?: string
  role?: string
  status?: number
}) =>
  request.get<unknown, PageResult<UserInfo>>('/user/page', { params })

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
  requestHeaders?: string
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
  appId?: number
  interfaceId?: number
  method?: string
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

export interface NotificationItem {
  id: number
  userId: number
  type: string
  title: string
  content?: string
  bizId?: number
  link?: string
  isRead: number
  createTime: string
}

export const pageNotifications = (params: { current: number; size: number; read?: number }) =>
  request.get<unknown, PageResult<NotificationItem>>('/notification/page', { params, skipNetworkRedirect: true })

export const unreadNotificationCount = () =>
  request.get<unknown, number>('/notification/unread-count', { skipNetworkRedirect: true })

export const readNotification = (id: number) =>
  request.post<unknown, void>('/notification/read', null, { params: { id } })

export const readAllNotifications = () =>
  request.post<unknown, void>('/notification/read-all')

export const deleteNotification = (id: number) =>
  request.post<unknown, void>('/notification/delete', null, { params: { id } })

export const clearNotifications = () =>
  request.post<unknown, void>('/notification/clear')

export interface AuditLogInfo {
  id: number
  userId?: number
  userAccount?: string
  action: string
  resource: string
  ip?: string
  statusCode?: number
  success: number
  detail?: string
  createTime: string
}

export const pageAuditLogs = (params: {
  current: number
  size: number
  keyword?: string
  userId?: number
  action?: string
  resource?: string
  success?: number
  startTime?: string
  endTime?: string
}) =>
  request.get<unknown, PageResult<AuditLogInfo>>('/audit/list', { params })

export const getAuditLog = (id: number) =>
  request.get<unknown, AuditLogInfo>(`/audit/${id}`)

export const exportAuditLogs = (params: {
  keyword?: string
  userId?: number
  action?: string
  resource?: string
  success?: number
  startTime?: string
  endTime?: string
}) =>
  request.get<unknown, Blob>('/audit/export', { params, responseType: 'blob' })
