'use client';

import Link from 'next/link';
import { ArrowLeftCircle } from 'lucide-react';
import { SearchFilters } from '@/components/busqueda/search-filters';
import { EnvioTable } from '@/components/envios/envio-table';
import { Pagination } from '@/components/busqueda/pagination';
import { EmptyState } from '@/components/busqueda/empty-state';
import { Spinner } from '@/components/ui/spinner';
import { useEnvios } from '@/hooks/use-envios';
import type { EstadoEnvio } from '@/types';

export default function BusquedaPage() {
  const {
    envios, isLoading, totalPages, totalElements, currentPage,
    filters, hasSearched, updateFilters, limpiarFiltros, buscar,
    cancelarEnvio,   // ← agregar
    paginaAnterior, paginaSiguiente, hasPreviousPage, hasNextPage,
  } = useEnvios();

  return (
    <div className="w-full max-w-5xl mx-auto p-4 md:p-6 lg:py-8">
      <Link
        href="/menu"
        className="text-[#198754] font-semibold mb-4 md:mb-6 inline-flex items-center gap-2 hover:opacity-80 transition-opacity"
      >
        <ArrowLeftCircle className="h-5 w-5" /> Volver al Panel
      </Link>

      <SearchFilters
        query={filters.query}
        estado={filters.estado}
        fecha={filters.fecha} // <-- Pasamos solo fecha
        onQueryChange={(value) => updateFilters({ query: value })}
        onEstadoChange={(value) => updateFilters({ estado: value })}
        onFechaChange={(value) => updateFilters({ fecha: value })} // <-- Un solo handler
        onSearch={() => buscar(0)}
        onClear={limpiarFiltros}
        isLoading={isLoading}
      />

      <div className="mt-6 md:mt-8">
        {!hasSearched && !isLoading && <EmptyState type="initial" />}

        {isLoading && (
          <div className="flex items-center justify-center py-12 bg-white rounded-2xl shadow-sm border-0">
            <Spinner className="h-8 w-8 text-[#198754]" />
            <span className="ml-3 text-muted-foreground font-medium">Obteniendo datos...</span>
          </div>
        )}

        {hasSearched && !isLoading && envios.length === 0 && <EmptyState type="no-results" />}

        {hasSearched && !isLoading && envios.length > 0 && (
          <div>
            <div className="flex items-center justify-between mb-3 px-2">
              <h5 className="font-bold text-gray-900 m-0 text-lg">Resultados de la consulta</h5>
            </div>
            <div className="bg-white rounded-2xl shadow-sm border-0 overflow-hidden">
              <EnvioTable envios={envios} onCancelar={cancelarEnvio} />
              <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                totalElements={totalElements}
                onPreviousPage={paginaAnterior}
                onNextPage={paginaSiguiente}
                hasPreviousPage={hasPreviousPage}
                hasNextPage={hasNextPage}
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
}