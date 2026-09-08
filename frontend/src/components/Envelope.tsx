type Props = {
  mode: 'locked' | 'ready' | 'prompt' | 'wait'
  hint?: string
  pending?: boolean
  onReveal?: () => void
  onOpen?: () => void
}

export function Envelope({ mode, hint, pending, onReveal, onOpen }: Props) {
  const interactive = mode === 'ready'

  return (
    <div className="mail-wrap">
      <div
        className={`mail-stage${interactive ? ' is-interactive' : ''}${mode === 'prompt' ? ' is-lifted' : ''}`}
        role={interactive ? 'button' : undefined}
        tabIndex={interactive ? 0 : undefined}
        onClick={interactive ? onReveal : undefined}
        onKeyDown={
          interactive
            ? (event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault()
                  onReveal?.()
                }
              }
            : undefined
        }
      >
        <div className="mail" aria-hidden="true">
          <div className="mail-lining" />
          <div className="mail-flap" />
          <div className="mail-pocket">
            <span className="mail-lines" />
            <span className="mail-lines short" />
          </div>
          <div className="wax-seal">
            <span>FM</span>
          </div>
        </div>
      </div>
      {mode === 'prompt' ? (
        <button className="btn" type="button" disabled={pending} onClick={() => onOpen?.()}>
          {pending ? 'Đang mở…' : 'Mở tin nhắn'}
        </button>
      ) : hint ? (
        <p className="mail-hint">{hint}</p>
      ) : null}
    </div>
  )
}
