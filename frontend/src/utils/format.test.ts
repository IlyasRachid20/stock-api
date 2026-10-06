import { describe, expect, it } from 'vitest'
import { daysAgo, formatCompact, formatDay, formatMoney, reductionPercent, signed } from './format'

describe('format', () => {
  // Intl puts a non-breaking space (character 160, U+00A0) after the currency code
  const NBSP = String.fromCharCode(160)
  const plain = (text: string) => text.split(NBSP).join(' ')

  it('formats money with the shop currency and 2 decimals', () => {
    expect(plain(formatMoney(9500))).toBe('MAD 9,500.00')
    expect(plain(formatMoney(49.9))).toBe('MAD 49.90')
  })

  it('shows report days as they are, without shifting them to another time zone', () => {
    expect(formatDay('2026-09-10')).toBe('10 Sept')
  })

  it('shortens amounts for chart axes', () => {
    expect(formatCompact(36000)).toBe('36K')
    expect(formatCompact(1500)).toBe('1.5K')
    expect(formatCompact(0)).toBe('0')
  })

  it('rounds price reductions to whole percents', () => {
    expect(reductionPercent(9500, 9999)).toBe(5)
    expect(reductionPercent(129, 149)).toBe(13)
  })

  it('signs stock changes', () => {
    expect(signed(5)).toBe('+5')
    expect(signed(-3)).toBe('-3')
    expect(signed(0)).toBe('0')
  })

  it('computes dates n days back, today included', () => {
    const today = new Date(2026, 8, 30)
    expect(daysAgo(0, today)).toBe('2026-09-30')
    expect(daysAgo(29, today)).toBe('2026-09-01')
    expect(daysAgo(30, today)).toBe('2026-08-31')
  })
})
