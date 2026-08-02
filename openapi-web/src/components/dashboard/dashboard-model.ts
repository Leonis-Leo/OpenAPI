export interface DashboardStatItem {
  key: string
  label: string
  value: number
  display: string
  route: string
  tone: 'primary' | 'success' | 'warning'
}

export interface OverviewSeries {
  days: string[]
  total: number[]
  ok: number[]
  fail: number[]
}

export interface DashboardInput {
  appCount: number
  subscribeCount: number
  interfaceCount: number
  total: number
  successRate: number
  pendingCount: number
  isAdmin: boolean
}

export function buildOverviewSeries(
  daily: { day: string; total: number; ok: number }[]
): OverviewSeries {
  return {
    days: daily.map((d) => d.day),
    total: daily.map((d) => d.total),
    ok: daily.map((d) => d.ok),
    fail: daily.map((d) => Math.max(0, d.total - d.ok))
  }
}

export function roundRate(ok: number, total: number): number {
  if (!total) return 0
  return Math.round((ok / total) * 100)
}

export function buildStatCards(input: DashboardInput): DashboardStatItem[] {
  const cards: DashboardStatItem[] = [
    { key: 'apps', label: '我的应用', value: input.appCount, display: String(input.appCount), route: '/apps', tone: 'primary' },
    { key: 'subscribes', label: '已订阅接口', value: input.subscribeCount, display: String(input.subscribeCount), route: '/subscribes', tone: 'primary' },
    { key: 'interfaces', label: '可调用接口', value: input.interfaceCount, display: String(input.interfaceCount), route: '/interfaces', tone: 'primary' }
  ]
  if (!input.isAdmin) return cards
  cards.push(
    { key: 'total', label: '累计调用', value: input.total, display: String(input.total), route: '/stats', tone: 'primary' },
    { key: 'rate', label: '成功率', value: input.successRate, display: `${input.successRate}%`, route: '/stats', tone: 'success' },
    { key: 'pending', label: '待审批订阅', value: input.pendingCount, display: String(input.pendingCount), route: '/subscribes', tone: 'warning' }
  )
  return cards
}
