import { api } from './client'
import type {
  AdminMessage,
  AdminMessageSummary,
  AdminStats,
  AdminUserDetail,
  MessageStatus,
  NotificationStatus,
  PageResponse,
  User,
  UserRole,
} from '../types'

function query(params: Record<string, string | number | boolean | undefined>): string {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === '') continue
    search.set(key, String(value))
  }
  const encoded = search.toString()
  return encoded ? `?${encoded}` : ''
}

export function fetchAdminStats() {
  return api<AdminStats>('/api/v1/admin/stats')
}

export function listAdminUsers(params: {
  q?: string
  enabled?: boolean
  role?: UserRole
  page?: number
  size?: number
}) {
  return api<PageResponse<User>>(
    `/api/v1/admin/users${query({
      q: params.q,
      enabled: params.enabled,
      role: params.role,
      page: params.page ?? 0,
      size: params.size ?? 20,
    })}`,
  )
}

export function getAdminUser(id: string) {
  return api<AdminUserDetail>(`/api/v1/admin/users/${id}`)
}

export function updateUserEnabled(id: string, enabled: boolean) {
  return api<AdminUserDetail>(`/api/v1/admin/users/${id}`, {
    method: 'PATCH',
    body: { enabled },
  })
}

export function listAdminMessages(params: {
  status?: MessageStatus
  notificationStatus?: NotificationStatus
  senderEmail?: string
  recipientEmail?: string
  page?: number
  size?: number
}) {
  return api<PageResponse<AdminMessageSummary>>(
    `/api/v1/admin/messages${query({
      status: params.status,
      notificationStatus: params.notificationStatus,
      senderEmail: params.senderEmail,
      recipientEmail: params.recipientEmail,
      page: params.page ?? 0,
      size: params.size ?? 20,
    })}`,
  )
}

export function getAdminMessage(id: string) {
  return api<AdminMessage>(`/api/v1/admin/messages/${id}`)
}

export function retryNotification(id: string) {
  return api<AdminMessage>(`/api/v1/admin/messages/${id}/retry-notification`, { method: 'POST' })
}
