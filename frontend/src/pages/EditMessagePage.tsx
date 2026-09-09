import { mutate } from 'swr'
import { useParams } from 'react-router-dom'
import { useMessage } from '../api/hooks'
import { updateMessage } from '../api/messages'
import { FormError } from '../components/Field'
import { LetterForm } from '../components/LetterForm'
import { PageState } from '../components/EmptyState'
import { DirectionalTransition, TransitionLink, useNavigateTransition } from '../components/transitions'
import { errorMessage } from '../lib/errors'
import { localInputToIso, toDatetimeLocal } from '../lib/time'

export function EditMessagePage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigateTransition()
  const { data: message, error, isLoading } = useMessage(id)

  if (isLoading) {
    return (
      <DirectionalTransition>
        <PageState>Đang mở thư để sửa…</PageState>
      </DirectionalTransition>
    )
  }

  if (!message) {
    return (
      <DirectionalTransition>
        <section className="panel">
          <FormError>{error ? errorMessage(error) : 'Không tìm thấy tin nhắn.'}</FormError>
          <TransitionLink kind="back" to="/sent">
            Quay lại đã gửi
          </TransitionLink>
        </section>
      </DirectionalTransition>
    )
  }

  if (message.status !== 'LOCKED') {
    return (
      <DirectionalTransition>
        <section className="panel">
          <h1>Không còn sửa được</h1>
          <p className="muted">Chỉ sửa khi tin nhắn còn khóa.</p>
          <TransitionLink kind="back" to={`/messages/${message.id}`} className="btn">
            Xem thư
          </TransitionLink>
        </section>
      </DirectionalTransition>
    )
  }

  return (
    <DirectionalTransition>
      <section className="compose">
        <header className="page-head">
          <div>
            <h1>Sửa thư</h1>
            <p className="muted">Chỉ được sửa khi trạng thái còn khóa và bạn là người gửi.</p>
          </div>
        </header>
        <LetterForm.Frame
          initial={{
            title: message.title,
            content: message.content ?? '',
            unlockAt: toDatetimeLocal(message.unlockAt),
          }}
          cancelTo={`/messages/${message.id}`}
          submitLabel="Lưu thay đổi"
          pendingLabel="Đang lưu…"
          onSubmit={async (values) => {
            await updateMessage(message.id, {
              title: values.title,
              content: values.content,
              unlockAt: localInputToIso(values.unlockAt),
            })
            await mutate((key) => Array.isArray(key) && key[0] === 'messages')
            navigate(`/messages/${message.id}`, 'back')
          }}
        >
          <LetterForm.Title />
          <LetterForm.Unlock />
          <LetterForm.Content />
        </LetterForm.Frame>
      </section>
    </DirectionalTransition>
  )
}
