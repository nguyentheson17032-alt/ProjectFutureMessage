import type { ReactNode } from 'react'

type Props = {
  title: string
  body: string
  action?: ReactNode
}

export function EmptyState({ title, body, action }: Props) {
  return (
    <div className="empty">
      <div className="empty-seal" aria-hidden="true" />
      <h2>{title}</h2>
      <p>{body}</p>
      {action}
    </div>
  )
}
