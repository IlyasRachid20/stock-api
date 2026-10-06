import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import type { ShopProduct, ShopProductDetail } from '../types'
import { CART_KEY, CartContext, loadCart, type CartLine, type CartValue } from './useCart'

function imageOf(product: ShopProduct | ShopProductDetail): string | null {
  return 'imageUrl' in product ? product.imageUrl : (product.images[0]?.url ?? null)
}

// The visitor's cart, kept in the browser (no account needed) until they order
export function CartProvider({ children }: { children: ReactNode }) {
  const [lines, setLines] = useState<CartLine[]>(loadCart)
  const [drawerOpened, setDrawerOpened] = useState(false)

  useEffect(() => {
    localStorage.setItem(CART_KEY, JSON.stringify(lines))
  }, [lines])

  const add = useCallback((product: ShopProduct | ShopProductDetail, quantity = 1) => {
    setLines((current) => {
      const existing = current.find((line) => line.productId === product.id)
      const line: CartLine = {
        productId: product.id,
        quantity: Math.min((existing?.quantity ?? 0) + quantity, product.maxQuantity),
        name: product.name,
        slug: product.slug,
        price: product.price,
        imageUrl: imageOf(product),
        maxQuantity: product.maxQuantity,
      }
      return existing ? current.map((l) => (l === existing ? line : l)) : [...current, line]
    })
    setDrawerOpened(true)
  }, [])

  const setQuantity = useCallback((productId: number, quantity: number) => {
    setLines((current) => current.map((line) =>
      line.productId === productId ? { ...line, quantity: Math.max(1, Math.min(quantity, line.maxQuantity)) } : line))
  }, [])

  const remove = useCallback((productId: number) => {
    setLines((current) => current.filter((line) => line.productId !== productId))
  }, [])

  const refresh = useCallback((products: ShopProduct[]) => {
    setLines((current) => current.map((line) => {
      const product = products.find((p) => p.id === line.productId)
      if (!product) return line
      return {
        ...line,
        name: product.name,
        slug: product.slug,
        price: product.price,
        imageUrl: product.imageUrl,
        maxQuantity: product.maxQuantity,
        quantity: Math.min(line.quantity, Math.max(product.maxQuantity, 1)),
      }
    }))
  }, [])

  const clear = useCallback(() => setLines([]), [])

  const value = useMemo<CartValue>(() => ({
    lines,
    count: lines.reduce((sum, line) => sum + line.quantity, 0),
    subtotal: lines.reduce((sum, line) => sum + line.price * line.quantity, 0),
    add,
    setQuantity,
    remove,
    refresh,
    clear,
    drawerOpened,
    openDrawer: () => setDrawerOpened(true),
    closeDrawer: () => setDrawerOpened(false),
  }), [lines, add, setQuantity, remove, refresh, clear, drawerOpened])

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}
