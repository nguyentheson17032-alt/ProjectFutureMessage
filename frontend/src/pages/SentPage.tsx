import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listSent } from '../api/messages'
import { EmptyState } from '../components/EmptyState'
import { MessageCard } from '../components/MessageCard'
import { errorMessage } from '../lib/errors'
import type { Message, PageResponse } from '../types'

export function SentPage() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState<PageResponse<Message> | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    listSent(page)
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
  }, [page])

  return (
    <section>
      <header className="page-head">
        <div>
          <h1>Đã gửi</h1>
          <p className="muted">Thư gửi người khác hiện title và nội dung. Thư gửi cho chính mình ẩn đến khi bạn mở.</p>
        </div>
        <Link to="/compose" className="btn">
          Viết thư mới
        </Link>
      </header>
      {loading ? <p className="page-state">Đang lấy thư đã gửi…</p> : null}
      {error ? <p className="form-error">{error}</p> : null}
      {!loading && data && data.items.length === 0 ? (
        <EmptyState
          title="Chưa gửi thư nào"
          body="Viết một lá thư, chọn ngày mở, rồi gửi cho mình hoặc người khác."
          action={
            <Link to="/compose" className="btn">
              Viết thư
            </Link>
          }
        />
      ) : null}
      <div className="letter-grid">
        {data?.items.map((message) => (
          <MessageCard key={message.id} message={message} perspective="sent" />
        ))}
      </div>
      {data && data.totalPages > 1 ? (
        <div className="pager">
          <button type="button" disabled={page <= 0} onClick={() => setPage((p) => p - 1)}>
            Trước
          </button>
          <span>
            Trang {page + 1}/{data.totalPages}
          </span>
          <button
            type="button"
            disabled={page + 1 >= data.totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Sau
          </button>
        </div>
      ) : null}
    </section>
  )
}
