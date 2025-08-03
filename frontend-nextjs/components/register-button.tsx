// components/RegisterButton.tsx
"use client";
import { signIn } from "next-auth/react";
import { Button } from '@/components/ui/button';

export default function RegisterButton() {
  return (
    <Button variant="primary" size="lg" onClick={() => signIn("keycloak")} className="mt-6 w-64 font-mono">
      S’inscrire
    </Button>
  );
}
