'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/auth-context';
import { Spinner } from '@/components/ui/spinner';

export default function Home() {
  const { isLoading, isAuthenticated, usuario } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (isLoading) return;

    if (!isAuthenticated) {
      router.replace('/login');
      return;
    }

    // Redirigir segun rol
    if (usuario?.rol === 'ROLE_CHOFER') {
      router.replace('/mi-viaje');
    } else {
      router.replace('/menu');
    }
  }, [isLoading, isAuthenticated, usuario, router]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-[#1b4332] to-[#081c15]">
      <div className="text-center space-y-4">
        <Spinner className="h-10 w-10 text-white mx-auto" />
        <p className="text-white/80">Cargando...</p>
      </div>
    </div>
  );
}
