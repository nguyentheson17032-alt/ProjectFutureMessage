import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Field } from '../components/Field'
import { PasswordInput } from '../components/PasswordInput'
import { errorMessage, fieldErrorMap } from '../lib/errors'

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [displayName, setDisplayName] = useState('')
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
      await register(email.trim(), password, displayName.trim())
      navigate('/inbox', { replace: true })
    } catch (err) {
      setFields(fieldErrorMap(err))
      setError(errorMessage(err))
    } finally {
      setPending(false)
    }
  }

  return (
    <section className="auth-panel">
      <h1>Tạo tài khoản</h1>
      <p className="muted">Email dùng để nhận thư — kể cả khi người khác gửi trước khi bạn đăng ký.</p>
      <form onSubmit={(event) => void onSubmit(event)} className="stack">
        <Field label="Tên hiển thị" error={fields.displayName}>
          <input
            type="text"
            maxLength={100}
            required
            autoComplete="name"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
          />
        </Field>
        <Field label="Email" error={fields.email}>
          <input
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </Field>
        <Field label="Mật khẩu" hint="Tối thiểu 8 ký tự." error={fields.password}>
          <PasswordInput
            autoComplete="new-password"
            required
            minLength={8}
            maxLength={72}
            value={password}
            onChange={setPassword}
          />
        </Field>
        {error ? <p className="form-error">{error}</p> : null}
        <button className="btn" type="submit" disabled={pending}>
          {pending ? 'Đang tạo…' : 'Đăng ký'}
        </button>
      </form>
      <p className="switch-auth">
        Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
      </p>
    </section>
  )
}
