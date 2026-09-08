import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchAdminStats } from '../../api/admin'
import { errorMessage } from '../../lib/errors'
import type { AdminStats } from '../../types'

export function AdminDashboardPage() {
  const [stats, setStats] = useState<AdminStats | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    fetchAdminStats()
      .then((result) => {
        if (!cancelled) {
          setStats(result)
          setError('')
        }
      })
      .catch((err) => {
        if (!cancelled) setError(errorMessage(err))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  if (loading) return <p className="page-state">Đang lấy số liệu…</p>
  if (error) return <p className="form-error">{error}</p>
  if (!stats) return null

  const { messagesByStatus: byStatus, notifications } = stats

  return (
    <section>
      <div className="stat-grid">
        <Link to="/admin/users" className="stat-card">
          <span className="stat-label">Người dùng</span>
          <strong>{stats.totalUsers}</strong>
          <span className="muted">Tài khoản đã đăng ký</span>
        </Link>
        <Link to="/admin/messages?status=LOCKED" className="stat-card">
          <span className="stat-label">Đang khóa</span>
          <strong>{byStatus.locked}</strong>
          <span className="muted">{stats.unlockingWithin24Hours} mở trong 24 giờ tới</span>
        </Link>
        <Link to="/admin/messages?status=AVAILABLE" className="stat-card">
          <span className="stat-label">Sẵn sàng mở</span>
          <strong>{byStatus.available}</strong>
          <span className="muted">Đã đến hạn, chưa mở</span>
        </Link>
        <Link to="/admin/messages?status=OPENED" className="stat-card">
          <span className="stat-label">Đã mở</span>
          <strong>{byStatus.opened}</strong>
          <span className="muted">Người nhận đã đọc</span>
        </Link>
        <Link to="/admin/messages?status=CANCELLED" className="stat-card">
          <span className="stat-label">Đã hủy</span>
          <strong>{byStatus.cancelled}</strong>
          <span className="muted">Người gửi hủy khi còn khóa</span>
        </Link>
        <Link to="/admin/messages?notificationStatus=FAILED" className="stat-card is-alert">
          <span className="stat-label">Email thất bại</span>
          <strong>{notifications.failed}</strong>
          <span className="muted">
            Chờ gửi {notifications.pending} · Đã gửi {notifications.sent}
          </span>
        </Link>
      </div>
    </section>
  )
}
