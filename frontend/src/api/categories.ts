import { useQuery } from '@tanstack/react-query'
import { api } from './client'
import type { Category } from './types'

// The shop's categories with their number of products, alphabetically (a short list, not paged)
export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: () => api<Category[]>('/api/categories'),
  })
}
