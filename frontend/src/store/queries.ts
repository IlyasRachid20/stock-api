import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { ShopCategory, ShopInfo } from './types'

// The shop's categories (header, footer, shop page); they rarely change
// Delivery fee and free delivery threshold
export function useShopInfo() {
  return useQuery({ queryKey: ['shop', 'info'], queryFn: () => api<ShopInfo>('/api/shop/info'), staleTime: 300_000 })
}

export function useShopCategories() {
  return useQuery({ queryKey: ['shop', 'categories'], queryFn: () => api<ShopCategory[]>('/api/shop/categories') })
}
