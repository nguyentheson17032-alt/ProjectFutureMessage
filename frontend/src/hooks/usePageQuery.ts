import { startTransition } from 'react'
import { useSearchParams } from 'react-router-dom'

export function usePageQuery() {
  const [params, setParams] = useSearchParams()
  const page = Number(params.get('page') ?? '0') || 0

  function setPage(nextPage: number) {
    startTransition(() => {
      const next = new URLSearchParams(params)
      if (nextPage <= 0) next.delete('page')
      else next.set('page', String(nextPage))
      setParams(next)
    })
  }

  function patchParams(mutate: (next: URLSearchParams) => void) {
    startTransition(() => {
      const next = new URLSearchParams(params)
      mutate(next)
      next.delete('page')
      setParams(next)
    })
  }

  return { page, setPage, params, setParams, patchParams }
}
