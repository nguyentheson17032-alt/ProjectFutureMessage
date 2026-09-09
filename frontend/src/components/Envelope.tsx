function MailVisual() {
  return (
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
  )
}

function Locked({ hint }: { hint: string }) {
  return (
    <div className="mail-wrap">
      <div className="mail-stage">
        <MailVisual />
      </div>
      <p className="mail-hint">{hint}</p>
    </div>
  )
}

function Ready({ onReveal }: { onReveal: () => void }) {
  return (
    <div className="mail-wrap">
      <button
        type="button"
        className="mail-stage is-interactive"
        aria-label="Nhấc phong bì"
        onClick={onReveal}
      >
        <MailVisual />
      </button>
      <p className="mail-hint">Bấm phong bì, rồi xác nhận mở tin nhắn.</p>
    </div>
  )
}

function Prompt({ pending, onOpen }: { pending: boolean; onOpen: () => void }) {
  return (
    <div className="mail-wrap">
      <div className="mail-stage is-lifted">
        <MailVisual />
      </div>
      <button className="btn" type="button" disabled={pending} onClick={onOpen}>
        {pending ? 'Đang mở…' : 'Mở tin nhắn'}
      </button>
    </div>
  )
}

function Wait({ hint }: { hint: string }) {
  return (
    <div className="mail-wrap">
      <div className="mail-stage">
        <MailVisual />
      </div>
      <p className="mail-hint">{hint}</p>
    </div>
  )
}

export const Envelope = {
  Locked,
  Ready,
  Prompt,
  Wait,
}
