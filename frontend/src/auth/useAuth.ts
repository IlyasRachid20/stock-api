import { createContext, useContext } from 'react'
import type { Role } from '../api/types'

// What is kept between page reloads. The token expires after 8 hours (see the API);
// an expired session is dropped instead of sending a token the API would refuse.
export interface Session {
  token: string
  expiresAt: number
  username: string
  roles: Role[]
}

export interface AuthValue {
  session: Session | null
  isAdmin: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

export const STORAGE_KEY = 'stock-api.session'
export const AuthContext = createContext<AuthValue | null>(null)

export function loadSession(now = Date.now()): Session | null {
  try {
    const session = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null') as Session | null
    return session && session.expiresAt > now ? session : null
  } catch {
    return null
  }
}

export function useAuth(): AuthValue {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used inside <AuthProvider>')
  return value
}
