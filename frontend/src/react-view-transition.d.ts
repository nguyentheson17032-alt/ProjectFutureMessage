import type { ReactNode } from 'react'

type ViewTransitionClass = 'none' | 'auto' | (string & {})

type ViewTransitionClassPerType = {
  default: ViewTransitionClass
  [transitionType: string]: ViewTransitionClass
}

type ViewTransitionClassProp = ViewTransitionClass | ViewTransitionClassPerType

declare module 'react' {
  export function addTransitionType(type: string): void

  export function ViewTransition(props: {
    children?: ReactNode
    name?: string
    default?: ViewTransitionClassProp
    enter?: ViewTransitionClassProp
    exit?: ViewTransitionClassProp
    share?: ViewTransitionClassProp
    update?: ViewTransitionClassProp
  }): ReactNode
}
