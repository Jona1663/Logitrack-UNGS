'use client';

import { useState, useEffect, useCallback } from 'react';
import type { Envio, RegistroHistorial, EnvioUpdateDTO } from '@/types';
import { api } from '@/lib/api';

interface UseEnvioDetailState {
  envio: Envio | null;
  historial: RegistroHistorial[];
  isLoading: boolean;
  isUpdating: boolean;
  error: string | null;
}

export function useEnvioDetail(id: string | number) {
  const [state, setState] = useState<UseEnvioDetailState>({
    envio: null,
    historial: [],
    isLoading: true,
    isUpdating: false,
    error: null,
  });

  const cargarDatos = useCallback(async () => {
    setState((prev) => ({ ...prev, isLoading: true, error: null }));

    try {
      const [envioData, historialData] = await Promise.all([
        api.getEnvioCompleto(id),
        api.getHistorialEnvio(id),
      ]);

      setState({
        envio: envioData,
        historial: historialData,
        isLoading: false,
        isUpdating: false,
        error: null,
      });
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Error al cargar el envio';
      setState((prev) => ({
        ...prev,
        isLoading: false,
        error: message,
      }));
    }
  }, [id]);

  useEffect(() => {
    if (id) {
      cargarDatos();
    }
  }, [id, cargarDatos]);

  const actualizarEnvio = useCallback(
    async (data: EnvioUpdateDTO) => {
      setState((prev) => ({ ...prev, isUpdating: true }));

      try {
        const envioActualizado = await api.actualizarEnvio(id, data);
        const historialActualizado = await api.getHistorialEnvio(id);

        setState((prev) => ({
          ...prev,
          envio: envioActualizado,
          historial: historialActualizado,
          isUpdating: false,
        }));

        return envioActualizado;
      } catch (error) {
        setState((prev) => ({ ...prev, isUpdating: false }));
        throw error;
      }
    },
    [id]
  );

  return {
    ...state,
    recargar: cargarDatos,
    actualizarEnvio,
  };
}
