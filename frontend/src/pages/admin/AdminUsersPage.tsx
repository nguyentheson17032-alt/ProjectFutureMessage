import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { listAdminUsers, updateUserEnabled } from '../../api/admin'
import { useAuth } from '../../auth/AuthContext'
import { errorMessage } from '../../lib/errors'
import { formatDateTime } from '../../lib/time'
import type { PageResponse, User, UserRole } from '../../types'

function parseEnabled(value: string): boolean | undefined {
  if (value === 'true') return true
  if (value === 'false') return false
  return undefined
}

function parseRole(value: string): UserRole | undefined {
  if (value === 'USER' || value === 'ADMIN') return value
  return undefined
}

export function AdminUsersPage() {
  const { user: me } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const q = searchParams.get('q') ?? ''
  const enabled = searchParams.get('enabled') ?? ''
  const role = searchParams.get('role') ?? ''
  const page = Number(searchParams.get('page') ?? '0') || 0

  const [draftQ, setDraftQ] = useState(q)
  const [data, setData] = useState<PageResponse<User> | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [pendingId, setPendingId] = useState<string | null>(null)

  useEffect(() => {
    setDraftQ(q)
  }, [q])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    listAdminUsers({
      q: q.trim() || undefined,
      enabled: parseEnabled(enabled),
      role: parseRole(role),
      page,
    })
      .then((result) => {
        if (!cancelled) {
          setData(result)
          setError('')
        }
      })
      .catch((err) => {
        if (!cancelled) setError(errorMessage(err))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [q, enabled, role, page])

  function applyFilters(event: FormEvent) {
    event.preventDefault()
    const next = new URLSearchParams()
    if (draftQ.trim()) next.set('q', draftQ.trim())
    if (enabled) next.set('enabled', enabled)
    if (role) next.set('role', role)
    setSearchParams(next)
  }

  function patchFilter(key: string, value: string) {
    const next = new URLSearchParams(searchParams)
    if (value) next.set(key, value)
    else next.delete(key)
    next.delete('page')
    setSearchParams(next)
  }

  function goPage(nextPage: number) {
    const next = new URLSearchParams(searchParams)
    if (nextPage <= 0) next.delete('page')
    else next.set('page', String(nextPage))
    setSearchParams(next)
  }

  async function toggleEnabled(user: User) {
    setPendingId(user.id)
    setError('')
    try {
      const updated = await updateUserEnabled(user.id, !user.enabled)
      setData((current) =>
        current
          ? {
              ...current,
              items: current.items.map((item) => (item.id === updated.id ? { ...item, enabled: updated.enabled } : item)),
            }
          : current,
      )
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setPendingId(null)
    }
  }

  return (
    <section>
      <form className="admin-filters" onSubmit={(event) => void applyFilters(event)}>
        <label>
          Tìm email / tên
          <input value={draftQ} onChange={(e) => setDraftQ(e.target.value)} placeholder="ada@…" />
        </label>
        <label>
          Trạng thái
          <select value={enabled} onChange={(e) => patchFilter('enabled', e.target.value)}>
            <option value="">Tất cả</option>
            <option value="true">Đang bật</option>
            <option value="false">Đã tắt</option>
          </select>
        </label>
        <label>
          Vai trò
          <select value={role} onChange={(e) => patchFilter('role', e.target.value)}>
            <option value="">Tất cả</option>
            <option value="USER">USER</option>
            <option value="ADMIN">ADMIN</option>
          </select>
        </label>
        <button className="btn" type="submit">
          Tìm
        </button>
      </form>

      {loading ? <p className="page-state">Đang tải người dùng…</p> : null}
      {error ? <p className="form-error">{error}</p> : null}

      {!loading && data && data.items.length === 0 ? (
        <p className="muted">Không có user khớp bộ lọc.</p>
      ) : null}

      {data && data.items.length > 0 ? (
        <div className="table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Email</th>
                <th>Tên</th>
                <th>Vai trò</th>
                <th>Tài khoản</th>
                <th>Tạo lúc</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {data.items.map((user) => {
                const self = me?.id === user.id
                return (
                  <tr key={user.id}>
                    <td>
                      <Link to={`/admin/users/${user.id}`}>{user.email}</Link>
                    </td>
                    <td>{user.displayName}</td>
                    <td>
                      <span className={`badge role-${user.role.toLowerCase()}`}>{user.role}</span>
                    </td>
                    <td>{user.enabled ? 'Đang bật' : 'Đã tắt'}</td>
                    <td>{formatDateTime(user.createdAt)}</td>
                    <td className="table-actions">
                      <button
                        type="button"
                        className={user.enabled ? 'btn ghost' : 'btn'}
                        disabled={pendingId === user.id || self}
                        title={self ? 'Không tắt tài khoản đang đăng nhập' : undefined}
                        onClick={() => void toggleEnabled(user)}
                      >
                        {user.enabled ? 'Tắt' : 'Bật'}
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      ) : null}

      {data && data.totalPages > 1 ? (
        <div className="pager">
          <button type="button" disabled={page <= 0} onClick={() => goPage(page - 1)}>
            Trước
          </button>
          <span>
            Trang {page + 1}/{data.totalPages}
          </span>
          <button type="button" disabled={page + 1 >= data.totalPages} onClick={() => goPage(page + 1)}>
            Sau
          </button>
        </div>
      ) : null}
    </section>
  )
}
