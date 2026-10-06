import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { ShopCategory } from './types'

// The shop's categories (header, footer, shop page); they rarely change
export function useShopCategories() {
  return useQuery({ queryKey: ['shop', 'categories'], queryFn: () => api<ShopCategory[]>('/api/shop/categories') })
}
