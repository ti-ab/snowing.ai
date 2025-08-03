import React, { useEffect } from 'react'
import { signOut } from 'next-auth/react'

export function Providers({ children }: { children: React.ReactNode }) {
  useEffect(() => {
    const onMsg = async (e: MessageEvent) => {
      if (e.data === 'logout-complete') {
        await signOut({ redirect: false })
        location.href = '/'          // final redirect
      }
    }
    window.addEventListener('message', onMsg)
    return () => window.removeEventListener('message', onMsg)
  }, [])
  return <>{children}</>
}
