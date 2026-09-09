import { useState, type FormEvent } from 'react'
import { useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { afterLoginPath } from '../auth/paths'
import { Field, FormError } from '../components/Field'
import { PasswordInput } from '../components/PasswordInput'
import { DirectionalTransition, TransitionLink, useNavigateTransition } from '../components/transitions'
import { focusFirstInvalid } from '../lib/form'
import { errorMessage, fieldErrorMap } from '../lib/errors'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigateTransition()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from

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
      const user = await login(email.trim(), password)
      navigate(afterLoginPath(user, from), 'forward', { replace: true })
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
        <h1>Đăng nhập</h1>
        <p className="muted">Mở hộp thư, hoặc vào bảng điều khiển nếu bạn là quản trị viên.</p>
        <form onSubmit={(event) => void onSubmit(event)} className="stack">
          <Field label="Email" name="email" error={fields.email}>
            <input
              type="email"
              name="email"
              autoComplete="email"
              inputMode="email"
              spellCheck={false}
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </Field>
          <Field label="Mật khẩu" name="password" error={fields.password}>
            <PasswordInput
              name="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={setPassword}
            />
          </Field>
          <FormError>{error}</FormError>
          <button className="btn" type="submit" disabled={pending}>
            {pending ? 'Đang vào…' : 'Đăng nhập'}
          </button>
        </form>
        <p className="switch-auth">
          Chưa có tài khoản? <TransitionLink kind="lateral" to="/register">Đăng ký</TransitionLink>
        </p>
      </section>
    </DirectionalTransition>
  )
}
