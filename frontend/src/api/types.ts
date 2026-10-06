// Shapes of the API's JSON, matching the Java DTOs (see /v3/api-docs)

export type Role = 'ADMIN' | 'CASHIER'

export interface Page<T> {
  content: T[]
  page: { size: number; number: number; totalElements: number; totalPages: number }
}

export interface LoginResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
}

export interface Me {
  username: string
  roles: Role[]
}

export interface ProductImage {
  id: number
  url: string
  width: number
  height: number
}

export interface Product {
  id: number
  name: string
  description: string | null
  category: Summary | null
  price: number
  // The struck-through price during the 30 days after a price drop, otherwise null
  previousPrice: number | null
  quantity: number
  minQuantity: number
  lowStock: boolean
  // Shown in the online shop
  published: boolean
  // Cover first
  images: ProductImage[]
}

export interface Summary {
  id: number
  name: string
}

export interface PriceChange {
  id: number
  oldPrice: number
  newPrice: number
  changedBy: string
  changedAt: string
}

export interface Category {
  id: number
  name: string
  productCount: number
}

export interface Customer {
  id: number
  name: string
  email: string | null
  phone: string | null
}

export interface SaleItem {
  id: number
  saleId: number
  product: Summary
  quantity: number
  unitPrice: number
  lineTotal: number
}

// A counter sale is STORE / COMPLETED; an online order is ONLINE and moves through OrderStatus
export type OrderStatus = 'NEW' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED' | 'RETURNED'

export interface Sale {
  id: number
  customer: Summary
  saleDate: string
  items: SaleItem[]
  total: number
  channel: 'STORE' | 'ONLINE'
  status: 'COMPLETED' | OrderStatus
  orderNumber: string | null
}

export interface OrderSummary {
  id: number
  orderNumber: string
  status: OrderStatus
  placedAt: string
  customerName: string
  phone: string
  city: string
  itemCount: number
  total: number
}

export interface OrderDetail {
  id: number
  orderNumber: string
  status: OrderStatus
  placedAt: string
  customerId: number
  delivery: { name: string; phone: string; city: string; address: string; note: string | null }
  items: SaleItem[]
  subtotal: number
  deliveryFee: number
  total: number
  history: { status: OrderStatus; note: string | null; changedBy: string; changedAt: string }[]
  // The steps it can move to (the buttons to show)
  nextStatuses: OrderStatus[]
}

export interface User {
  id: number
  username: string
  role: Role
  enabled: boolean
}

export type MovementType = 'INITIAL' | 'RESTOCK' | 'ADJUSTMENT' | 'SALE' | 'SALE_CANCELLED'

export interface StockMovement {
  id: number
  product: Summary
  type: MovementType
  quantityChange: number
  quantityAfter: number
  reason: string | null
  saleItemId: number | null
  createdBy: string
  createdAt: string
}

export interface SalesSummary {
  from: string
  to: string
  salesCount: number
  itemsSold: number
  revenue: number
  averageSale: number
  // The part of the totals from delivered online orders
  onlineSalesCount: number
  onlineRevenue: number
}

export interface DailySales {
  date: string
  salesCount: number
  itemsSold: number
  revenue: number
}

export interface TopProduct {
  productId: number
  name: string
  quantitySold: number
  revenue: number
}

// categoryId is null for the "Uncategorized" group
export interface CategorySales {
  categoryId: number | null
  name: string
  quantitySold: number
  revenue: number
}
