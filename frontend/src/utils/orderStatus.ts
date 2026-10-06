import type { OrderStatus } from '../api/types'

// What the staff reads for each status of an online order, and its colour
export const ORDER_STATUS: Record<OrderStatus, { label: string; color: string }> = {
  NEW: { label: 'To confirm', color: 'orange' },
  CONFIRMED: { label: 'Confirmed', color: 'blue' },
  SHIPPED: { label: 'Shipped', color: 'grape' },
  DELIVERED: { label: 'Delivered', color: 'teal' },
  CANCELLED: { label: 'Cancelled', color: 'gray' },
  RETURNED: { label: 'Returned', color: 'red' },
}
