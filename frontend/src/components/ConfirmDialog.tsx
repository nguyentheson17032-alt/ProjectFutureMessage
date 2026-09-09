import type { ReactNode } from 'react'
import { useEffect, useRef } from 'react'

type Props = {
  open: boolean
  title: string
  children: ReactNode
  confirmLabel: string
  pending?: boolean
  pendingLabel?: string
  danger?: boolean
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDialog({
  open,
  title,
  children,
  confirmLabel,
  pending,
  pendingLabel = 'Đang xử lý…',
  danger,
  onConfirm,
  onCancel,
}: Props) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    else if (!open && dialog.open) dialog.close()
  }, [open])

  return (
    <dialog
      ref={ref}
      className="confirm-dialog"
      aria-labelledby="confirm-title"
      onCancel={(event) => {
        event.preventDefault()
        if (!pending) onCancel()
      }}
    >
      <h2 id="confirm-title">{title}</h2>
      <div>{children}</div>
      <div className="row-actions">
        <button type="button" className="btn ghost" disabled={pending} onClick={onCancel}>
          Giữ nguyên
        </button>
        <button
          type="button"
          className={danger ? 'btn danger' : 'btn'}
          disabled={pending}
          onClick={onConfirm}
        >
          {pending ? pendingLabel : confirmLabel}
        </button>
      </div>
    </dialog>
  )
}
