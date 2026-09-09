import { useAdminStats } from '../../api/hooks'
import { FormError } from '../../components/Field'
import { PageState } from '../../components/EmptyState'
import { FadeTransition, TransitionLink } from '../../components/transitions'
import { errorMessage } from '../../lib/errors'
import { formatCount } from '../../lib/time'

export function AdminDashboardPage() {
  const { data: stats, error, isLoading } = useAdminStats()

  if (isLoading) {
    return (
      <FadeTransition>
        <PageState>Đang lấy số liệu…</PageState>
      </FadeTransition>
    )
  }
  if (error) {
    return (
      <FadeTransition>
        <FormError>{errorMessage(error)}</FormError>
      </FadeTransition>
    )
  }
  if (!stats) return null

  const { messagesByStatus: byStatus, notifications } = stats

  return (
    <FadeTransition>
      <section>
        <div className="stat-grid">
          <TransitionLink kind="forward" to="/admin/users" className="stat-card">
            <span className="stat-label">Người dùng</span>
            <strong>{formatCount(stats.totalUsers)}</strong>
            <span className="muted">Tài khoản đã đăng ký</span>
          </TransitionLink>
          <TransitionLink kind="forward" to="/admin/messages?status=LOCKED" className="stat-card">
            <span className="stat-label">Đang khóa</span>
            <strong>{formatCount(byStatus.locked)}</strong>
            <span className="muted">
              {formatCount(stats.unlockingWithin24Hours)} mở trong 24 giờ tới
            </span>
          </TransitionLink>
          <TransitionLink kind="forward" to="/admin/messages?status=AVAILABLE" className="stat-card">
            <span className="stat-label">Sẵn sàng mở</span>
            <strong>{formatCount(byStatus.available)}</strong>
            <span className="muted">Đã đến hạn, chưa mở</span>
          </TransitionLink>
          <TransitionLink kind="forward" to="/admin/messages?status=OPENED" className="stat-card">
            <span className="stat-label">Đã mở</span>
            <strong>{formatCount(byStatus.opened)}</strong>
            <span className="muted">Người nhận đã đọc</span>
          </TransitionLink>
          <TransitionLink kind="forward" to="/admin/messages?status=CANCELLED" className="stat-card">
            <span className="stat-label">Đã hủy</span>
            <strong>{formatCount(byStatus.cancelled)}</strong>
            <span className="muted">Người gửi hủy khi còn khóa</span>
          </TransitionLink>
          <TransitionLink
            kind="forward"
            to="/admin/messages?notificationStatus=FAILED"
            className="stat-card is-alert"
          >
            <span className="stat-label">Email thất bại</span>
            <strong>{formatCount(notifications.failed)}</strong>
            <span className="muted">
              Chờ gửi {formatCount(notifications.pending)} · Đã gửi {formatCount(notifications.sent)}
            </span>
          </TransitionLink>
        </div>
      </section>
    </FadeTransition>
  )
}
