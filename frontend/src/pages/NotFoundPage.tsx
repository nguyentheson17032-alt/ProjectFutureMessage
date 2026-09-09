import { DirectionalTransition, TransitionLink } from '../components/transitions'

export function NotFoundPage() {
  return (
    <DirectionalTransition>
      <section className="panel">
        <h1>Không tìm thấy trang</h1>
        <p className="muted">Đường dẫn này không tồn tại, hoặc tin nhắn không thuộc về bạn.</p>
        <TransitionLink kind="back" to="/" className="btn">
          Về trang chủ
        </TransitionLink>
      </section>
    </DirectionalTransition>
  )
}
