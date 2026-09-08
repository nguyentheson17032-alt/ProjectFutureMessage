import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getAdminUser, updateUserEnabled } from '../../api/admin'
import { useAuth } from '../../auth/AuthContext'
import { errorMessage } from '../../lib/errors'
import { formatDateTime } from '../../lib/time'
import type { AdminUserDetail } from '../../types'

export function AdminUserDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { user: me } = useAuth()
  const [user, setUser] = useState<AdminUserDetail | null>(null)
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)

  useEffect(() => {
    if (!id) return
    let cancelled = false
    getAdminUser(id)
      .then((result) => {
        if (!cancelled) setUser(result)
      })
      .catch((err) => {
        if (!cancelled) setError(errorMessage(err))
      })
    return () => {
      cancelled = true
    }
  }, [id])

  async function toggleEnabled() {
    if (!user) return
    setPending(true)
    setError('')
    try {
      setUser(await updateUserEnabled(user.id, !user.enabled))
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setPending(false)
    }
  }

  if (error && !user) {
    return (
      <section className="panel">
        <h2>Không tải được user</h2>
        <p className="form-error">{error}</p>
        <Link to="/admin/users" className="btn">
          Về danh sách
        </Link>
      </section>
    )
  }

  if (!user) return <p className="page-state">Đang lấy hồ sơ…</p>

  const self = me?.id === user.id

  return (
    <article className="admin-detail">
      <header className="page-head">
        <div>
          <p className="eyebrow">{user.role}</p>
          <h2>{user.displayName}</h2>
          <p className="muted">{user.email}</p>
        </div>
        <button
          type="button"
          className={user.enabled ? 'btn ghost' : 'btn'}
          disabled={pending || self}
          title={self ? 'Không tắt tài khoản đang đăng nhập' : undefined}
          onClick={() => void toggleEnabled()}
        >
          {user.enabled ? 'Tắt tài khoản' : 'Bật tài khoản'}
        </button>
      </header>

      {error ? <p className="form-error">{error}</p> : null}

      <dl className="admin-dl">
        <div>
          <dt>Trạng thái</dt>
          <dd>{user.enabled ? 'Đang bật — có thể đăng nhập' : 'Đã tắt — không login / refresh'}</dd>
        </div>
        <div>
          <dt>Email đã xác minh</dt>
          <dd>{user.emailVerified ? 'Có' : 'Chưa'}</dd>
        </div>
        <div>
          <dt>Tạo lúc</dt>
          <dd>{formatDateTime(user.createdAt)}</dd>
        </div>
        <div>
          <dt>Cập nhật</dt>
          <dd>{formatDateTime(user.updatedAt)}</dd>
        </div>
        <div>
          <dt>Thư đã gửi</dt>
          <dd>
            {user.sentCount}{' '}
            <Link to={`/admin/messages?senderEmail=${encodeURIComponent(user.email)}`}>xem</Link>
          </dd>
        </div>
        <div>
          <dt>Hộp thư (theo email)</dt>
          <dd>
            {user.inboxCount}{' '}
            <Link to={`/admin/messages?recipientEmail=${encodeURIComponent(user.email)}`}>xem</Link>
          </dd>
        </div>
      </dl>

      <p className="muted">Tắt tài khoản không xóa thư đã tạo. Role không đổi được từ UI.</p>
      <Link to="/admin/users" className="linkish">
        Về danh sách user
      </Link>
    </article>
  )
}
