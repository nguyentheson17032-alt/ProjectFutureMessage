import {
  addTransitionType,
  startTransition,
  ViewTransition,
  type MouseEvent as ReactMouseEvent,
  type ReactNode,
} from 'react'
import {
  Link,
  NavLink,
  useNavigate,
  type LinkProps,
  type NavigateOptions,
  type NavLinkProps,
  type To,
} from 'react-router-dom'

export type NavKind = 'forward' | 'back' | 'lateral'

function applyKind(kind: NavKind) {
  if (kind === 'forward') addTransitionType('nav-forward')
  if (kind === 'back') addTransitionType('nav-back')
}

function shouldPassThrough(event: ReactMouseEvent) {
  return event.metaKey || event.ctrlKey || event.shiftKey || event.altKey || event.button !== 0
}

export function useNavigateTransition() {
  const navigate = useNavigate()
  return (to: To, kind: NavKind = 'forward', options?: NavigateOptions) => {
    startTransition(() => {
      applyKind(kind)
      navigate(to, options)
    })
  }
}

export function DirectionalTransition({ children }: { children: ReactNode }) {
  return (
    <ViewTransition
      enter={{ 'nav-forward': 'nav-forward', 'nav-back': 'nav-back', default: 'none' }}
      exit={{ 'nav-forward': 'nav-forward', 'nav-back': 'nav-back', default: 'none' }}
      default="none"
    >
      {children}
    </ViewTransition>
  )
}

export function FadeTransition({ children }: { children: ReactNode }) {
  return (
    <ViewTransition enter="fade-in" exit="fade-out" default="none">
      {children}
    </ViewTransition>
  )
}

type TransitionLinkProps = LinkProps & { kind?: NavKind }

export function TransitionLink({ kind = 'forward', onClick, to, ...rest }: TransitionLinkProps) {
  const navigate = useNavigate()
  return (
    <Link
      to={to}
      onClick={(event) => {
        onClick?.(event)
        if (event.defaultPrevented || shouldPassThrough(event)) return
        event.preventDefault()
        startTransition(() => {
          applyKind(kind)
          navigate(to)
        })
      }}
      {...rest}
    />
  )
}

type TransitionNavLinkProps = NavLinkProps & { kind?: NavKind }

export function TransitionNavLink({
  kind = 'lateral',
  onClick,
  to,
  ...rest
}: TransitionNavLinkProps) {
  const navigate = useNavigate()
  return (
    <NavLink
      to={to}
      onClick={(event) => {
        onClick?.(event)
        if (event.defaultPrevented || shouldPassThrough(event)) return
        event.preventDefault()
        startTransition(() => {
          applyKind(kind)
          navigate(to)
        })
      }}
      {...rest}
    />
  )
}
