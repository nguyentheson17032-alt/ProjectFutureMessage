import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <section className="panel">
      <h1>Không tìm thấy trang</h1>
      <p className="muted">Đường dẫn này không tồn tại, hoặc tin nhắn không thuộc về bạn.</p>
      <Link to="/" className="btn">
        Về trang chủ
      </Link>
    </section>
  )
}
