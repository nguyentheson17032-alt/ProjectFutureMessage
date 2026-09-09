import { createContext, use, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { mutate } from 'swr'
import * as authApi from '../api/auth'
import { persistSession, clearSession, getRefreshToken, getStoredUser } from './session'
import type { User } from '../types'

type AuthContextValue = {
  user: User | null
  ready: boolean
  login: (email: string, password: string) => Promise<User>
  register: (email: string, password: string, displayName: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => getStoredUser())
  const [ready, setReady] = useState(false)

  useEffect(() => {
    const onSession = () => setUser(getStoredUser())
    const onLogout = () => setUser(null)
    window.addEventListener('fm:session', onSession)
    window.addEventListener('fm:logout', onLogout)
    return () => {
      window.removeEventListener('fm:session', onSession)
      window.removeEventListener('fm:logout', onLogout)
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    async function boot() {
      if (!getStoredUser()) {
        setReady(true)
        return
      }
      try {
        const me = await authApi.fetchMe()
        if (!cancelled) setUser(me)
      } catch {
        if (!cancelled) setUser(getStoredUser())
      } finally {
        if (!cancelled) setReady(true)
      }
    }
    void boot()
    return () => {
      cancelled = true
    }
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const auth = await authApi.login(email, password)
    persistSession(auth)
    setUser(auth.user)
    return auth.user
  }, [])

  const register = useCallback(async (email: string, password: string, displayName: string) => {
    const auth = await authApi.register(email, password, displayName)
    persistSession(auth)
    setUser(auth.user)
  }, [])

  const logout = useCallback(async () => {
    const refreshToken = getRefreshToken()
    try {
      if (refreshToken) await authApi.logout(refreshToken)
    } catch {
      // still clear local session
    }
    clearSession()
    setUser(null)
    await mutate(() => true, undefined, { revalidate: false })
  }, [])

  const value = useMemo(
    () => ({ user, ready, login, register, logout }),
    [user, ready, login, register, logout],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}

export function useAuth() {
  const ctx = use(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
