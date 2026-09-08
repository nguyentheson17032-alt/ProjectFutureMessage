import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { ForbiddenPage } from '../pages/ForbiddenPage'
import { useAuth } from './AuthContext'

export function RequireAdmin() {
  const { user, ready } = useAuth()
  const location = useLocation()

  if (!ready) {
    return (
      <div className="page-state">
        <p>Đang kiểm tra quyền quản trị…</p>
      </div>
    )
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (user.role !== 'ADMIN') {
    return <ForbiddenPage />
  }

  return <Outlet />
}
