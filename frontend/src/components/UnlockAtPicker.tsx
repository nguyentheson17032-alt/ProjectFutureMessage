import { formatDateTime, localInputToIso, toDatetimeLocal } from '../lib/time'

type Props = {
  value: string
  onChange: (value: string) => void
  error?: string
}

const PRESETS = [
  { label: '1 phút', ms: 60_000 },
  { label: '5 phút', ms: 5 * 60_000 },
  { label: '30 phút', ms: 30 * 60_000 },
  { label: '1 giờ', ms: 60 * 60_000 },
  { label: '1 ngày', ms: 24 * 60 * 60_000 },
  { label: '1 tuần', ms: 7 * 24 * 60 * 60_000 },
] as const

function split(value: string): { date: string; time: string } {
  const [date = '', time = ''] = value.split('T')
  return { date, time: time.slice(0, 5) }
}

export function UnlockAtPicker({ value, onChange, error }: Props) {
  const { date, time } = split(value)

  function applyPreset(ms: number) {
    onChange(toDatetimeLocal(undefined, ms))
  }

  return (
    <div className="field">
      <span className="field-label">Thời điểm mở</span>
      <div className="unlock-picker">
        <label className="unlock-slot">
          <span>Ngày</span>
          <input
            type="date"
            required
            aria-label="Ngày mở"
            value={date}
            onChange={(e) => onChange(`${e.target.value}T${time || '09:00'}`)}
          />
        </label>
        <label className="unlock-slot">
          <span>Giờ</span>
          <input
            type="time"
            required
            step={60}
            aria-label="Giờ mở"
            value={time}
            onChange={(e) => onChange(`${date}T${e.target.value}`)}
          />
        </label>
      </div>
      <div className="unlock-presets" role="group" aria-label="Chọn nhanh thời điểm mở">
        {PRESETS.map((preset) => (
          <button type="button" key={preset.label} className="chip" onClick={() => applyPreset(preset.ms)}>
            {preset.label}
          </button>
        ))}
      </div>
      {error ? <span className="field-error">{error}</span> : null}
      {!error && date && time ? (
        <span className="field-hint">
          Sẽ mở lúc {formatDateTime(localInputToIso(`${date}T${time}`))} (UTC+7). Nút nhanh đặt thời điểm từ bây giờ.
        </span>
      ) : null}
    </div>
  )
}
