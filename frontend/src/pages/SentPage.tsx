import { ViewTransition } from 'react'
import { useSent } from '../api/hooks'
import { EmptyState, PageState } from '../components/EmptyState'
import { FormError } from '../components/Field'
import { MessageCard } from '../components/MessageCard'
import { Pager } from '../components/Pager'
import { DirectionalTransition, TransitionLink } from '../components/transitions'
import { usePageQuery } from '../hooks/usePageQuery'
import { errorMessage } from '../lib/errors'

export function SentPage() {
  const { page, setPage } = usePageQuery()
  const { data, error, isLoading } = useSent(page)

  return (
    <DirectionalTransition>
      <section>
        <header className="page-head">
          <div>
            <h1>Đã gửi</h1>
            <p className="muted">
              Thư gửi người khác hiện tiêu đề và nội dung. Thư gửi cho chính mình ẩn đến khi bạn mở.
            </p>
          </div>
          <TransitionLink kind="forward" to="/compose" className="btn">
            Viết thư mới
          </TransitionLink>
        </header>
        {isLoading ? <PageState>Đang lấy thư đã gửi…</PageState> : null}
        {error ? <FormError>{errorMessage(error)}</FormError> : null}
        {!isLoading && data && data.items.length === 0 ? (
          <EmptyState
            title="Chưa gửi thư nào"
            body="Viết một lá thư, chọn ngày mở, rồi gửi cho mình hoặc người khác."
            action={
              <TransitionLink kind="forward" to="/compose" className="btn">
                Viết thư
              </TransitionLink>
            }
          />
        ) : null}
        <ViewTransition key={String(page)} enter="slide-up" default="none">
          <div className="letter-grid">
            {data?.items.map((message) => (
              <MessageCard key={message.id} message={message} perspective="sent" />
            ))}
          </div>
        </ViewTransition>
        <Pager page={page} totalPages={data?.totalPages ?? 0} onPage={setPage} />
      </section>
    </DirectionalTransition>
  )
}
