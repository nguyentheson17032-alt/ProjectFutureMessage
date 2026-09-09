import {
  cloneElement,
  isValidElement,
  useId,
  type ReactElement,
  type ReactNode,
} from 'react'

type ControlProps = {
  id?: string
  name?: string
  'aria-invalid'?: boolean
  'aria-describedby'?: string
}

type Props = {
  label: string
  hint?: string
  error?: string
  name?: string
  children: ReactElement<ControlProps> | ReactNode
}

export function Field({ label, hint, error, name, children }: Props) {
  const id = useId()
  const errorId = `${id}-err`
  const hintId = `${id}-hint`
  const describedBy = error ? errorId : hint ? hintId : undefined
  const control =
    isValidElement<ControlProps>(children)
      ? cloneElement(children, {
          id,
          name: name ?? children.props.name,
          'aria-invalid': error ? true : undefined,
          'aria-describedby': describedBy,
        })
      : children

  return (
    <div className="field">
      <label className="field-label" htmlFor={id}>
        {label}
      </label>
      {control}
      {error ? (
        <span className="field-error" id={errorId} role="alert">
          {error}
        </span>
      ) : hint ? (
        <span className="field-hint" id={hintId}>
          {hint}
        </span>
      ) : null}
    </div>
  )
}

export function FormError({ children }: { children: ReactNode }) {
  if (!children) return null
  return (
    <p className="form-error" role="alert" aria-live="polite" tabIndex={-1}>
      {children}
    </p>
  )
}
