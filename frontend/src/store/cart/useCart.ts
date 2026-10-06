import { createContext, useContext } from 'react'
import type { ShopProduct, ShopProductDetail } from '../types'

// A cart line keeps what it needs to show the product until the shop's latest data is loaded
// (prices and stock are checked again on the cart page and when ordering)
export interface CartLine {
  productId: number
  quantity: number
  name: string
  slug: string
  price: number
  imageUrl: string | null
  maxQuantity: number
}

export interface CartValue {
  lines: CartLine[]
  count: number
  subtotal: number
  add: (product: ShopProduct | ShopProductDetail, quantity?: number) => void
  setQuantity: (productId: number, quantity: number) => void
  remove: (productId: number) => void
  // Updates the lines with the shop's latest names, prices and stock
  refresh: (products: ShopProduct[]) => void
  clear: () => void
  drawerOpened: boolean
  openDrawer: () => void
  closeDrawer: () => void
}

export const CART_KEY = 'techsouk.cart'
export const CartContext = createContext<CartValue | null>(null)

export function loadCart(): CartLine[] {
  try {
    const lines = JSON.parse(localStorage.getItem(CART_KEY) ?? '[]') as CartLine[]
    return Array.isArray(lines) ? lines : []
  } catch {
    return []
  }
}

export function useCart(): CartValue {
  const value = useContext(CartContext)
  if (!value) throw new Error('useCart must be used inside <CartProvider>')
  return value
}
