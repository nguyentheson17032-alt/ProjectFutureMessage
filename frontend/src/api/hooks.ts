import useSWR from 'swr'
import {
  fetchAdminStats,
  getAdminMessage,
  getAdminUser,
  listAdminMessages,
  listAdminUsers,
} from './admin'
import { getMessage, listInbox, listSent } from './messages'
import type { MessageStatus, NotificationStatus, UserRole } from '../types'

export function useInbox(page: number) {
  return useSWR(['messages', 'inbox', page], () => listInbox(page))
}

export function useSent(page: number) {
  return useSWR(['messages', 'sent', page], () => listSent(page))
}

export function useMessage(id: string | undefined) {
  return useSWR(id ? ['messages', 'detail', id] : null, () => getMessage(id!))
}

export function useAdminStats() {
  return useSWR(['admin', 'stats'], fetchAdminStats)
}

export function useAdminUsers(params: {
  q?: string
  enabled?: boolean
  role?: UserRole
  page: number
}) {
  return useSWR(['admin', 'users', params], () => listAdminUsers(params))
}

export function useAdminUser(id: string | undefined) {
  return useSWR(id ? ['admin', 'user', id] : null, () => getAdminUser(id!))
}

export function useAdminMessages(params: {
  status?: MessageStatus
  notificationStatus?: NotificationStatus
  senderEmail?: string
  recipientEmail?: string
  page: number
}) {
  return useSWR(['admin', 'messages', params], () => listAdminMessages(params))
}

export function useAdminMessage(id: string | undefined) {
  return useSWR(id ? ['admin', 'message', id] : null, () => getAdminMessage(id!))
}
