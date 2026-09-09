import { Navigate, Outlet, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { PageState } from '../components/EmptyState'
import { useAuth } from './AuthContext'
import { homePath } from './paths'

export function RequireAuth() {
  const { user, ready } = useAuth()
  const location = useLocation()

  if (!ready) {
    return <PageState>Đang mở phiên đăng nhập…</PageState>
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <Outlet />
}

export function GuestOnly({ children }: { children: ReactNode }) {
  const { user, ready } = useAuth()
  if (!ready) return <PageState>Đang mở phiên đăng nhập…</PageState>
  if (user) return <Navigate to={homePath(user)} replace />
  return children
}
