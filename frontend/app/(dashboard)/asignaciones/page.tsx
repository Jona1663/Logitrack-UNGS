'use client';
import Link from 'next/link';
import { ArrowLeftCircle } from 'lucide-react';
import { useAuth } from '@/contexts/auth-context';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';
import { AsignacionesTable } from '@/components/asignaciones/asignaciones-table';

export default function AsignacionesPage() {
  const { usuario, permisos } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (usuario && !permisos?.asignarTransporte) {
      router.replace('/menu');
    }
  }, [usuario, permisos, router]);

  return (
    <div className="w-full max-w-5xl mx-auto p-4 md:p-6 lg:py-8">
      <Link
        href="/menu"
        className="text-[#198754] font-semibold mb-4 md:mb-6 inline-flex items-center gap-2 hover:opacity-80 transition-opacity"
      >
        <ArrowLeftCircle className="h-5 w-5" /> Volver al Panel
      </Link>

      <AsignacionesTable />
    </div>
  );
}