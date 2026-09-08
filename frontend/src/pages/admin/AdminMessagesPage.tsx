import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { listAdminMessages } from '../../api/admin'
import { NotificationBadge } from '../../components/NotificationBadge'
import { StatusBadge } from '../../components/StatusBadge'
import { errorMessage } from '../../lib/errors'
import { formatDateTime } from '../../lib/time'
import type { AdminMessageSummary, MessageStatus, NotificationStatus, PageResponse } from '../../types'

function parseStatus(value: string): MessageStatus | undefined {
  if (value === 'LOCKED' || value === 'AVAILABLE' || value === 'OPENED' || value === 'CANCELLED') return value
  return undefined
}

function parseNotification(value: string): NotificationStatus | undefined {
  if (value === 'PENDING' || value === 'SENT' || value === 'FAILED') return value
  return undefined
}

export function AdminMessagesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const status = searchParams.get('status') ?? ''
  const notificationStatus = searchParams.get('notificationStatus') ?? ''
  const senderEmail = searchParams.get('senderEmail') ?? ''
  const recipientEmail = searchParams.get('recipientEmail') ?? ''
  const page = Number(searchParams.get('page') ?? '0') || 0

  const [draftSender, setDraftSender] = useState(senderEmail)
  const [draftRecipient, setDraftRecipient] = useState(recipientEmail)
  const [data, setData] = useState<PageResponse<AdminMessageSummary> | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setDraftSender(senderEmail)
    setDraftRecipient(recipientEmail)
  }, [senderEmail, recipientEmail])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    listAdminMessages({
      status: parseStatus(status),
      notificationStatus: parseNotification(notificationStatus),
      senderEmail: senderEmail.trim() || undefined,
      recipientEmail: recipientEmail.trim() || undefined,
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
  }, [status, notificationStatus, senderEmail, recipientEmail, page])

  function writeParams(next: URLSearchParams) {
    next.delete('page')
    setSearchParams(next)
  }

  function applyEmails(event: FormEvent) {
    event.preventDefault()
    const next = new URLSearchParams(searchParams)
    if (draftSender.trim()) next.set('senderEmail', draftSender.trim())
    else next.delete('senderEmail')
    if (draftRecipient.trim()) next.set('recipientEmail', draftRecipient.trim())
    else next.delete('recipientEmail')
    writeParams(next)
  }

  function patchFilter(key: string, value: string) {
    const next = new URLSearchParams(searchParams)
    if (value) next.set(key, value)
    else next.delete(key)
    writeParams(next)
  }

  function goPage(nextPage: number) {
    const next = new URLSearchParams(searchParams)
    if (nextPage <= 0) next.delete('page')
    else next.set('page', String(nextPage))
    setSearchParams(next)
  }

  return (
    <section>
      <form className="admin-filters" onSubmit={(event) => void applyEmails(event)}>
        <label>
          Trạng thái thư
          <select value={status} onChange={(e) => patchFilter('status', e.target.value)}>
            <option value="">Tất cả</option>
            <option value="LOCKED">LOCKED</option>
            <option value="AVAILABLE">AVAILABLE</option>
            <option value="OPENED">OPENED</option>
            <option value="CANCELLED">CANCELLED</option>
          </select>
        </label>
        <label>
          Email thông báo
          <select
            value={notificationStatus}
            onChange={(e) => patchFilter('notificationStatus', e.target.value)}
          >
            <option value="">Tất cả</option>
            <option value="PENDING">PENDING</option>
            <option value="SENT">SENT</option>
            <option value="FAILED">FAILED</option>
          </select>
        </label>
        <label>
          Email người gửi
          <input value={draftSender} onChange={(e) => setDraftSender(e.target.value)} placeholder="ada@…" />
        </label>
        <label>
          Email người nhận
          <input
            value={draftRecipient}
            onChange={(e) => setDraftRecipient(e.target.value)}
            placeholder="bob@…"
          />
        </label>
        <button className="btn" type="submit">
          Lọc
        </button>
      </form>

      {loading ? <p className="page-state">Đang tải tin nhắn…</p> : null}
      {error ? <p className="form-error">{error}</p> : null}

      {!loading && data && data.items.length === 0 ? (
        <p className="muted">Không có thư khớp bộ lọc.</p>
      ) : null}

      {data && data.items.length > 0 ? (
        <div className="table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Tiêu đề</th>
                <th>Người gửi</th>
                <th>Người nhận</th>
                <th>Trạng thái</th>
                <th>Email</th>
                <th>Mở lúc</th>
              </tr>
            </thead>
            <tbody>
              {data.items.map((message) => (
                <tr key={message.id} className={message.notificationStatus === 'FAILED' ? 'is-alert' : undefined}>
                  <td>
                    <Link to={`/admin/messages/${message.id}`}>{message.title}</Link>
                  </td>
                  <td>{message.senderEmail}</td>
                  <td>{message.recipientEmail}</td>
                  <td>
                    <StatusBadge status={message.status} />
                  </td>
                  <td>
                    <NotificationBadge status={message.notificationStatus} />
                  </td>
                  <td>{formatDateTime(message.unlockAt)}</td>
                </tr>
              ))}
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
