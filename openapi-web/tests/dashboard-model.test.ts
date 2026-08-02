import { test } from 'node:test'
import assert from 'node:assert/strict'
import { buildOverviewSeries, roundRate, buildStatCards } from '../src/components/dashboard/dashboard-model.ts'

test('buildOverviewSeries computes fail = total - ok', () => {
  const s = buildOverviewSeries([
    { day: '2026-08-01', total: 10, ok: 7 },
    { day: '2026-08-02', total: 0, ok: 0 }
  ])
  assert.deepEqual(s.days, ['2026-08-01', '2026-08-02'])
  assert.deepEqual(s.total, [10, 0])
  assert.deepEqual(s.ok, [7, 0])
  assert.deepEqual(s.fail, [3, 0])
})

test('buildOverviewSeries never returns negative fail', () => {
  const s = buildOverviewSeries([{ day: '2026-08-01', total: 5, ok: 9 }])
  assert.deepEqual(s.fail, [0])
})

test('roundRate guards zero total and rounds', () => {
  assert.equal(roundRate(0, 0), 0)
  assert.equal(roundRate(1, 3), 33)
})

test('buildStatCards: admin gets 6 cards in order', () => {
  const cards = buildStatCards({
    appCount: 4, subscribeCount: 4, interfaceCount: 3,
    total: 23, successRate: 100, pendingCount: 0, isAdmin: true
  })
  assert.equal(cards.length, 6)
  assert.deepEqual(cards.map((c) => c.key), ['apps', 'subscribes', 'interfaces', 'total', 'rate', 'pending'])
  assert.equal(cards[3].display, '23')
  assert.equal(cards[4].display, '100%')
  assert.equal(cards[4].tone, 'success')
  assert.equal(cards[5].tone, 'warning')
})

test('buildStatCards: regular user gets 3 cards and no stats', () => {
  const cards = buildStatCards({
    appCount: 1, subscribeCount: 2, interfaceCount: 3,
    total: 0, successRate: 0, pendingCount: 0, isAdmin: false
  })
  assert.equal(cards.length, 3)
  assert.deepEqual(cards.map((c) => c.route), ['/apps', '/subscribes', '/interfaces'])
})
