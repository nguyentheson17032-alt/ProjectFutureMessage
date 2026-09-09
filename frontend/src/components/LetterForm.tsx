import {
  createContext,
  use,
  useCallback,
  useMemo,
  useState,
  type FormEvent,
  type ReactNode,
} from 'react'
import { focusFirstInvalid } from '../lib/form'
import { errorMessage, fieldErrorMap } from '../lib/errors'
import { useUnsavedChanges } from '../hooks/useUnsavedChanges'
import { ConfirmDialog } from './ConfirmDialog'
import { Field, FormError } from './Field'
import { UnlockAtPicker } from './UnlockAtPicker'
import { TransitionLink } from './transitions'

type LetterValues = {
  title: string
  content: string
  unlockAt: string
}

type LetterFormContextValue = LetterValues & {
  setTitle: (value: string) => void
  setContent: (value: string) => void
  setUnlockAt: (value: string) => void
  pending: boolean
  error: string
  fields: Record<string, string>
}

const LetterFormContext = createContext<LetterFormContextValue | null>(null)

function useLetterForm() {
  const ctx = use(LetterFormContext)
  if (!ctx) throw new Error('LetterForm parts must be used inside LetterForm.Frame')
  return ctx
}

function Frame({
  initial,
  cancelTo,
  submitLabel,
  pendingLabel,
  onSubmit,
  children,
}: {
  initial: LetterValues
  cancelTo?: string
  submitLabel?: string
  pendingLabel?: string
  onSubmit: (values: LetterValues) => Promise<void>
  children: ReactNode
}) {
  const [title, setTitle] = useState(initial.title)
  const [content, setContent] = useState(initial.content)
  const [unlockAt, setUnlockAt] = useState(initial.unlockAt)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')
  const [fields, setFields] = useState<Record<string, string>>({})

  const dirty =
    !pending &&
    (title !== initial.title || content !== initial.content || unlockAt !== initial.unlockAt)
  const blocker = useUnsavedChanges(dirty)

  const value = useMemo(
    () => ({
      title,
      content,
      unlockAt,
      setTitle,
      setContent,
      setUnlockAt,
      pending,
      error,
      fields,
    }),
    [title, content, unlockAt, pending, error, fields],
  )

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setFields({})
    setPending(true)
    try {
      await onSubmit({
        title: title.trim(),
        content: content.trim(),
        unlockAt,
      })
    } catch (err) {
      const nextFields = fieldErrorMap(err)
      const form = event.currentTarget
      setFields(nextFields)
      setError(errorMessage(err))
      requestAnimationFrame(() => {
        requestAnimationFrame(() => focusFirstInvalid(form))
      })
    } finally {
      setPending(false)
    }
  }

  return (
    <LetterFormContext value={value}>
      <form onSubmit={(event) => void handleSubmit(event)} className="paper-form" autoComplete="off">
        {children}
        <FormError>{error}</FormError>
        {cancelTo ? (
          <div className="row-actions">
            <Submit label={submitLabel} pendingLabel={pendingLabel} />
            <TransitionLink kind="back" to={cancelTo} className="btn ghost">
              Hủy chỉnh sửa
            </TransitionLink>
          </div>
        ) : (
          <Submit label={submitLabel} pendingLabel={pendingLabel} />
        )}
      </form>
      <ConfirmDialog
        open={blocker.state === 'blocked'}
        title="Rời trang khi chưa lưu?"
        confirmLabel="Rời trang"
        danger
        onCancel={() => blocker.reset?.()}
        onConfirm={() => blocker.proceed?.()}
      >
        <p>Thay đổi trên thư này chưa được lưu. Rời trang sẽ mất nội dung vừa viết.</p>
      </ConfirmDialog>
    </LetterFormContext>
  )
}

function Title() {
  const { title, setTitle, fields } = useLetterForm()
  return (
    <Field label="Tiêu đề" name="title" error={fields.title}>
      <input
        type="text"
        name="title"
        maxLength={200}
        required
        autoComplete="off"
        placeholder="Gửi tôi của năm sau…"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
      />
    </Field>
  )
}

function Content() {
  const { content, setContent, fields } = useLetterForm()
  return (
    <Field label="Nội dung" name="content" error={fields.content}>
      <textarea
        name="content"
        required
        maxLength={20000}
        rows={12}
        autoComplete="off"
        placeholder="Những điều bạn muốn đọc lại sau này…"
        value={content}
        onChange={(e) => setContent(e.target.value)}
      />
    </Field>
  )
}

function Unlock() {
  const { unlockAt, setUnlockAt, fields } = useLetterForm()
  return <UnlockAtPicker value={unlockAt} onChange={setUnlockAt} error={fields.unlockAt} />
}

function Submit({
  label = 'Niêm phong và gửi',
  pendingLabel = 'Đang niêm phong…',
}: {
  label?: string
  pendingLabel?: string
}) {
  const { pending } = useLetterForm()
  return (
    <button className="btn" type="submit" disabled={pending}>
      {pending ? pendingLabel : label}
    </button>
  )
}

export const LetterForm = {
  Frame,
  Title,
  Content,
  Unlock,
  Submit,
}

export function useRecipientTarget() {
  const [target, setTarget] = useState<'self' | 'other'>('self')
  const [recipientEmail, setRecipientEmail] = useState('')
  const setSelf = useCallback(() => setTarget('self'), [])
  const setOther = useCallback(() => setTarget('other'), [])
  return { target, recipientEmail, setRecipientEmail, setSelf, setOther }
}
