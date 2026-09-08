import { api } from './client'
import type { Message, PageResponse } from '../types'

export type CreateMessageBody = {
  title: string
  content: string
  unlockAt: string
  recipientEmail?: string
}

export type UpdateMessageBody = {
  title?: string
  content?: string
  unlockAt?: string
}

export function listInbox(page = 0, size = 20) {
  return api<PageResponse<Message>>(`/api/v1/messages/inbox?page=${page}&size=${size}`)
}

export function listSent(page = 0, size = 20) {
  return api<PageResponse<Message>>(`/api/v1/messages/sent?page=${page}&size=${size}`)
}

export function getMessage(id: string) {
  return api<Message>(`/api/v1/messages/${id}`)
}

export function createMessage(body: CreateMessageBody) {
  return api<Message>('/api/v1/messages', { method: 'POST', body })
}

export function updateMessage(id: string, body: UpdateMessageBody) {
  return api<Message>(`/api/v1/messages/${id}`, { method: 'PATCH', body })
}

export function cancelMessage(id: string) {
  return api<void>(`/api/v1/messages/${id}`, { method: 'DELETE' })
}

export function openMessage(id: string) {
  return api<Message>(`/api/v1/messages/${id}/open`, { method: 'POST' })
}
