import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { api, configureApi } from '../api/client'
import type { LoginResponse, Me } from '../api/types'
import { AuthContext, loadSession, STORAGE_KEY, type AuthValue, type Session } from './useAuth'

// The token is read from the stored session from the very first request: on a page reload, parts of
// the screen (e.g. the menu's order count) ask the API before the provider's effects have run
configureApi({ getToken: () => loadSession()?.token ?? null, onUnauthorized: () => {} })

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [session, setSession] = useState<Session | null>(() => loadSession())

  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY)
    setSession(null)
    queryClient.clear()
  }, [queryClient])

  // A 401 on a call made with a token means it expired: log out
  useEffect(() => {
    configureApi({ getToken: () => loadSession()?.token ?? null, onUnauthorized: logout })
  }, [logout])

  // Log out right when the token expires
  useEffect(() => {
    if (!session) return
    const timer = window.setTimeout(logout, Math.max(0, session.expiresAt - Date.now()))
    return () => window.clearTimeout(timer)
  }, [session, logout])

  const login = useCallback(async (username: string, password: string) => {
    const response = await api<LoginResponse>('/api/auth/login', { method: 'POST', body: { username, password } })
    const expiresAt = Date.now() + response.expiresIn * 1000
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ token: response.accessToken, expiresAt, username, roles: [] }))
    const me = await api<Me>('/api/auth/me')
    const newSession: Session = { token: response.accessToken, expiresAt, username: me.username, roles: me.roles }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(newSession))
    setSession(newSession)
  }, [])

  const value = useMemo<AuthValue>(
    () => ({ session, isAdmin: session?.roles.includes('ADMIN') ?? false, login, logout }),
    [session, login, logout],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
