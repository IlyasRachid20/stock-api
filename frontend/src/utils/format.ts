// Display helpers. Prices in the API are plain numbers; the shop's currency is set here.
export const CURRENCY = 'MAD'

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: CURRENCY, minimumFractionDigits: 2 })
const integer = new Intl.NumberFormat('en-US')
const dateTime = new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' })
const shortDay = new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', timeZone: 'UTC' })

export const formatMoney = (value: number) => money.format(value)
export const formatInteger = (value: number) => integer.format(value)

// Short amounts for chart axes, where the full format doesn't fit: 36000 -> "36K"
const compact = new Intl.NumberFormat('en-US', { notation: 'compact', maximumFractionDigits: 1 })
export const formatCompact = (value: number) => compact.format(value)

// API instants are UTC ("...Z"); shown in the browser's own time zone
export const formatDateTime = (instant: string) => dateTime.format(new Date(instant))

// Report days ("2026-09-10") are already days in the shop's time zone: shown as is
export const formatDay = (day: string) => shortDay.format(new Date(`${day}T00:00:00Z`))

export const signed = (value: number) => (value > 0 ? `+${value}` : String(value))

// yyyy-MM-dd, n days before today (today included)
export function daysAgo(n: number, today = new Date()): string {
  const d = new Date(Date.UTC(today.getFullYear(), today.getMonth(), today.getDate()))
  d.setUTCDate(d.getUTCDate() - n)
  return d.toISOString().slice(0, 10)
}
