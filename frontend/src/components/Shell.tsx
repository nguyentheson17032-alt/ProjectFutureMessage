import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function Shell() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/')
  }

  return (
    <div className="app-shell">
      <div className="sky" aria-hidden="true" />
      <header className="topbar">
        <NavLink to={user ? '/inbox' : '/'} className="brand">
          <span className="brand-seal" aria-hidden="true" />
          <span className="brand-text">
            Future Message
            <small>Thư gửi tương lai</small>
          </span>
        </NavLink>
        {user ? (
          <nav className="nav">
            <NavLink to="/inbox">Hộp thư</NavLink>
            <NavLink to="/sent">Đã gửi</NavLink>
            <NavLink to="/compose" className="nav-cta">
              Viết thư
            </NavLink>
            <span className="nav-user">{user.displayName}</span>
            <button type="button" className="linkish" onClick={() => void handleLogout()}>
              Đăng xuất
            </button>
          </nav>
        ) : (
          <nav className="nav">
            <NavLink to="/login">Đăng nhập</NavLink>
            <NavLink to="/register" className="nav-cta">
              Đăng ký
            </NavLink>
          </nav>
        )}
      </header>
      <main className="main">
        <Outlet />
      </main>
      <footer className="footer">
        <p>Tin nhắn khóa đến đúng thời điểm. Sau khi mở, nội dung không còn được sửa.</p>
      </footer>
    </div>
  )
}
