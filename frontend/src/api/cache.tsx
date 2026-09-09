import { SWRConfig } from 'swr'
import type { ReactNode } from 'react'

export function ApiCacheProvider({ children }: { children: ReactNode }) {
  return (
    <SWRConfig
      value={{
        revalidateOnFocus: true,
        shouldRetryOnError: false,
      }}
    >
      {children}
    </SWRConfig>
  )
}
