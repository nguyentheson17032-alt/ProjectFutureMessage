import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { afterLoginPath } from '../auth/paths'
import { Field } from '../components/Field'
import { PasswordInput } from '../components/PasswordInput'
import { errorMessage, fieldErrorMap } from '../lib/errors'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [fields, setFields] = useState<Record<string, string>>({})
  const [pending, setPending] = useState(false)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setFields({})
    setPending(true)
    try {
      const user = await login(email.trim(), password)
      navigate(afterLoginPath(user, from), { replace: true })
    } catch (err) {
      setFields(fieldErrorMap(err))
      setError(errorMessage(err))
    } finally {
      setPending(false)
    }
  }

  return (
    <section className="auth-panel">
      <h1>Đăng nhập</h1>
      <p className="muted">Mở hộp thư, hoặc vào bảng điều khiển nếu bạn là quản trị viên.</p>
      <form onSubmit={(event) => void onSubmit(event)} className="stack">
        <Field label="Email" error={fields.email}>
          <input
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </Field>
        <Field label="Mật khẩu" error={fields.password}>
          <PasswordInput
            autoComplete="current-password"
            required
            value={password}
            onChange={setPassword}
          />
        </Field>
        {error ? <p className="form-error">{error}</p> : null}
        <button className="btn" type="submit" disabled={pending}>
          {pending ? 'Đang vào…' : 'Đăng nhập'}
        </button>
      </form>
      <p className="switch-auth">
        Chưa có tài khoản? <Link to="/register">Đăng ký</Link>
      </p>
    </section>
  )
}
