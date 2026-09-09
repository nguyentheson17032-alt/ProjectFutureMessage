import { useState, type FormEvent } from 'react'
import { useAdminMessages } from '../../api/hooks'
import { FormError } from '../../components/Field'
import { PageState } from '../../components/EmptyState'
import { NotificationBadge } from '../../components/NotificationBadge'
import { Pager } from '../../components/Pager'
import { StatusBadge } from '../../components/StatusBadge'
import { FadeTransition, TransitionLink } from '../../components/transitions'
import { usePageQuery } from '../../hooks/usePageQuery'
import { errorMessage } from '../../lib/errors'
import { formatDateTime } from '../../lib/time'
import type { MessageStatus, NotificationStatus } from '../../types'

function parseStatus(value: string): MessageStatus | undefined {
  if (value === 'LOCKED' || value === 'AVAILABLE' || value === 'OPENED' || value === 'CANCELLED') return value
  return undefined
}

function parseNotification(value: string): NotificationStatus | undefined {
  if (value === 'PENDING' || value === 'SENT' || value === 'FAILED') return value
  return undefined
}

export function AdminMessagesPage() {
  const { page, setPage, params, patchParams } = usePageQuery()
  const status = params.get('status') ?? ''
  const notificationStatus = params.get('notificationStatus') ?? ''
  const senderEmail = params.get('senderEmail') ?? ''
  const recipientEmail = params.get('recipientEmail') ?? ''

  const [draftSender, setDraftSender] = useState(senderEmail)
  const [draftRecipient, setDraftRecipient] = useState(recipientEmail)
  const [emailSeen, setEmailSeen] = useState(`${senderEmail}\0${recipientEmail}`)
  const emailKey = `${senderEmail}\0${recipientEmail}`
  if (emailKey !== emailSeen) {
    setEmailSeen(emailKey)
    setDraftSender(senderEmail)
    setDraftRecipient(recipientEmail)
  }

  const { data, error, isLoading } = useAdminMessages({
    status: parseStatus(status),
    notificationStatus: parseNotification(notificationStatus),
    senderEmail: senderEmail.trim() || undefined,
    recipientEmail: recipientEmail.trim() || undefined,
    page,
  })

  function applyEmails(event: FormEvent) {
    event.preventDefault()
    patchParams((next) => {
      if (draftSender.trim()) next.set('senderEmail', draftSender.trim())
      else next.delete('senderEmail')
      if (draftRecipient.trim()) next.set('recipientEmail', draftRecipient.trim())
      else next.delete('recipientEmail')
      if (status) next.set('status', status)
      else next.delete('status')
      if (notificationStatus) next.set('notificationStatus', notificationStatus)
      else next.delete('notificationStatus')
    })
  }

  return (
    <FadeTransition>
      <section>
        <form className="admin-filters" onSubmit={(event) => void applyEmails(event)}>
          <label>
            Trạng thái thư
            <select
              name="status"
              value={status}
              onChange={(e) =>
                patchParams((next) => {
                  if (e.target.value) next.set('status', e.target.value)
                  else next.delete('status')
                })
              }
            >
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
              name="notificationStatus"
              value={notificationStatus}
              onChange={(e) =>
                patchParams((next) => {
                  if (e.target.value) next.set('notificationStatus', e.target.value)
                  else next.delete('notificationStatus')
                })
              }
            >
              <option value="">Tất cả</option>
              <option value="PENDING">PENDING</option>
              <option value="SENT">SENT</option>
              <option value="FAILED">FAILED</option>
            </select>
          </label>
          <label>
            Email người gửi
            <input
              name="senderEmail"
              type="email"
              autoComplete="off"
              spellCheck={false}
              value={draftSender}
              onChange={(e) => setDraftSender(e.target.value)}
              placeholder="ada@…"
            />
          </label>
          <label>
            Email người nhận
            <input
              name="recipientEmail"
              type="email"
              autoComplete="off"
              spellCheck={false}
              value={draftRecipient}
              onChange={(e) => setDraftRecipient(e.target.value)}
              placeholder="bob@…"
            />
          </label>
          <button className="btn" type="submit">
            Lọc tin nhắn
          </button>
        </form>

        {isLoading ? <PageState>Đang tải tin nhắn…</PageState> : null}
        {error ? <FormError>{errorMessage(error)}</FormError> : null}

        {!isLoading && data && data.items.length === 0 ? (
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
                  <tr
                    key={message.id}
                    className={message.notificationStatus === 'FAILED' ? 'is-alert' : undefined}
                  >
                    <td>
                      <TransitionLink kind="forward" to={`/admin/messages/${message.id}`}>
                        {message.title}
                      </TransitionLink>
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

        <Pager page={page} totalPages={data?.totalPages ?? 0} onPage={setPage} />
      </section>
    </FadeTransition>
  )
}
