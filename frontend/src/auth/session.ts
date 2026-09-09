import type { AuthResponse, User } from '../types'

const SESSION_KEY = 'fm.session.v1'
const LEGACY_ACCESS = 'fm.accessToken'
const LEGACY_REFRESH = 'fm.refreshToken'
const LEGACY_USER = 'fm.user'

type SessionV1 = {
  v: 1
  accessToken: string
  refreshToken: string
  user: User
}

let memory: SessionV1 | null | undefined

function readRaw(): string | null {
  try {
    return localStorage.getItem(SESSION_KEY)
  } catch {
    return null
  }
}

function migrateLegacy(): SessionV1 | null {
  try {
    const accessToken = localStorage.getItem(LEGACY_ACCESS)
    const refreshToken = localStorage.getItem(LEGACY_REFRESH)
    const rawUser = localStorage.getItem(LEGACY_USER)
    if (!accessToken || !refreshToken || !rawUser) return null
    const user = JSON.parse(rawUser) as User
    const session: SessionV1 = { v: 1, accessToken, refreshToken, user }
    localStorage.setItem(SESSION_KEY, JSON.stringify(session))
    localStorage.removeItem(LEGACY_ACCESS)
    localStorage.removeItem(LEGACY_REFRESH)
    localStorage.removeItem(LEGACY_USER)
    return session
  } catch {
    return null
  }
}

function readSession(): SessionV1 | null {
  if (memory !== undefined) return memory
  const raw = readRaw()
  if (!raw) {
    memory = migrateLegacy()
    return memory
  }
  try {
    const parsed = JSON.parse(raw) as SessionV1
    memory = parsed?.v === 1 ? parsed : null
  } catch {
    memory = null
  }
  return memory
}

export function getAccessToken(): string | null {
  return readSession()?.accessToken ?? null
}

export function getRefreshToken(): string | null {
  return readSession()?.refreshToken ?? null
}

export function getStoredUser(): User | null {
  return readSession()?.user ?? null
}

export function persistSession(auth: AuthResponse): void {
  const session: SessionV1 = {
    v: 1,
    accessToken: auth.accessToken,
    refreshToken: auth.refreshToken,
    user: auth.user,
  }
  memory = session
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  window.dispatchEvent(new Event('fm:session'))
}

export function clearSession(): void {
  memory = null
  localStorage.removeItem(SESSION_KEY)
  localStorage.removeItem(LEGACY_ACCESS)
  localStorage.removeItem(LEGACY_REFRESH)
  localStorage.removeItem(LEGACY_USER)
  window.dispatchEvent(new Event('fm:logout'))
}
