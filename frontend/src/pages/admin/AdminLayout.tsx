import { Outlet } from 'react-router-dom'
import { TransitionNavLink } from '../../components/transitions'

export function AdminLayout() {
  return (
    <div className="admin-page">
      <header className="page-head">
        <div>
          <p className="eyebrow">Vận hành</p>
          <h1>Quản trị</h1>
          <p className="muted">
            Theo dõi tài khoản, thư và email mở khóa. Không thay hộp thư người dùng: admin không mở
            thư hộ người nhận.
          </p>
        </div>
      </header>
      <nav className="admin-subnav" aria-label="Mục quản trị">
        <TransitionNavLink kind="lateral" to="/admin" end>
          Tổng quan
        </TransitionNavLink>
        <TransitionNavLink kind="lateral" to="/admin/users">
          Người dùng
        </TransitionNavLink>
        <TransitionNavLink kind="lateral" to="/admin/messages">
          Tin nhắn
        </TransitionNavLink>
      </nav>
      <Outlet />
    </div>
  )
}
