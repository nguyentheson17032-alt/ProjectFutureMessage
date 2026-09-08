import { api } from './client'
import type { AuthResponse, User } from '../types'

export function register(email: string, password: string, displayName: string) {
  return api<AuthResponse>('/api/v1/auth/register', {
    method: 'POST',
    auth: false,
    body: { email, password, displayName },
  })
}

export function login(email: string, password: string) {
  return api<AuthResponse>('/api/v1/auth/login', {
    method: 'POST',
    auth: false,
    body: { email, password },
  })
}

export function logout(refreshToken: string) {
  return api<void>('/api/v1/auth/logout', {
    method: 'POST',
    auth: false,
    body: { refreshToken },
  })
}

export function fetchMe() {
  return api<User>('/api/v1/users/me')
}
