import { useState, type FormEvent } from 'react'
import { useAuth } from '../auth/AuthContext'
import { Field, FormError } from '../components/Field'
import { PasswordInput } from '../components/PasswordInput'
import { DirectionalTransition, TransitionLink, useNavigateTransition } from '../components/transitions'
import { focusFirstInvalid } from '../lib/form'
import { errorMessage, fieldErrorMap } from '../lib/errors'

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigateTransition()
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [fields, setFields] = useState<Record<string, string>>({})
  const [pending, setPending] = useState(false)

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setFields({})
    setPending(true)
    try {
      await register(email.trim(), password, displayName.trim())
      navigate('/inbox', 'forward', { replace: true })
    } catch (err) {
      const form = event.currentTarget
      setFields(fieldErrorMap(err))
      setError(errorMessage(err))
      requestAnimationFrame(() => {
        requestAnimationFrame(() => focusFirstInvalid(form))
      })
    } finally {
      setPending(false)
    }
  }

  return (
    <DirectionalTransition>
      <section className="auth-panel">
        <h1>Tạo tài khoản</h1>
        <p className="muted">Email dùng để nhận thư, kể cả khi người khác gửi trước khi bạn đăng ký.</p>
        <form onSubmit={(event) => void onSubmit(event)} className="stack">
          <Field label="Tên hiển thị" name="displayName" error={fields.displayName}>
            <input
              type="text"
              name="displayName"
              maxLength={100}
              required
              autoComplete="name"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
            />
          </Field>
          <Field label="Email" name="email" error={fields.email}>
            <input
              type="email"
              name="email"
              required
              autoComplete="email"
              inputMode="email"
              spellCheck={false}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </Field>
          <Field label="Mật khẩu" name="password" hint="Tối thiểu 8 ký tự." error={fields.password}>
            <PasswordInput
              name="password"
              autoComplete="new-password"
              required
              minLength={8}
              maxLength={72}
              value={password}
              onChange={setPassword}
            />
          </Field>
          <FormError>{error}</FormError>
          <button className="btn" type="submit" disabled={pending}>
            {pending ? 'Đang tạo…' : 'Tạo tài khoản'}
          </button>
        </form>
        <p className="switch-auth">
          Đã có tài khoản? <TransitionLink kind="lateral" to="/login">Đăng nhập</TransitionLink>
        </p>
      </section>
    </DirectionalTransition>
  )
}
