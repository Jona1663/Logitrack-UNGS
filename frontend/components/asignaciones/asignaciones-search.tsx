'use client';

import { useState, useCallback } from 'react';
import { Search, RefreshCw, MapPin, User, Truck, AlertCircle, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { SearchFilters } from '@/components/busqueda/search-filters';
import { Pagination } from '@/components/busqueda/pagination';
import { EmptyState } from '@/components/busqueda/empty-state';
import { EstadoBadge } from '@/components/envios/estado-badge';
import { api } from '@/lib/api';
import { getNombreChofer, normalizarEnum } from '@/lib/utils';
import type { Envio, EstadoEnvio } from '@/types';

interface AsignacionesSearchProps {
  onReasignar: (envio: Envio) => void;
}

export function AsignacionesSearch({ onReasignar }: AsignacionesSearchProps) {
  // Estado local para la búsqueda (similar al hook useEnvios)
  const [envios, setEnvios] = useState<Envio[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [hasSearched, setHasSearched] = useState(false);

  // Filtros iniciales
  const [filters, setFilters] = useState({
    query: '',
    estado: '' as EstadoEnvio | '',
    fecha: '',
  });

  const updateFilters = (newFilters: Partial<typeof filters>) => {
    setFilters((prev) => ({ ...prev, ...newFilters }));
  };

  const buscar = async (page: number) => {
    setIsLoading(true);
    try {
      const response = await api.buscarEnviosAsignadosAvanzado({
        query: filters.query,
        estado: filters.estado || undefined,
        fecha: filters.fecha || undefined,
        page,
        size: 10,
      });
      setEnvios(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
      setCurrentPage(page);
      setHasSearched(true);
    } catch (error) {
      toast.error('Error al buscar envíos asignados');
      setEnvios([]);
      setHasSearched(true);
    } finally {
      setIsLoading(false);
    }
  };

  const limpiarFiltros = () => {
    setFilters({ query: '', estado: '', fecha: '' });
    // Opcional: resetear resultados
    setEnvios([]);
    setHasSearched(false);
  };

  const paginaAnterior = () => {
    if (currentPage > 0) buscar(currentPage - 1);
  };
  const paginaSiguiente = () => {
    if (currentPage + 1 < totalPages) buscar(currentPage + 1);
  };

  return (
    <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
      <div className="bg-blue-50 px-6 py-5 md:px-10 md:py-6 border-b border-blue-200/60">
        <h4 className="text-xl md:text-2xl font-bold text-gray-900 flex items-center gap-2 mb-1">
          <Search className="h-6 w-6 text-blue-600" /> Buscar Envíos Asignados
        </h4>
        <p className="text-muted-foreground text-sm">
          Filtre por ID, cliente, CPE, estado o fecha para localizar un envío ya asignado y cambiar su chofer o camión.
        </p>
      </div>

      {/* Filtros reutilizables */}
      <div className="px-6 pt-6">
        <SearchFilters
          query={filters.query}
          estado={filters.estado}
          fecha={filters.fecha}
          onQueryChange={(value) => updateFilters({ query: value })}
          onEstadoChange={(value) => updateFilters({ estado: value })}
          onFechaChange={(value) => updateFilters({ fecha: value })}
          onSearch={() => buscar(0)}
          onClear={limpiarFiltros}
          isLoading={isLoading}
        />
      </div>

      {/* Resultados */}
      <div className="mt-4 pb-6 px-6">
        {!hasSearched && !isLoading && <EmptyState type="initial" />}

        {isLoading && (
          <div className="flex items-center justify-center py-12">
            <Spinner className="h-8 w-8 text-blue-600" />
            <span className="ml-3 text-muted-foreground font-medium">Buscando envíos...</span>
          </div>
        )}

        {hasSearched && !isLoading && envios.length === 0 && <EmptyState type="no-results" />}

        {hasSearched && !isLoading && envios.length > 0 && (
          <div>
            <div className="flex items-center justify-between mb-3 px-2">
              <h5 className="font-bold text-gray-900 text-lg">Resultados</h5>
              <span className="text-sm text-muted-foreground">{totalElements} envíos encontrados</span>
            </div>
            <div className="border rounded-xl overflow-hidden">
              {/* Tabla Desktop */}
              <div className="hidden md:block overflow-x-auto">
                <Table>
                  <TableHeader className="bg-muted/30">
                    <TableRow>
                      <TableHead className="pl-6">ID / CPE</TableHead>
                      <TableHead>Cliente</TableHead>
                      <TableHead>Carga</TableHead>
                      <TableHead>Estado</TableHead>
                      <TableHead>Chofer actual</TableHead>
                      <TableHead>Camión actual</TableHead>
                      <TableHead className="pr-6 text-right">Acción</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {envios.map((envio) => (
                      <TableRow key={envio.idEnvio}>
                        <TableCell className="pl-6">
                          <span className="font-bold text-blue-600">{envio.idEnvio}</span>
                          <span className="text-xs text-muted-foreground block">CPE: {envio.cpe}</span>
                        </TableCell>
                        <TableCell>{envio.origen?.empresa?.razonSocial || '-'}</TableCell>
                        <TableCell>
                          {normalizarEnum(envio.tipoGrano)}<br />
                          <span className="text-xs text-muted-foreground">{((envio.kgOrigen || 0)/1000).toFixed(1)} Tn</span>
                        </TableCell>
                        <TableCell><EstadoBadge estado={envio.estadoActual} /></TableCell>
                        <TableCell>
                          {envio.chofer ? (
                            <span className="flex items-center gap-1"><User className="h-3 w-3" />{getNombreChofer(envio.chofer)}</span>
                          ) : '-'}
                        </TableCell>
                        <TableCell>
                          {envio.camion ? (
                            <span className="flex items-center gap-1"><Truck className="h-3 w-3" />{envio.camion.patente}</span>
                          ) : '-'}
                        </TableCell>
                        <TableCell className="pr-6 text-right">
                          <Button
                            size="sm"
                            onClick={() => onReasignar(envio)}
                            className="bg-blue-600 hover:bg-blue-700 text-white shadow-sm"
                          >
                            <RefreshCw className="h-4 w-4 mr-1.5" /> Reasignar
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>

              {/* Tarjetas Mobile */}
              <div className="md:hidden p-4 space-y-4">
                {envios.map((envio) => (
                  <div key={envio.idEnvio} className="border rounded-lg p-4 space-y-2 bg-white">
                    <div className="flex justify-between items-start">
                      <div>
                        <span className="font-bold text-blue-600">{envio.idEnvio}</span>
                        <span className="text-xs text-muted-foreground ml-2">CPE: {envio.cpe}</span>
                      </div>
                      <EstadoBadge estado={envio.estadoActual} showIcon={false} />
                    </div>
                    <div className="grid grid-cols-2 gap-2 text-sm">
                      <span className="text-muted-foreground">Cliente:</span>
                      <span className="font-medium">{envio.origen?.empresa?.razonSocial || '-'}</span>
                      <span className="text-muted-foreground">Carga:</span>
                      <span>{normalizarEnum(envio.tipoGrano)} - {((envio.kgOrigen || 0)/1000).toFixed(1)} Tn</span>
                      <span className="text-muted-foreground">Chofer:</span>
                      <span>{envio.chofer ? getNombreChofer(envio.chofer) : '-'}</span>
                      <span className="text-muted-foreground">Camión:</span>
                      <span>{envio.camion?.patente || '-'}</span>
                    </div>
                    <Button
                      onClick={() => onReasignar(envio)}
                      className="w-full bg-blue-600 hover:bg-blue-700 text-white"
                    >
                      <RefreshCw className="h-4 w-4 mr-2" /> Reasignar
                    </Button>
                  </div>
                ))}
              </div>

              <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                totalElements={totalElements}
                onPreviousPage={paginaAnterior}
                onNextPage={paginaSiguiente}
                hasPreviousPage={currentPage > 0}
                hasNextPage={currentPage + 1 < totalPages}
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
}