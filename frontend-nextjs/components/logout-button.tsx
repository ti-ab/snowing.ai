'use client';
import { Button } from '@/components/ui/button';
import federatedLogout from '@/app/utils/auth';

export default function RegisterButton() {
  return (
    <Button variant="primary" size="lg" onClick={() => federatedLogout()} className="mt-6 w-64 font-mono">
      Se déconnecter
    </Button>
  );
}


