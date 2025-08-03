'use server'

import { signOut } from '@/auth'
import { cookies } from 'next/headers'
export async function logoutAction() {
  const store = cookies()
  const names = [
    'next-auth.session-token',
    'authjs.session-token',
    // add any __Secure-/__Host- variants
  ]
  names.forEach(n => store.delete(n))
  await signOut({ redirect: false })   // invalidate NextAuth cache
  return { ok: true }
}
