import { useQueryClient } from '@tanstack/react-query'

// After any stock change, the lists, low-stock alerts, history and reports are out of date
export function useRefreshStock() {
  const queryClient = useQueryClient()
  return () => {
    for (const key of ['products', 'stock-movements', 'reports', 'sales']) {
      queryClient.invalidateQueries({ queryKey: [key] })
    }
  }
}
