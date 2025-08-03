import RegisterButton from '@/components/register-button';
import LogoutButton from '@/components/logout-button';
import { logout } from "next-auth/react";
import { Button } from '@/components/ui/button';


export default async function Page() {
  return <div
    className="fixed inset-0 z-10 mx-auto flex h-svh flex-col items-center justify-center text-center"
  >
    <RegisterButton />

    <LogoutButton />


  </div>;
}
