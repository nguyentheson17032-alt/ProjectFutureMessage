import { useEffect, useState, ViewTransition } from 'react'
import { useParams } from 'react-router-dom'
import { mutate } from 'swr'
import { useMessage } from '../api/hooks'
import { cancelMessage, openMessage } from '../api/messages'
import { useAuth } from '../auth/AuthContext'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { Envelope } from '../components/Envelope'
import { FormError } from '../components/Field'
import { PageState } from '../components/EmptyState'
import { StatusBadge } from '../components/StatusBadge'
import { DirectionalTransition, TransitionLink, useNavigateTransition } from '../components/transitions'
import { errorMessage } from '../lib/errors'
import { countdown, formatDateTime } from '../lib/time'

export function MessageDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { user } = useAuth()
  const navigate = useNavigateTransition()
  const { data: message, error, isLoading, mutate: mutateMessage } = useMessage(id)
  const [pending, setPending] = useState(false)
  const [actionError, setActionError] = useState('')
  const [waitLabel, setWaitLabel] = useState('')
  const [confirmCancel, setConfirmCancel] = useState(false)
  const [showOpenLabel, setShowOpenLabel] = useState(false)
  const [activeId, setActiveId] = useState(id)
  if (id !== activeId) {
    setActiveId(id)
    setShowOpenLabel(false)
    setActionError('')
  }

  useEffect(() => {
    if (!message) return
    const tick = () => setWaitLabel(countdown(message.unlockAt))
    tick()
    const timer = window.setInterval(tick, 1000)
    return () => window.clearInterval(timer)
  }, [message])

  async function handleOpen() {
    if (!id) return
    setPending(true)
    setActionError('')
    try {
      const openedMessage = await openMessage(id)
      await mutateMessage(openedMessage, { revalidate: false })
      await mutate((key) => Array.isArray(key) && key[0] === 'messages')
    } catch (err) {
      setActionError(errorMessage(err))
    } finally {
      setPending(false)
    }
  }

  async function handleCancel() {
    if (!id) return
    setPending(true)
    setActionError('')
    try {
      await cancelMessage(id)
      await mutate((key) => Array.isArray(key) && key[0] === 'messages')
      navigate('/sent', 'back')
    } catch (err) {
      setActionError(errorMessage(err))
      setConfirmCancel(false)
    } finally {
      setPending(false)
    }
  }

  if (error && !message) {
    return (
      <DirectionalTransition>
        <section className="panel">
          <h1>Không mở được thư</h1>
          <FormError>{errorMessage(error)}</FormError>
          <TransitionLink kind="back" to="/inbox" className="btn">
            Về hộp thư
          </TransitionLink>
        </section>
      </DirectionalTransition>
    )
  }

  if (isLoading || !message) {
    return (
      <DirectionalTransition>
        <PageState>Đang lấy thư…</PageState>
      </DirectionalTransition>
    )
  }

  const isSender = Boolean(user && user.id === message.senderId)
  const isRecipient = Boolean(user && user.email.toLowerCase() === message.recipientEmail.toLowerCase())
  const toSelf = message.recipientType === 'SELF'
  const locked = message.status === 'LOCKED'
  const available = message.status === 'AVAILABLE'
  const opened = message.status === 'OPENED'
  const canOpen = Boolean(isRecipient && available)
  const canEdit = Boolean(isSender && locked)
  const showLetter = Boolean(opened || (isSender && !toSelf))
  const heading = showLetter
    ? message.title
    : available
      ? 'Phong bì sẵn sàng mở'
      : 'Phong bì đã niêm'

  return (
    <DirectionalTransition>
      <article className="detail">
        <header className="detail-meta">
          <StatusBadge status={message.status} />
          {showLetter ? (
            <ViewTransition name={`letter-title-${message.id}`} share="text-morph" default="none">
              <h1>{heading}</h1>
            </ViewTransition>
          ) : (
            <h1>{heading}</h1>
          )}
          <ul>
            <li>Người gửi: {message.senderDisplayName}</li>
            <li>
              Người nhận:{' '}
              {message.recipientType === 'SELF' ? 'Chính người gửi' : message.recipientEmail}
            </li>
            <li>Mở lúc: {formatDateTime(message.unlockAt)}</li>
            <li>{locked ? waitLabel : `Tạo lúc ${formatDateTime(message.createdAt)}`}</li>
            {message.openedAt ? <li>Đã mở: {formatDateTime(message.openedAt)}</li> : null}
          </ul>
        </header>

        <ViewTransition name={`letter-${message.id}`} share="morph" default="none">
          <div>
            {locked && !showLetter ? (
              <Envelope.Locked hint="Phong bì còn niêm. Tiêu đề và nội dung hiện khi đến hạn." />
            ) : null}

            {canOpen && !showLetter ? (
              showOpenLabel ? (
                <Envelope.Prompt pending={pending} onOpen={() => void handleOpen()} />
              ) : (
                <Envelope.Ready onReveal={() => setShowOpenLabel(true)} />
              )
            ) : null}

            {available && !canOpen && !showLetter ? (
              <Envelope.Wait hint="Chỉ người nhận mới được mở phong bì này." />
            ) : null}

            {showLetter ? (
              <div className="paper">
                <p className="paper-kicker">Nội dung</p>
                <p className="paper-body">{message.content}</p>
              </div>
            ) : null}
          </div>
        </ViewTransition>

        <FormError>{actionError}</FormError>

        <div className="row-actions">
          {canEdit ? (
            <TransitionLink kind="forward" to={`/messages/${message.id}/edit`} className="btn ghost">
              Sửa thư
            </TransitionLink>
          ) : null}
          {canEdit ? (
            <button type="button" className="btn ghost" onClick={() => setConfirmCancel(true)}>
              Hủy thư
            </button>
          ) : null}
          <TransitionLink kind="back" to={isRecipient ? '/inbox' : '/sent'} className="linkish">
            Quay lại danh sách
          </TransitionLink>
        </div>
      </article>
      <ConfirmDialog
        open={confirmCancel}
        title="Hủy thư này?"
        confirmLabel="Hủy thư"
        pending={pending}
        pendingLabel="Đang hủy…"
        danger
        onCancel={() => setConfirmCancel(false)}
        onConfirm={() => void handleCancel()}
      >
        <p>Thư sẽ chuyển sang đã hủy. Người nhận không mở được nội dung.</p>
      </ConfirmDialog>
    </DirectionalTransition>
  )
}
