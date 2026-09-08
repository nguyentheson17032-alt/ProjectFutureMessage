import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { createMessage } from '../api/messages'
import { Field } from '../components/Field'
import { UnlockAtPicker } from '../components/UnlockAtPicker'
import { errorMessage, fieldErrorMap } from '../lib/errors'
import { defaultUnlockLocal, localInputToIso } from '../lib/time'

export function ComposePage() {
  const navigate = useNavigate()
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [unlockAt, setUnlockAt] = useState(defaultUnlockLocal)
  const [target, setTarget] = useState<'self' | 'other'>('self')
  const [recipientEmail, setRecipientEmail] = useState('')
  const [error, setError] = useState('')
  const [fields, setFields] = useState<Record<string, string>>({})
  const [pending, setPending] = useState(false)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setFields({})
    setPending(true)
    try {
      const created = await createMessage({
        title: title.trim(),
        content: content.trim(),
        unlockAt: localInputToIso(unlockAt),
        recipientEmail: target === 'other' ? recipientEmail.trim() : undefined,
      })
      navigate(`/messages/${created.id}`)
    } catch (err) {
      setFields(fieldErrorMap(err))
      setError(errorMessage(err))
    } finally {
      setPending(false)
    }
  }

  return (
    <section className="compose">
      <header className="page-head">
        <div>
          <h1>Viết thư</h1>
          <p className="muted">Thời điểm mở theo múi giờ Việt Nam (UTC+7). Người nhận có thể chưa có tài khoản.</p>
        </div>
      </header>
      <form onSubmit={(event) => void onSubmit(event)} className="paper-form">
        <Field label="Tiêu đề" error={fields.title}>
          <input
            type="text"
            maxLength={200}
            required
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Gửi tôi của năm sau…"
          />
        </Field>
        <fieldset className="segmented">
          <legend>Người nhận</legend>
          <label>
            <input
              type="radio"
              name="target"
              checked={target === 'self'}
              onChange={() => setTarget('self')}
            />
            Chính tôi
          </label>
          <label>
            <input
              type="radio"
              name="target"
              checked={target === 'other'}
              onChange={() => setTarget('other')}
            />
            Người khác
          </label>
        </fieldset>
        {target === 'other' ? (
          <Field label="Email người nhận" error={fields.recipientEmail}>
            <input
              type="email"
              required
              value={recipientEmail}
              onChange={(e) => setRecipientEmail(e.target.value)}
              placeholder="ban@example.com"
            />
          </Field>
        ) : null}
        <UnlockAtPicker value={unlockAt} onChange={setUnlockAt} error={fields.unlockAt} />
        <Field label="Nội dung" error={fields.content}>
          <textarea
            required
            maxLength={20000}
            rows={12}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="Những điều bạn muốn đọc lại sau này…"
          />
        </Field>
        {error ? <p className="form-error">{error}</p> : null}
        <button className="btn" type="submit" disabled={pending}>
          {pending ? 'Đang niêm phong…' : 'Niêm phong và gửi'}
        </button>
      </form>
    </section>
  )
}
