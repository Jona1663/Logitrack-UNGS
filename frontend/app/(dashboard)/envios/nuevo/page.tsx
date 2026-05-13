'use client';

import Link from 'next/link';
import { ArrowLeftCircle } from 'lucide-react';
import { EnvioForm } from '@/components/envios/envio-form';

export default function CrearEnvioPage() {
  return (
    <div className="w-full max-w-5xl mx-auto p-4 md:p-6 lg:py-8">
      {/* Botón de retroceso idéntico al de tu diseño original */}
      <Link
        href="/menu"
        className="text-[#198754] font-semibold mb-4 md:mb-6 inline-flex items-center gap-2 hover:opacity-80 transition-opacity"
      >
        <ArrowLeftCircle className="h-5 w-5" /> Volver al Panel
      </Link>

      {/* Aquí renderizamos directamente el formulario para que ocupe todo el ancho disponible */}
      <EnvioForm />
    </div>
  );
}