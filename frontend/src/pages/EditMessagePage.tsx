import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getMessage, updateMessage } from '../api/messages'
import { Field } from '../components/Field'
import { UnlockAtPicker } from '../components/UnlockAtPicker'
import { errorMessage, fieldErrorMap } from '../lib/errors'
import { localInputToIso, toDatetimeLocal } from '../lib/time'
import type { Message } from '../types'

export function EditMessagePage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [message, setMessage] = useState<Message | null>(null)
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [unlockAt, setUnlockAt] = useState('')
  const [error, setError] = useState('')
  const [fields, setFields] = useState<Record<string, string>>({})
  const [pending, setPending] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!id) return
    let cancelled = false
    getMessage(id)
      .then((item) => {
        if (cancelled) return
        setMessage(item)
        setTitle(item.title)
        setContent(item.content ?? '')
        setUnlockAt(toDatetimeLocal(item.unlockAt))
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
  }, [id])

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!id) return
    setError('')
    setFields({})
    setPending(true)
    try {
      await updateMessage(id, {
        title: title.trim(),
        content: content.trim(),
        unlockAt: localInputToIso(unlockAt),
      })
      navigate(`/messages/${id}`)
    } catch (err) {
      setFields(fieldErrorMap(err))
      setError(errorMessage(err))
    } finally {
      setPending(false)
    }
  }

  if (loading) return <p className="page-state">Đang mở thư để sửa…</p>
  if (!message) {
    return (
      <section className="panel">
        <p className="form-error">{error || 'Không tìm thấy tin nhắn.'}</p>
        <Link to="/sent">Quay lại đã gửi</Link>
      </section>
    )
  }

  if (message.status !== 'LOCKED') {
    return (
      <section className="panel">
        <h1>Không còn sửa được</h1>
        <p className="muted">Chỉ sửa khi tin nhắn còn khóa.</p>
        <Link to={`/messages/${message.id}`} className="btn">
          Xem thư
        </Link>
      </section>
    )
  }

  return (
    <section className="compose">
      <header className="page-head">
        <div>
          <h1>Sửa thư</h1>
          <p className="muted">Chỉ được sửa khi trạng thái còn khóa và bạn là người gửi.</p>
        </div>
      </header>
      <form onSubmit={(event) => void onSubmit(event)} className="paper-form">
        <Field label="Tiêu đề" error={fields.title}>
          <input type="text" maxLength={200} required value={title} onChange={(e) => setTitle(e.target.value)} />
        </Field>
        <UnlockAtPicker value={unlockAt} onChange={setUnlockAt} error={fields.unlockAt} />
        <Field label="Nội dung" error={fields.content}>
          <textarea required maxLength={20000} rows={12} value={content} onChange={(e) => setContent(e.target.value)} />
        </Field>
        {error ? <p className="form-error">{error}</p> : null}
        <div className="row-actions">
          <button className="btn" type="submit" disabled={pending}>
            {pending ? 'Đang lưu…' : 'Lưu thay đổi'}
          </button>
          <Link to={`/messages/${message.id}`} className="btn ghost">
            Hủy
          </Link>
        </div>
      </form>
    </section>
  )
}
