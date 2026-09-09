import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { mutate } from 'swr'
import { retryNotification } from '../../api/admin'
import { useAdminMessage } from '../../api/hooks'
import { FormError } from '../../components/Field'
import { PageState } from '../../components/EmptyState'
import { NotificationBadge } from '../../components/NotificationBadge'
import { StatusBadge } from '../../components/StatusBadge'
import { DirectionalTransition, TransitionLink } from '../../components/transitions'
import { errorMessage } from '../../lib/errors'
import { formatDateTime } from '../../lib/time'

export function AdminMessageDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { data: message, error, isLoading, mutate: mutateMessage } = useAdminMessage(id)
  const [pending, setPending] = useState(false)
  const [actionError, setActionError] = useState('')

  async function handleRetry() {
    if (!id) return
    setPending(true)
    setActionError('')
    try {
      const updated = await retryNotification(id)
      await mutateMessage(updated, { revalidate: false })
      await mutate((key) => Array.isArray(key) && key[0] === 'admin')
    } catch (err) {
      setActionError(errorMessage(err))
    } finally {
      setPending(false)
    }
  }

  if (error && !message) {
    return (
      <DirectionalTransition>
        <section className="panel">
          <h2>Không tải được thư</h2>
          <FormError>{errorMessage(error)}</FormError>
          <TransitionLink kind="back" to="/admin/messages" className="btn">
            Về danh sách
          </TransitionLink>
        </section>
      </DirectionalTransition>
    )
  }

  if (isLoading || !message) {
    return (
      <DirectionalTransition>
        <PageState>Đang lấy chi tiết vận hành…</PageState>
      </DirectionalTransition>
    )
  }

  const canRetry =
    message.notificationStatus === 'FAILED' &&
    (message.status === 'AVAILABLE' || message.status === 'OPENED')
  const contentHidden = message.content === undefined

  return (
    <DirectionalTransition>
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

        <FormError>{actionError}</FormError>

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
              {message.recipientType === 'SELF' ? ', gửi cho chính mình' : ''}
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
              Admin chỉ đọc thư khi trạng thái AVAILABLE hoặc OPENED. Thư LOCKED / CANCELLED giữ
              nguyên quyền riêng tư.
            </p>
          </div>
        ) : (
          <div className="paper">
            <p className="paper-kicker">Nội dung</p>
            <p className="paper-body">{message.content}</p>
          </div>
        )}

        {canRetry ? (
          <p className="muted">
            Gửi lại đưa notification về PENDING. Job email (khoảng 30s) sẽ gửi qua Mailpit.
          </p>
        ) : null}

        <TransitionLink kind="back" to="/admin/messages" className="linkish">
          Về danh sách thư
        </TransitionLink>
      </article>
    </DirectionalTransition>
  )
}
