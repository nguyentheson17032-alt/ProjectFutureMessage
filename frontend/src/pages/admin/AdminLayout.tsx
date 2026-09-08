import { NavLink, Outlet } from 'react-router-dom'

export function AdminLayout() {
  return (
    <div className="admin-page">
      <header className="page-head">
        <div>
          <p className="eyebrow">Vận hành</p>
          <h1>Quản trị</h1>
          <p className="muted">
            Theo dõi user, thư và email mở khóa. Không thay hộp thư người dùng — admin không mở thư hộ người nhận.
          </p>
        </div>
      </header>
      <nav className="admin-subnav" aria-label="Mục quản trị">
        <NavLink to="/admin" end>
          Tổng quan
        </NavLink>
        <NavLink to="/admin/users">Người dùng</NavLink>
        <NavLink to="/admin/messages">Tin nhắn</NavLink>
      </nav>
      <Outlet />
    </div>
  )
}
