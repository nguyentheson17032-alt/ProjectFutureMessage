import { useState } from 'react'
import { mutate } from 'swr'
import { createMessage } from '../api/messages'
import { Field } from '../components/Field'
import { LetterForm, useRecipientTarget } from '../components/LetterForm'
import { DirectionalTransition, useNavigateTransition } from '../components/transitions'
import { defaultUnlockLocal, localInputToIso } from '../lib/time'

export function ComposePage() {
  const navigate = useNavigateTransition()
  const { target, recipientEmail, setRecipientEmail, setSelf, setOther } = useRecipientTarget()
  const [initial] = useState(() => ({ title: '', content: '', unlockAt: defaultUnlockLocal() }))

  return (
    <DirectionalTransition>
      <section className="compose">
        <header className="page-head">
          <div>
            <h1>Viết thư</h1>
            <p className="muted">
              Thời điểm mở theo múi giờ Việt Nam (UTC+7). Người nhận có thể chưa có tài khoản.
            </p>
          </div>
        </header>
        <LetterForm.Frame
          initial={initial}
          onSubmit={async (values) => {
            const created = await createMessage({
              title: values.title,
              content: values.content,
              unlockAt: localInputToIso(values.unlockAt),
              recipientEmail: target === 'other' ? recipientEmail.trim() : undefined,
            })
            await mutate((key) => Array.isArray(key) && key[0] === 'messages')
            navigate(`/messages/${created.id}`, 'forward')
          }}
        >
          <LetterForm.Title />
          <fieldset className="segmented">
            <legend>Người nhận</legend>
            <label>
              <input type="radio" name="target" checked={target === 'self'} onChange={setSelf} />
              Chính tôi
            </label>
            <label>
              <input type="radio" name="target" checked={target === 'other'} onChange={setOther} />
              Người khác
            </label>
          </fieldset>
          {target === 'other' ? (
            <Field label="Email người nhận" name="recipientEmail">
              <input
                type="email"
                name="recipientEmail"
                required
                autoComplete="off"
                inputMode="email"
                spellCheck={false}
                value={recipientEmail}
                onChange={(e) => setRecipientEmail(e.target.value)}
                placeholder="ban@example.com"
              />
            </Field>
          ) : null}
          <LetterForm.Unlock />
          <LetterForm.Content />
        </LetterForm.Frame>
      </section>
    </DirectionalTransition>
  )
}
