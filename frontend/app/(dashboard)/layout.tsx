'use client';

import { useAuth } from '@/contexts/auth-context';
import { Header } from '@/components/layout/header';
import { Footer } from '@/components/layout/footer';
import { Spinner } from '@/components/ui/spinner';

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isLoading, isAuthenticated, usuario } = useAuth();

  // Mostrar loading mientras carga la sesion o mientras no hay datos de autenticacion
  // El AuthContext maneja todas las redirecciones
  if (isLoading || !isAuthenticated || !usuario) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <Spinner className="h-8 w-8" />
      </div>
    );
  }

  // Si es chofer, el AuthContext redirigira a /mi-viaje
  if (usuario.rol === 'ROLE_CHOFER') {
    return null;
  }

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <Header />
      <main className="flex-1">{children}</main>
      <Footer />
    </div>
  );
}
