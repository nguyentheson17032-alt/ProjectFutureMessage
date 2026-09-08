import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getAdminMessage, retryNotification } from '../../api/admin'
import { NotificationBadge } from '../../components/NotificationBadge'
import { StatusBadge } from '../../components/StatusBadge'
import { errorMessage } from '../../lib/errors'
import { formatDateTime } from '../../lib/time'
import type { AdminMessage } from '../../types'

export function AdminMessageDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [message, setMessage] = useState<AdminMessage | null>(null)
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)

  useEffect(() => {
    if (!id) return
    let cancelled = false
    getAdminMessage(id)
      .then((result) => {
        if (!cancelled) setMessage(result)
      })
      .catch((err) => {
        if (!cancelled) setError(errorMessage(err))
      })
    return () => {
      cancelled = true
    }
  }, [id])

  async function handleRetry() {
    if (!id) return
    setPending(true)
    setError('')
    try {
      setMessage(await retryNotification(id))
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setPending(false)
    }
  }

  if (error && !message) {
    return (
      <section className="panel">
        <h2>Không tải được thư</h2>
        <p className="form-error">{error}</p>
        <Link to="/admin/messages" className="btn">
          Về danh sách
        </Link>
      </section>
    )
  }

  if (!message) return <p className="page-state">Đang lấy chi tiết vận hành…</p>

  const canRetry = message.notificationStatus === 'FAILED' && (message.status === 'AVAILABLE' || message.status === 'OPENED')
  const contentHidden = message.content === undefined

  return (
    <article className="admin-detail">
      <header className="page-head">
        <div>
          <div className="row-actions">
            <StatusBadge status={message.status} />
            <NotificationBadge status={message.notificationStatus} />
          </div>
          <h2>{message.title}</h2>
        </div>
        {canRetry ? (
          <button className="btn" type="button" disabled={pending} onClick={() => void handleRetry()}>
            {pending ? 'Đang xếp hàng…' : 'Gửi lại email'}
          </button>
        ) : null}
      </header>

      {error ? <p className="form-error">{error}</p> : null}

      <dl className="admin-dl">
        <div>
          <dt>Người gửi</dt>
          <dd>
            {message.senderDisplayName} ({message.senderEmail})
          </dd>
        </div>
        <div>
          <dt>Người nhận</dt>
          <dd>
            {message.recipientEmail}
            {message.recipientType === 'SELF' ? ' — gửi cho chính mình' : ''}
          </dd>
        </div>
        <div>
          <dt>Mở lúc</dt>
          <dd>{formatDateTime(message.unlockAt)}</dd>
        </div>
        <div>
          <dt>Đã mở</dt>
          <dd>{message.openedAt ? formatDateTime(message.openedAt) : 'Chưa'}</dd>
        </div>
        <div>
          <dt>Email thông báo</dt>
          <dd>
            {message.notifiedAt ? `Gửi lúc ${formatDateTime(message.notifiedAt)}` : 'Chưa gửi thành công'}
          </dd>
        </div>
        <div>
          <dt>Tạo / cập nhật</dt>
          <dd>
            {formatDateTime(message.createdAt)} · {formatDateTime(message.updatedAt)}
          </dd>
        </div>
      </dl>

      {contentHidden ? (
        <div className="panel">
          <h3>Nội dung đang ẩn</h3>
          <p className="muted">
            Admin chỉ đọc thư khi trạng thái AVAILABLE hoặc OPENED. Thư LOCKED / CANCELLED giữ nguyên quyền riêng tư.
          </p>
        </div>
      ) : (
        <div className="paper">
          <p className="paper-kicker">Nội dung</p>
          <p className="paper-body">{message.content}</p>
        </div>
      )}

      {canRetry ? (
        <p className="muted">Gửi lại đưa notification về PENDING. Job email (khoảng 30 giây) sẽ gửi qua Mailpit.</p>
      ) : null}

      <Link to="/admin/messages" className="linkish">
        Về danh sách thư
      </Link>
    </article>
  )
}
