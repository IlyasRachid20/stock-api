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

export interface Product {
  id: number
  name: string
  price: number
  quantity: number
  minQuantity: number
  lowStock: boolean
}

export interface Summary {
  id: number
  name: string
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
