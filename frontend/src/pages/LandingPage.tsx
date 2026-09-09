import { ViewTransition } from 'react'
import { useAuth } from '../auth/AuthContext'
import { DirectionalTransition, TransitionLink } from '../components/transitions'

export function LandingPage() {
  const { user } = useAuth()

  return (
    <DirectionalTransition>
      <div className="landing">
        <section className="hero">
          <p className="eyebrow">Phong bì khóa theo thời gian</p>
          <h1>
            Gửi một lá thư
            <br />
            tới tương lai
          </h1>
          <p className="lede">
            Viết cho chính mình hoặc cho người khác. Nội dung bị khóa đến thời điểm bạn chọn. Đến
            hạn, hệ thống mở khóa và gửi email. Người nhận mới đọc được thư.
          </p>
          <div className="hero-actions">
            {user?.role === 'ADMIN' ? (
              <TransitionLink kind="forward" to="/admin" className="btn">
                Vào quản trị
              </TransitionLink>
            ) : user ? (
              <TransitionLink kind="forward" to="/compose" className="btn">
                Viết thư mới
              </TransitionLink>
            ) : (
              <>
                <TransitionLink kind="forward" to="/register" className="btn">
                  Tạo tài khoản
                </TransitionLink>
                <TransitionLink kind="forward" to="/login" className="btn ghost">
                  Đăng nhập
                </TransitionLink>
              </>
            )}
          </div>
        </section>

        <ol className="steps">
          <ViewTransition>
            <li>
              <span>01</span>
              <h3>Viết</h3>
              <p>Ghi lại điều bạn muốn gửi: cho mình, hoặc một email người thân.</p>
            </li>
          </ViewTransition>
          <ViewTransition>
            <li>
              <span>02</span>
              <h3>Khóa</h3>
              <p>Chọn ngày giờ mở. Trước thời điểm đó, người nhận không thấy nội dung.</p>
            </li>
          </ViewTransition>
          <ViewTransition>
            <li>
              <span>03</span>
              <h3>Mở</h3>
              <p>Đến hạn, thư chuyển sang sẵn sàng. Người nhận mở một lần, thời điểm được ghi lại.</p>
            </li>
          </ViewTransition>
        </ol>
      </div>
    </DirectionalTransition>
  )
}
