import { useQueryClient } from '@tanstack/react-query'

// After any stock change, the lists, low-stock alerts, history, reports and the number of
// products per category are out of date
export function useRefreshStock() {
  const queryClient = useQueryClient()
  return () => {
    for (const key of ['products', 'stock-movements', 'reports', 'sales', 'categories']) {
      queryClient.invalidateQueries({ queryKey: [key] })
    }
  }
}
