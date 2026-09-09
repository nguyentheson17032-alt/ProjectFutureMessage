import { ViewTransition } from 'react'
import { useInbox } from '../api/hooks'
import { EmptyState, PageState } from '../components/EmptyState'
import { FormError } from '../components/Field'
import { MessageCard } from '../components/MessageCard'
import { Pager } from '../components/Pager'
import { DirectionalTransition, TransitionLink } from '../components/transitions'
import { usePageQuery } from '../hooks/usePageQuery'
import { errorMessage } from '../lib/errors'

export function InboxPage() {
  const { page, setPage } = usePageQuery()
  const { data, error, isLoading } = useInbox(page)

  return (
    <DirectionalTransition>
      <section>
        <header className="page-head">
          <div>
            <h1>Hộp thư</h1>
            <p className="muted">
              Phong bì khóa hoặc sẵn sàng mở ẩn tiêu đề và nội dung. Chỉ thư đã mở mới hiện chữ.
            </p>
          </div>
        </header>
        {isLoading ? <PageState>Đang lấy thư…</PageState> : null}
        {error ? <FormError>{errorMessage(error)}</FormError> : null}
        {!isLoading && data && data.items.length === 0 ? (
          <EmptyState
            title="Hộp thư còn trống"
            body="Khi ai đó gửi thư tới email của bạn, hoặc bạn gửi cho chính mình, thư sẽ hiện ở đây."
            action={
              <TransitionLink kind="forward" to="/compose" className="btn">
                Viết thư cho tương lai
              </TransitionLink>
            }
          />
        ) : null}
        <ViewTransition key={String(page)} enter="slide-up" default="none">
          <div className="letter-grid">
            {data?.items.map((message) => (
              <MessageCard key={message.id} message={message} perspective="inbox" />
            ))}
          </div>
        </ViewTransition>
        <Pager page={page} totalPages={data?.totalPages ?? 0} onPage={setPage} />
      </section>
    </DirectionalTransition>
  )
}
