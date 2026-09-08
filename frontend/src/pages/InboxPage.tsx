import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listInbox } from '../api/messages'
import { EmptyState } from '../components/EmptyState'
import { MessageCard } from '../components/MessageCard'
import { errorMessage } from '../lib/errors'
import type { Message, PageResponse } from '../types'

export function InboxPage() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState<PageResponse<Message> | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    listInbox(page)
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
          <h1>Hộp thư</h1>
          <p className="muted">Phong bì khóa hoặc sẵn sàng mở ẩn tiêu đề và nội dung. Chỉ thư đã mở mới hiện chữ.</p>
        </div>
      </header>
      {loading ? <p className="page-state">Đang lấy thư…</p> : null}
      {error ? <p className="form-error">{error}</p> : null}
      {!loading && data && data.items.length === 0 ? (
        <EmptyState
          title="Hộp thư còn trống"
          body="Khi ai đó gửi thư tới email của bạn — hoặc bạn gửi cho chính mình — chúng sẽ hiện ở đây."
          action={
            <Link to="/compose" className="btn">
              Viết thư cho tương lai
            </Link>
          }
        />
      ) : null}
      <div className="letter-grid">
        {data?.items.map((message) => (
          <MessageCard key={message.id} message={message} perspective="inbox" />
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
