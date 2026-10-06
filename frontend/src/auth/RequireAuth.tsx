import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useAuth } from './useAuth'

// Sends visitors without a session to the login page, and back here after logging in
export function RequireAuth({ children, adminOnly = false }: { children: ReactNode; adminOnly?: boolean }) {
  const { session, isAdmin } = useAuth()
  const location = useLocation()

  if (!session) {
    return <Navigate to="/admin/login" replace state={{ from: location.pathname + location.search }} />
  }
  if (adminOnly && !isAdmin) {
    return <Navigate to="/admin" replace />
  }
  return children
}
