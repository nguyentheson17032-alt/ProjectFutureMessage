import { Link } from 'react-router-dom'

export function ForbiddenPage() {
  return (
    <section className="panel">
      <h1>Không có quyền</h1>
      <p className="muted">
        Trang quản trị chỉ dành cho tài khoản ADMIN. User thường không thấy và không gọi được API này.
      </p>
      <Link to="/inbox" className="btn">
        Về hộp thư
      </Link>
    </section>
  )
}
