import { Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { homePath } from '../auth/paths'
import { TransitionLink, TransitionNavLink, useNavigateTransition } from './transitions'

export function Shell() {
  const { user, logout } = useAuth()
  const navigate = useNavigateTransition()

  async function handleLogout() {
    await logout()
    navigate('/', 'lateral')
  }

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main">
        Tới nội dung chính
      </a>
      <div className="sky" aria-hidden="true" />
      <header className="topbar">
        <TransitionLink kind="lateral" to={homePath(user)} className="brand">
          <span className="brand-seal" aria-hidden="true" />
          <span className="brand-text">
            Future Message
            <small>Thư gửi tương lai</small>
          </span>
        </TransitionLink>
        {user ? (
          <nav className="nav" aria-label="Chính">
            <TransitionNavLink kind="lateral" to="/inbox">
              Hộp thư
            </TransitionNavLink>
            <TransitionNavLink kind="lateral" to="/sent">
              Đã gửi
            </TransitionNavLink>
            {user.role === 'ADMIN' ? (
              <TransitionNavLink kind="lateral" to="/admin">
                Quản trị
              </TransitionNavLink>
            ) : null}
            <TransitionNavLink kind="forward" to="/compose" className="nav-cta">
              Viết thư
            </TransitionNavLink>
            <span className="nav-user">{user.displayName}</span>
            <button type="button" className="linkish" onClick={() => void handleLogout()}>
              Đăng xuất
            </button>
          </nav>
        ) : (
          <nav className="nav" aria-label="Tài khoản">
            <TransitionNavLink kind="forward" to="/login">
              Đăng nhập
            </TransitionNavLink>
            <TransitionNavLink kind="forward" to="/register" className="nav-cta">
              Đăng ký
            </TransitionNavLink>
          </nav>
        )}
      </header>
      <main id="main" className="main" tabIndex={-1}>
        <Outlet />
      </main>
      <footer className="footer">
        <p>Tin nhắn khóa đến đúng thời điểm. Sau khi mở, nội dung không còn được sửa.</p>
      </footer>
    </div>
  )
}
