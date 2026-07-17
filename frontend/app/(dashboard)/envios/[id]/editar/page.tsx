'use client';

import { use } from 'react';
import Link from 'next/link';
import { ArrowLeftCircle } from 'lucide-react';
import { EnvioForm } from '@/components/envios/envio-form';
import { Spinner } from '@/components/ui/spinner';
import { useEnvioDetail } from '@/hooks/use-envio-detail';

export default function EditarEnvioPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const { envio, isLoading, error } = useEnvioDetail(id);

  if (isLoading) {
    return (
      <div className="w-full max-w-5xl mx-auto p-4 md:p-6 lg:py-8 flex justify-center items-center min-h-[60vh]">
        <Spinner className="h-10 w-10 text-[#198754]" />
      </div>
    );
  }

  if (error || !envio) {
    return (
      <div className="w-full max-w-5xl mx-auto p-4 md:p-6 lg:py-8 text-center pt-20">
        <p className="text-destructive font-bold text-lg mb-4">
          {error || 'Envío no encontrado'}
        </p>
        <Link href="/busqueda" className="text-[#198754] underline">
          Volver a Rastreo
        </Link>
      </div>
    );
  }

  return (
    <div className="w-full max-w-5xl mx-auto p-4 md:p-6 lg:py-8">
      <Link
        href={`/envios/${id}`}
        className="text-[#198754] font-semibold mb-4 md:mb-6 inline-flex items-center gap-2 hover:opacity-80 transition-opacity"
      >
        <ArrowLeftCircle className="h-5 w-5" /> Volver a la Ficha
      </Link>

      <EnvioForm
        modo="editar"
        envioId={id}
        envioInicial={envio}
      />
    </div>
  );
}