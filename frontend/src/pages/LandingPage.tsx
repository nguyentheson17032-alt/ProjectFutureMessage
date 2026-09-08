import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function LandingPage() {
  const { user } = useAuth()

  return (
    <div className="landing">
      <section className="hero">
        <p className="eyebrow">Phong bì khóa theo thời gian</p>
        <h1>
          Gửi một lá thư
          <br />
          tới tương lai
        </h1>
        <p className="lede">
          Viết cho chính mình, hoặc cho người khác. Nội dung bị khóa đến đúng thời điểm bạn chọn.
          Khi đến hạn, hệ thống mở khóa, gửi email, và người nhận mới được đọc.
        </p>
        <div className="hero-actions">
          {user ? (
            <Link to="/compose" className="btn">
              Viết thư mới
            </Link>
          ) : (
            <>
              <Link to="/register" className="btn">
                Bắt đầu viết
              </Link>
              <Link to="/login" className="btn ghost">
                Đã có tài khoản
              </Link>
            </>
          )}
        </div>
      </section>

      <ol className="steps">
        <li>
          <span>01</span>
          <h3>Viết</h3>
          <p>Ghi lại điều bạn muốn gửi — cho mình, hoặc một email người thân.</p>
        </li>
        <li>
          <span>02</span>
          <h3>Khóa</h3>
          <p>Chọn ngày giờ mở. Trước thời điểm đó, người nhận không thấy nội dung.</p>
        </li>
        <li>
          <span>03</span>
          <h3>Mở</h3>
          <p>Đến hạn, thư chuyển sang sẵn sàng. Người nhận mở một lần, thời điểm được ghi lại.</p>
        </li>
      </ol>
    </div>
  )
}
