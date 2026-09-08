import type { NotificationStatus } from '../types'

const LABEL: Record<NotificationStatus, string> = {
  PENDING: 'Chờ gửi',
  SENT: 'Đã gửi',
  FAILED: 'Gửi thất bại',
}

export function NotificationBadge({ status }: { status: NotificationStatus }) {
  return <span className={`badge notif-${status.toLowerCase()}`}>{LABEL[status]}</span>
}
