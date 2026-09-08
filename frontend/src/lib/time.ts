const VN_TZ = 'Asia/Ho_Chi_Minh'

function zonedParts(date: Date) {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: VN_TZ,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).formatToParts(date)
  const get = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((part) => part.type === type)?.value ?? ''
  let hour = get('hour')
  if (hour === '24') hour = '00'
  return {
    year: get('year'),
    month: get('month'),
    day: get('day'),
    hour,
    minute: get('minute'),
  }
}

export function toDatetimeLocal(iso?: string, plusMs = 0): string {
  const date = iso ? new Date(new Date(iso).getTime() + plusMs) : new Date(Date.now() + plusMs)
  const p = zonedParts(date)
  return `${p.year}-${p.month}-${p.day}T${p.hour}:${p.minute}`
}

export function defaultUnlockLocal(): string {
  return toDatetimeLocal(undefined, 7 * 24 * 60 * 60 * 1000)
}

/** datetime-local value is treated as Vietnam time (UTC+7, no DST). */
export function localInputToIso(value: string): string {
  return new Date(`${value}:00+07:00`).toISOString()
}

export function formatDateTime(iso: string): string {
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: VN_TZ,
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(iso))
}

export function formatDate(iso: string): string {
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: VN_TZ,
    dateStyle: 'medium',
  }).format(new Date(iso))
}

export function countdown(unlockAt: string): string {
  const ms = new Date(unlockAt).getTime() - Date.now()
  if (ms <= 0) return 'Đã đến hạn mở'
  const totalMinutes = Math.floor(ms / 60_000)
  const days = Math.floor(totalMinutes / (60 * 24))
  const hours = Math.floor((totalMinutes % (60 * 24)) / 60)
  const minutes = totalMinutes % 60
  if (days > 0) return `${days} ngày ${hours} giờ nữa`
  if (hours > 0) return `${hours} giờ ${minutes} phút nữa`
  const seconds = Math.max(1, Math.floor(ms / 1000))
  if (minutes > 0) return `${minutes} phút nữa`
  return `${seconds} giây nữa`
}
