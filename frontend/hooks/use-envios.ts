'use client';

import { useState, useCallback } from 'react';
import type { Envio, BusquedaEnviosParams, PaginatedResponse, EstadoEnvio } from '@/types';
import { api } from '@/lib/api';

interface UseEnviosState {
  envios: Envio[];
  isLoading: boolean;
  error: string | null;
  totalPages: number;
  totalElements: number;
  currentPage: number;
}

interface UseEnviosFilters {
  query: string;
  estado: EstadoEnvio | '';
  fecha: string; // <-- Solo una fecha
}

const PAGE_SIZE = 10;

export function useEnvios() {
  const [state, setState] = useState<UseEnviosState>({
    envios: [],
    isLoading: false,
    error: null,
    totalPages: 0,
    totalElements: 0,
    currentPage: 0,
  });

  const [filters, setFilters] = useState<UseEnviosFilters>({
    query: '',
    estado: '',
    fecha: '',
  });

  const [hasSearched, setHasSearched] = useState(false);

  const buscar = useCallback(async (page: number = 0) => {
    setState((prev) => ({ ...prev, isLoading: true, error: null }));
    setHasSearched(true);

    try {
      const params: BusquedaEnviosParams = {
        query: filters.query || undefined,
        estado: filters.estado || undefined,
        fecha: filters.fecha || undefined,
        page,
        size: PAGE_SIZE,
      };
      const response = await api.buscarEnvios(params);

      const capitalizar = (str: string) =>
        str ? str.charAt(0).toUpperCase() + str.slice(1).toLowerCase() : 'Pendiente';

      // const enviosNormalizados = response.content.map((raw: any) => ({
      //   idEnvio: raw.id ?? raw.idEnvio,
      //   cpe: raw.cpe,
      //   kgOrigen: (raw.kgOrigen ?? raw.kgOrigen) ?? 0,
      //   tipoGrano: raw.tipoGrano ?? raw.tipoGrano ?? '',
      //   estadoActual: capitalizar(raw.estadoActual ?? raw.estadoActual ?? 'pendiente'),
      //   origen: {
      //     empresa: {
      //       razonSocial: raw.origen?.empresa?.razonSocial ?? raw.clienteRazonSocial ?? 'Sin cliente'
      //     }
      //   },
      //   destino: {
      //     nombreLugar: raw.destino?.nombreLugar ?? raw.destinoNombre ?? 'Destino pendiente'
      //   },
      //   // Valores por defecto para las propiedades faltantes
      //   prioridadIa: raw.prioridadIa ?? false,
      //   fechaCreacion: raw.fechaCreacion ?? new Date().toISOString(),
      //   chofer: raw.chofer ?? null,
      //   camion: raw.camion ?? null,
      // } as Envio));

      // Elimina o ignora la función capitalizar para el estadoActual
      const enviosNormalizados = response.content.map((raw: any) => ({
        idEnvio: raw.id ?? raw.idEnvio,
        cpe: raw.cpe,
        kgOrigen: raw.kgOrigen ?? 0,
        tipoGrano: raw.tipoGrano ?? '',

        // 1. Tomamos el valor directo del backend. 
        // 2. Si no viene, asignamos 'PENDIENTE' en mayúsculas como fallback seguro.
        // 3. Forzamos el tipado correcto de TypeScript as EstadoEnvio.
        estadoActual: (raw.estadoActual || 'PENDIENTE') as EstadoEnvio,

        origen: {
          empresa: {
            razonSocial: raw.origen?.empresa?.razonSocial ?? raw.clienteRazonSocial ?? 'Sin cliente'
          }
        },
        destino: {
          nombreLugar: raw.destino?.nombreLugar ?? raw.destinoNombre ?? 'Destino pendiente'
        },
        prioridadIa: raw.prioridadIa ?? false,
        fechaCreacion: raw.fechaCreacion ?? new Date().toISOString(),
        chofer: raw.chofer ?? null,
        camion: raw.camion ?? null,
      } as Envio));

      setState({
        envios: enviosNormalizados,
        isLoading: false,
        error: null,
        totalPages: response.totalPages,
        totalElements: response.totalElements,
        currentPage: response.number,
      });
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Error al buscar envios';
      setState((prev) => ({
        ...prev,
        isLoading: false,
        error: message,
        envios: [],
      }));
    }
  }, [filters]);

  const cancelarEnvio = useCallback(async (id: string | number) => {
    await api.cancelarEnvio(id);
    // Recarga la página actual manteniendo filtros y paginación
    await buscar(state.currentPage);
  }, [buscar, state.currentPage]);

  const updateFilters = useCallback((newFilters: Partial<UseEnviosFilters>) => {
    setFilters((prev) => ({ ...prev, ...newFilters }));
  }, []);

  const limpiarFiltros = useCallback(() => {
    // 1. Limpiamos los inputs
    setFilters({
      query: '',
      estado: '',
      fecha: '',
    });

    // 2. Volvemos al estado inicial (muestra el camión gris)
    setHasSearched(false);

    // 3. Vaciamos la tabla y reseteamos la paginación
    setState({
      envios: [],
      isLoading: false,
      error: null,
      totalPages: 0,
      totalElements: 0,
      currentPage: 0,
    });
  }, []);

  const irAPagina = useCallback((page: number) => {
    buscar(page);
  }, [buscar]);

  const paginaAnterior = useCallback(() => {
    if (state.currentPage > 0) {
      buscar(state.currentPage - 1);
    }
  }, [buscar, state.currentPage]);

  const paginaSiguiente = useCallback(() => {
    if (state.currentPage < state.totalPages - 1) {
      buscar(state.currentPage + 1);
    }
  }, [buscar, state.currentPage, state.totalPages]);

  return {
    ...state,
    filters,
    hasSearched,
    updateFilters,
    limpiarFiltros,
    buscar,
    cancelarEnvio,
    irAPagina,
    paginaAnterior,
    paginaSiguiente,
    hasPreviousPage: state.currentPage > 0,
    hasNextPage: state.currentPage < state.totalPages - 1,
  };
}
