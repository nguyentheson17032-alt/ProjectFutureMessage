import { Link } from 'react-router-dom'
import type { Message } from '../types'
import { countdown, formatDateTime } from '../lib/time'
import { StatusBadge } from './StatusBadge'

type Props = {
  message: Message
  perspective: 'inbox' | 'sent'
}

export function MessageCard({ message, perspective }: Props) {
  const hideLetter =
    perspective === 'inbox'
      ? message.status !== 'OPENED'
      : message.recipientType === 'SELF' && message.status !== 'OPENED'

  const meta =
    perspective === 'inbox'
      ? `Từ ${message.senderDisplayName}`
      : message.recipientType === 'SELF'
        ? 'Gửi cho chính mình'
        : `Gửi tới ${message.recipientEmail}`

  const title = hideLetter
    ? message.status === 'AVAILABLE'
      ? 'Phong bì sẵn sàng mở'
      : 'Phong bì đã niêm'
    : message.title

  const preview = hideLetter
    ? message.status === 'AVAILABLE'
      ? 'Bấm vào phong bì, rồi chọn Mở tin nhắn.'
      : 'Tiêu đề và nội dung sẽ hiện khi đến hạn mở.'
    : (message.content ?? '—')

  return (
    <Link to={`/messages/${message.id}`} className={`letter-card${hideLetter ? ' is-sealed' : ''}`}>
      <div className="letter-card-head">
        <StatusBadge status={message.status} />
        <span className="muted">{countdown(message.unlockAt)}</span>
      </div>
      <h3>{title}</h3>
      <p className="letter-preview">{preview}</p>
      <div className="letter-card-meta">
        <span>{meta}</span>
        <span>Mở lúc {formatDateTime(message.unlockAt)}</span>
      </div>
    </Link>
  )
}
