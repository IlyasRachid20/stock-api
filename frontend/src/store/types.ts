// Shapes of the online shop's public API (/api/shop), see ShopDtos.java

export type Availability = 'IN_STOCK' | 'FEW_LEFT' | 'OUT_OF_STOCK'

export interface CategoryLink {
  id: number
  slug: string
  name: string
}

export interface ShopCategory extends CategoryLink {
  productCount: number
  imageUrl: string | null
}

export interface ShopProduct {
  id: number
  slug: string
  name: string
  category: CategoryLink | null
  price: number
  // Struck through for 30 days after a price drop
  previousPrice: number | null
  availability: Availability
  // Only when few are left
  onlyLeft: number | null
  // The most a visitor can put in the cart
  maxQuantity: number
  imageUrl: string | null
}

export interface ShopProductDetail extends Omit<ShopProduct, 'imageUrl'> {
  description: string | null
  images: { id: number; url: string; width: number; height: number }[]
}

export interface ShopHome {
  categories: ShopCategory[]
  deals: ShopProduct[]
  bestSellers: ShopProduct[]
}

export interface ShopInfo {
  currency: string
  deliveryFee: number
  freeDeliveryFrom: number
}

export type OrderStatus = 'NEW' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED' | 'RETURNED'

// An order as its customer sees it (after placing it, or when tracking it)
export interface OrderView {
  orderNumber: string
  status: OrderStatus
  placedAt: string
  items: { productId: number; name: string; quantity: number; unitPrice: number; lineTotal: number }[]
  subtotal: number
  deliveryFee: number
  total: number
  delivery: { name: string; phone: string; city: string; address: string; note: string | null }
  history: { status: OrderStatus; at: string }[]
}

// What the customer reads for each status (cash on delivery: the shop calls before shipping)
export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  NEW: 'Received',
  CONFIRMED: 'Confirmed',
  SHIPPED: 'On its way',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
  RETURNED: 'Returned',
}

// What delivery costs for a subtotal
export const deliveryFeeFor = (subtotal: number, info: ShopInfo) => (subtotal >= info.freeDeliveryFrom ? 0 : info.deliveryFee)

// "/p/1-galaxy-s26": the id finds the product, the name makes the address readable
export const productPath = (product: { id: number; slug: string }) => `/p/${product.id}-${product.slug}`
