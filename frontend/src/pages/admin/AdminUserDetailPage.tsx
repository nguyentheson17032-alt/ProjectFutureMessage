import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { mutate } from 'swr'
import { updateUserEnabled } from '../../api/admin'
import { useAdminUser } from '../../api/hooks'
import { useAuth } from '../../auth/AuthContext'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { FormError } from '../../components/Field'
import { PageState } from '../../components/EmptyState'
import { DirectionalTransition, TransitionLink } from '../../components/transitions'
import { errorMessage } from '../../lib/errors'
import { formatCount, formatDateTime } from '../../lib/time'

export function AdminUserDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { user: me } = useAuth()
  const { data: user, error, isLoading, mutate: mutateUser } = useAdminUser(id)
  const [confirm, setConfirm] = useState(false)
  const [pending, setPending] = useState(false)
  const [actionError, setActionError] = useState('')

  async function toggleEnabled() {
    if (!user) return
    setPending(true)
    setActionError('')
    try {
      const updated = await updateUserEnabled(user.id, !user.enabled)
      await mutateUser(updated, { revalidate: false })
      await mutate((key) => Array.isArray(key) && key[0] === 'admin')
      setConfirm(false)
    } catch (err) {
      setActionError(errorMessage(err))
    } finally {
      setPending(false)
    }
  }

  if (error && !user) {
    return (
      <DirectionalTransition>
        <section className="panel">
          <h2>Không tải được tài khoản</h2>
          <FormError>{errorMessage(error)}</FormError>
          <TransitionLink kind="back" to="/admin/users" className="btn">
            Về danh sách
          </TransitionLink>
        </section>
      </DirectionalTransition>
    )
  }

  if (isLoading || !user) {
    return (
      <DirectionalTransition>
        <PageState>Đang lấy hồ sơ…</PageState>
      </DirectionalTransition>
    )
  }

  const self = me?.id === user.id

  return (
    <DirectionalTransition>
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
            disabled={self}
            title={self ? 'Không tắt tài khoản đang đăng nhập' : undefined}
            onClick={() => setConfirm(true)}
          >
            {user.enabled ? 'Tắt tài khoản' : 'Bật tài khoản'}
          </button>
        </header>

        <FormError>{actionError}</FormError>

        <dl className="admin-dl">
          <div>
            <dt>Trạng thái</dt>
            <dd>{user.enabled ? 'Đang bật, có thể đăng nhập' : 'Đã tắt, không login / refresh'}</dd>
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
              {formatCount(user.sentCount)}{' '}
              <TransitionLink
                kind="forward"
                to={`/admin/messages?senderEmail=${encodeURIComponent(user.email)}`}
              >
                xem thư đã gửi
              </TransitionLink>
            </dd>
          </div>
          <div>
            <dt>Hộp thư (theo email)</dt>
            <dd>
              {formatCount(user.inboxCount)}{' '}
              <TransitionLink
                kind="forward"
                to={`/admin/messages?recipientEmail=${encodeURIComponent(user.email)}`}
              >
                xem hộp thư
              </TransitionLink>
            </dd>
          </div>
        </dl>

        <p className="muted">Tắt tài khoản không xóa thư đã tạo. Role không đổi được từ UI.</p>
        <TransitionLink kind="back" to="/admin/users" className="linkish">
          Về danh sách tài khoản
        </TransitionLink>
      </article>
      <ConfirmDialog
        open={confirm}
        title={user.enabled ? 'Tắt tài khoản này?' : 'Bật tài khoản này?'}
        confirmLabel={user.enabled ? 'Tắt tài khoản' : 'Bật tài khoản'}
        pending={pending}
        pendingLabel="Đang cập nhật…"
        danger={user.enabled}
        onCancel={() => setConfirm(false)}
        onConfirm={() => void toggleEnabled()}
      >
        <p>
          {user.enabled
            ? `${user.email} sẽ không đăng nhập hoặc làm mới phiên được.`
            : `${user.email} có thể đăng nhập lại.`}
        </p>
      </ConfirmDialog>
    </DirectionalTransition>
  )
}
