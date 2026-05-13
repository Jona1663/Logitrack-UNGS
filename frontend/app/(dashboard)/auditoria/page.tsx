'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { ArrowLeftCircle, History, ClipboardX } from 'lucide-react';
import { GlobalHistorialTable } from '@/components/auditoria/global-historial-table';
import { Spinner } from '@/components/ui/spinner';
import { api } from '@/lib/api';
import type { RegistroHistorial } from '@/types';

export default function AuditoriaPage() {
  const [historial, setHistorial] = useState<RegistroHistorial[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const cargarHistorial = async () => {
      try {
        const data = await api.getHistorialCompleto();
        setHistorial(data);
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Error al cargar historial';
        setError(message);
      } finally {
        setIsLoading(false);
      }
    };

    cargarHistorial();
  }, []);

  return (
    <div className="w-full max-w-5xl mx-auto p-4 md:p-6 lg:py-8">
      {/* Botón Volver */}
      <Link
        href="/menu"
        className="text-[#198754] font-semibold mb-3 md:mb-4 inline-flex items-center gap-2 hover:opacity-80 transition-opacity"
      >
        <ArrowLeftCircle className="h-5 w-5" /> Volver al Panel
      </Link>

      {/* Encabezado Principal (Réplica exacta de tu HTML) */}
      <div className="flex items-center gap-3 mb-6 mt-2 px-2 md:px-0">
        <div className="bg-amber-500/10 text-amber-500 p-3 rounded-xl border border-amber-500/25 shadow-sm shrink-0">
          <History className="h-7 w-7" />
        </div>
        <div>
          <h4 className="font-bold text-gray-900 mb-1 text-xl md:text-2xl">Auditoría Global</h4>
          <p className="text-muted-foreground text-sm m-0">
            Registro histórico de modificaciones de estado y prioridad en la red logística.
          </p>
        </div>
      </div>

      {/* Estados de Carga, Error y Datos */}
      {isLoading ? (
        <div className="bg-white rounded-2xl shadow-sm border-0 p-12 text-center flex flex-col items-center">
          <Spinner className="h-8 w-8 text-[#198754] mb-3" />
          <p className="text-muted-foreground text-sm">Procesando registros de auditoría...</p>
        </div>
      ) : error ? (
        <div className="bg-red-50 rounded-2xl shadow-sm border-0 p-12 text-center">
          <p className="text-red-600 font-bold mb-0 flex items-center justify-center gap-2">
            <ClipboardX className="h-5 w-5" /> {error}
          </p>
        </div>
      ) : historial.length === 0 ? (
        /* Estado Vacío (Tu "emptyState" original) */
        <div className="bg-white rounded-2xl shadow-sm border-0 p-8 md:p-16 text-center">
          <ClipboardX className="h-16 w-16 mx-auto text-muted-foreground opacity-25 mb-4" strokeWidth={1.5} />
          <h6 className="font-bold text-gray-900 text-lg">Sin registros de auditoría</h6>
          <p className="text-muted-foreground text-sm max-w-md mx-auto mt-2">
            Actualmente no se encuentran cambios registrados en los envíos del sistema.
          </p>
        </div>
      ) : (
        /* Tabla de Resultados */
        <div className="bg-white rounded-2xl shadow-sm border-0 overflow-hidden mb-8 md:mb-10">
          <GlobalHistorialTable historial={historial} />
        </div>
      )}
    </div>
  );
}