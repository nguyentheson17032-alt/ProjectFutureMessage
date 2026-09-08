export type MessageStatus = 'LOCKED' | 'AVAILABLE' | 'OPENED' | 'CANCELLED'
export type RecipientType = 'SELF' | 'OTHER'
export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED'
export type UserRole = 'USER' | 'ADMIN'

export type User = {
  id: string
  email: string
  displayName: string
  emailVerified: boolean
  role: UserRole
  enabled: boolean
  createdAt: string
}

export type AuthResponse = {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export type Message = {
  id: string
  senderId: string
  senderDisplayName: string
  recipientEmail: string
  recipientUserId: string | null
  recipientType: RecipientType
  title: string
  content?: string
  unlockAt: string
  status: MessageStatus
  openedAt: string | null
  notificationStatus?: NotificationStatus
  notifiedAt?: string | null
  createdAt: string
  updatedAt?: string
}

export type PageResponse<T> = {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type AdminStats = {
  totalUsers: number
  messagesByStatus: {
    locked: number
    available: number
    opened: number
    cancelled: number
  }
  notifications: {
    pending: number
    sent: number
    failed: number
  }
  unlockingWithin24Hours: number
}

export type AdminUserDetail = User & {
  updatedAt: string
  sentCount: number
  inboxCount: number
}

export type AdminMessageSummary = {
  id: string
  senderId: string
  senderEmail: string
  senderDisplayName: string
  recipientEmail: string
  recipientUserId: string | null
  recipientType: RecipientType
  title: string
  unlockAt: string
  status: MessageStatus
  openedAt: string | null
  notificationStatus: NotificationStatus
  notifiedAt: string | null
  createdAt: string
}

export type AdminMessage = AdminMessageSummary & {
  content?: string
  updatedAt: string
}

export type FieldError = {
  field: string
  message: string
}

export type ApiErrorBody = {
  code: string
  message: string
  timestamp: string
  path: string
  fieldErrors?: FieldError[]
}
