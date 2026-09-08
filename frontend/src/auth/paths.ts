import type { User } from '../types'

export function homePath(user: User | null): string {
  if (!user) return '/'
  return user.role === 'ADMIN' ? '/admin' : '/inbox'
}

export function afterLoginPath(user: User, intended?: string): string {
  if (user.role === 'ADMIN') {
    if (intended?.startsWith('/admin')) return intended
    return '/admin'
  }
  if (intended && intended.startsWith('/') && !intended.startsWith('/login') && !intended.startsWith('/register')) {
    return intended
  }
  return '/inbox'
}
