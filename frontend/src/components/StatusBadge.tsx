import type { MessageStatus } from '../types'

const LABEL: Record<MessageStatus, string> = {
  LOCKED: 'Đang khóa',
  AVAILABLE: 'Sẵn sàng mở',
  OPENED: 'Đã mở',
  CANCELLED: 'Đã hủy',
}

export function StatusBadge({ status }: { status: MessageStatus }) {
  return <span className={`badge status-${status.toLowerCase()}`}>{LABEL[status]}</span>
}
