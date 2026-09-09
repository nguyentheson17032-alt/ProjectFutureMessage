import { DirectionalTransition, TransitionLink } from '../components/transitions'

export function ForbiddenPage() {
  return (
    <DirectionalTransition>
      <section className="panel">
        <h1>Không có quyền</h1>
        <p className="muted">
          Trang quản trị chỉ mở với tài khoản ADMIN. Quay lại hộp thư để tiếp tục.
        </p>
        <TransitionLink kind="back" to="/inbox" className="btn">
          Về hộp thư
        </TransitionLink>
      </section>
    </DirectionalTransition>
  )
}
