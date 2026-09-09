import { ViewTransition } from 'react'
import type { Message } from '../types'
import { countdown, formatDateTime } from '../lib/time'
import { StatusBadge } from './StatusBadge'
import { TransitionLink } from './transitions'

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
      : 'Tiêu đề và nội dung hiện khi đến hạn mở.'
    : (message.content ?? 'Không có nội dung.')

  return (
    <ViewTransition>
      <TransitionLink
        kind="forward"
        to={`/messages/${message.id}`}
        className={`letter-card${hideLetter ? ' is-sealed' : ''}`}
      >
        <ViewTransition name={`letter-${message.id}`} share="morph" default="none">
          <div>
            <div className="letter-card-head">
              <StatusBadge status={message.status} />
              <span className="muted">{countdown(message.unlockAt)}</span>
            </div>
            {hideLetter ? (
              <h3>{title}</h3>
            ) : (
              <ViewTransition name={`letter-title-${message.id}`} share="text-morph" default="none">
                <h3>{title}</h3>
              </ViewTransition>
            )}
            <p className="letter-preview">{preview}</p>
            <div className="letter-card-meta">
              <span>{meta}</span>
              <span>Mở lúc {formatDateTime(message.unlockAt)}</span>
            </div>
          </div>
        </ViewTransition>
      </TransitionLink>
    </ViewTransition>
  )
}
