import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { cancelMessage, getMessage, openMessage } from '../api/messages'
import { useAuth } from '../auth/AuthContext'
import { Envelope } from '../components/Envelope'
import { StatusBadge } from '../components/StatusBadge'
import { errorMessage } from '../lib/errors'
import { countdown, formatDateTime } from '../lib/time'
import type { Message } from '../types'

export function MessageDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [message, setMessage] = useState<Message | null>(null)
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)
  const [waitLabel, setWaitLabel] = useState('')
  const [confirmCancel, setConfirmCancel] = useState(false)
  const [showOpenLabel, setShowOpenLabel] = useState(false)

  useEffect(() => {
    if (!message) return
    const tick = () => setWaitLabel(countdown(message.unlockAt))
    tick()
    const timer = window.setInterval(tick, 1000)
    return () => window.clearInterval(timer)
  }, [message])

  useEffect(() => {
    if (!id) return
    let cancelled = false
    setShowOpenLabel(false)
    getMessage(id)
      .then((item) => {
        if (!cancelled) setMessage(item)
      })
      .catch((err) => {
        if (!cancelled) setError(errorMessage(err))
      })
    return () => {
      cancelled = true
    }
  }, [id])

  const isSender = Boolean(user && message && user.id === message.senderId)
  const isRecipient = Boolean(
    user && message && user.email.toLowerCase() === message.recipientEmail.toLowerCase(),
  )
  const toSelf = message?.recipientType === 'SELF'
  const locked = message?.status === 'LOCKED'
  const available = message?.status === 'AVAILABLE'
  const opened = message?.status === 'OPENED'
  const canOpen = Boolean(isRecipient && available)
  const canEdit = Boolean(isSender && locked)
  const showLetter = Boolean(opened || (isSender && !toSelf))

  async function handleOpen() {
    if (!id) return
    setPending(true)
    setError('')
    try {
      const openedMessage = await openMessage(id)
      setMessage(openedMessage)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setPending(false)
    }
  }

  async function handleCancel() {
    if (!id) return
    setPending(true)
    setError('')
    try {
      await cancelMessage(id)
      navigate('/sent')
    } catch (err) {
      setError(errorMessage(err))
      setConfirmCancel(false)
    } finally {
      setPending(false)
    }
  }

  if (error && !message) {
    return (
      <section className="panel">
        <h1>Không mở được thư</h1>
        <p className="form-error">{error}</p>
        <Link to="/inbox" className="btn">
          Về hộp thư
        </Link>
      </section>
    )
  }

  if (!message) return <p className="page-state">Đang lấy thư…</p>

  const heading = showLetter
    ? message.title
    : available
      ? 'Phong bì sẵn sàng mở'
      : 'Phong bì đã niêm'

  return (
    <article className="detail">
      <header className="detail-meta">
        <StatusBadge status={message.status} />
        <h1>{heading}</h1>
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

      {locked && !showLetter ? (
        <Envelope mode="locked" hint="Phong bì còn niêm. Tiêu đề và nội dung hiện khi đến hạn." />
      ) : null}

      {canOpen && !showLetter ? (
        <Envelope
          mode={showOpenLabel ? 'prompt' : 'ready'}
          pending={pending}
          onReveal={() => setShowOpenLabel(true)}
          onOpen={() => void handleOpen()}
        />
      ) : null}

      {available && !canOpen && !showLetter ? (
        <Envelope mode="wait" hint="Chỉ người nhận mới được mở phong bì này." />
      ) : null}

      {showLetter ? (
        <div className="paper">
          <p className="paper-kicker">Nội dung</p>
          <p className="paper-body">{message.content}</p>
        </div>
      ) : null}

      {error ? <p className="form-error">{error}</p> : null}

      <div className="row-actions">
        {canEdit ? (
          <Link to={`/messages/${message.id}/edit`} className="btn ghost">
            Sửa thư
          </Link>
        ) : null}
        {canEdit ? (
          confirmCancel ? (
            <>
              <button className="btn danger" type="button" disabled={pending} onClick={() => void handleCancel()}>
                Xác nhận hủy
              </button>
              <button type="button" className="linkish" onClick={() => setConfirmCancel(false)}>
                Không hủy
              </button>
            </>
          ) : (
            <button type="button" className="btn ghost" onClick={() => setConfirmCancel(true)}>
              Hủy thư
            </button>
          )
        ) : null}
        <Link to={isRecipient ? '/inbox' : '/sent'} className="linkish">
          Quay lại danh sách
        </Link>
      </div>
    </article>
  )
}
