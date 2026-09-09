export function focusFirstInvalid(form: HTMLFormElement) {
  const invalid = form.querySelector<HTMLElement>('[aria-invalid="true"]')
  if (invalid) {
    invalid.focus()
    return
  }
  const alert = form.querySelector<HTMLElement>('[role="alert"]')
  alert?.focus()
}
