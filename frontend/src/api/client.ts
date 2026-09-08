import { clearSession, getAccessToken, getRefreshToken, persistSession } from '../auth/session'
import { parseApiError, ApiError } from '../lib/errors'
import type { AuthResponse } from '../types'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''

let refreshInFlight: Promise<boolean> | null = null

async function readBody(res: Response): Promise<unknown> {
  if (res.status === 204) return null
  const text = await res.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

async function refreshTokens(): Promise<boolean> {
  if (refreshInFlight) return refreshInFlight
  refreshInFlight = (async () => {
    const refreshToken = getRefreshToken()
    if (!refreshToken) return false
    const res = await fetch(`${API_BASE}/api/v1/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
    const body = await readBody(res)
    if (!res.ok) {
      clearSession()
      return false
    }
    persistSession(body as AuthResponse)
    return true
  })()
  try {
    return await refreshInFlight
  } finally {
    refreshInFlight = null
  }
}

type RequestOptions = {
  method?: string
  body?: unknown
  auth?: boolean
  retry?: boolean
}

export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, auth = true, retry = true } = options
  const headers: Record<string, string> = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth) {
    const token = getAccessToken()
    if (token) headers.Authorization = `Bearer ${token}`
  }

  let res: Response
  try {
    res = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', 'Không kết nối được máy chủ. Kiểm tra backend đang chạy.')
  }

  if (res.status === 401 && auth && retry) {
    const ok = await refreshTokens()
    if (ok) return api<T>(path, { ...options, retry: false })
  }

  const payload = await readBody(res)
  if (!res.ok) throw parseApiError(res.status, payload)
  return payload as T
}
