import { useState, type FormEvent } from 'react'
import { mutate } from 'swr'
import { updateUserEnabled } from '../../api/admin'
import { useAdminUsers } from '../../api/hooks'
import { useAuth } from '../../auth/AuthContext'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { FormError } from '../../components/Field'
import { PageState } from '../../components/EmptyState'
import { Pager } from '../../components/Pager'
import { FadeTransition, TransitionLink } from '../../components/transitions'
import { usePageQuery } from '../../hooks/usePageQuery'
import { errorMessage } from '../../lib/errors'
import { formatDateTime } from '../../lib/time'
import type { User, UserRole } from '../../types'

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
  const { page, setPage, params, patchParams } = usePageQuery()
  const q = params.get('q') ?? ''
  const enabled = params.get('enabled') ?? ''
  const role = params.get('role') ?? ''

  const [draftQ, setDraftQ] = useState(q)
  const [qSeen, setQSeen] = useState(q)
  if (q !== qSeen) {
    setQSeen(q)
    setDraftQ(q)
  }
  const [pendingUser, setPendingUser] = useState<User | null>(null)
  const [pending, setPending] = useState(false)
  const [actionError, setActionError] = useState('')

  const { data, error, isLoading, mutate: mutateUsers } = useAdminUsers({
    q: q.trim() || undefined,
    enabled: parseEnabled(enabled),
    role: parseRole(role),
    page,
  })

  function applyFilters(event: FormEvent) {
    event.preventDefault()
    patchParams((next) => {
      if (draftQ.trim()) next.set('q', draftQ.trim())
      else next.delete('q')
      if (enabled) next.set('enabled', enabled)
      else next.delete('enabled')
      if (role) next.set('role', role)
      else next.delete('role')
    })
  }

  async function confirmToggle() {
    if (!pendingUser) return
    setPending(true)
    setActionError('')
    try {
      const updated = await updateUserEnabled(pendingUser.id, !pendingUser.enabled)
      await mutateUsers(
        (current) =>
          current
            ? {
                ...current,
                items: current.items.map((item) =>
                  item.id === updated.id ? { ...item, enabled: updated.enabled } : item,
                ),
              }
            : current,
        { revalidate: false },
      )
      await mutate((key) => Array.isArray(key) && key[0] === 'admin')
      setPendingUser(null)
    } catch (err) {
      setActionError(errorMessage(err))
    } finally {
      setPending(false)
    }
  }

  return (
    <FadeTransition>
      <section>
        <form className="admin-filters" onSubmit={(event) => void applyFilters(event)}>
          <label>
            Tìm email / tên
            <input
              name="q"
              type="search"
              autoComplete="off"
              spellCheck={false}
              value={draftQ}
              onChange={(e) => setDraftQ(e.target.value)}
              placeholder="ada@…"
            />
          </label>
          <label>
            Trạng thái
            <select
              name="enabled"
              value={enabled}
              onChange={(e) =>
                patchParams((next) => {
                  if (e.target.value) next.set('enabled', e.target.value)
                  else next.delete('enabled')
                })
              }
            >
              <option value="">Tất cả</option>
              <option value="true">Đang bật</option>
              <option value="false">Đã tắt</option>
            </select>
          </label>
          <label>
            Vai trò
            <select
              name="role"
              value={role}
              onChange={(e) =>
                patchParams((next) => {
                  if (e.target.value) next.set('role', e.target.value)
                  else next.delete('role')
                })
              }
            >
              <option value="">Tất cả</option>
              <option value="USER">USER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </label>
          <button className="btn" type="submit">
            Tìm tài khoản
          </button>
        </form>

        {isLoading ? <PageState>Đang tải người dùng…</PageState> : null}
        {error ? <FormError>{errorMessage(error)}</FormError> : null}
        <FormError>{actionError}</FormError>

        {!isLoading && data && data.items.length === 0 ? (
          <p className="muted">Không có tài khoản khớp bộ lọc.</p>
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
                  <th>
                    <span className="sr-only">Thao tác</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.items.map((user) => {
                  const self = me?.id === user.id
                  return (
                    <tr key={user.id}>
                      <td>
                        <TransitionLink kind="forward" to={`/admin/users/${user.id}`}>
                          {user.email}
                        </TransitionLink>
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
                          disabled={self}
                          title={self ? 'Không tắt tài khoản đang đăng nhập' : undefined}
                          onClick={() => setPendingUser(user)}
                        >
                          {user.enabled ? 'Tắt tài khoản' : 'Bật tài khoản'}
                        </button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        ) : null}

        <Pager page={page} totalPages={data?.totalPages ?? 0} onPage={setPage} />
      </section>
      <ConfirmDialog
        open={Boolean(pendingUser)}
        title={pendingUser?.enabled ? 'Tắt tài khoản này?' : 'Bật tài khoản này?'}
        confirmLabel={pendingUser?.enabled ? 'Tắt tài khoản' : 'Bật tài khoản'}
        pending={pending}
        pendingLabel="Đang cập nhật…"
        danger={pendingUser?.enabled}
        onCancel={() => setPendingUser(null)}
        onConfirm={() => void confirmToggle()}
      >
        <p>
          {pendingUser?.enabled
            ? `${pendingUser.email} sẽ không đăng nhập hoặc làm mới phiên được.`
            : `${pendingUser?.email ?? ''} có thể đăng nhập lại.`}
        </p>
      </ConfirmDialog>
    </FadeTransition>
  )
}
