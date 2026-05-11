'use client';

import { useState, useEffect, useCallback } from 'react';
import type { EnvioChofer } from '@/types';
import { api } from '@/lib/api';
import { FLUJO_LOGISTICO } from '@/lib/constants';

interface UseViajeChoferState {
  viaje: EnvioChofer | null;
  isLoading: boolean;
  isUpdating: boolean;
  error: string | null;
}

export function useViajeChofer() {
  const [state, setState] = useState<UseViajeChoferState>({
    viaje: null,
    isLoading: true,
    isUpdating: false,
    error: null,
  });

  const cargarViaje = useCallback(async () => {
    setState((prev) => ({ ...prev, isLoading: true, error: null }));

    try {
      const asignaciones = await api.getMisAsignaciones();

      // Buscar viaje activo (no entregado ni cancelado)
      const viajeActivo = asignaciones.find(
        (e) => e.estado_actual !== 'ENTREGADO' && e.estado_actual !== 'CANCELADO'
      ) ?? null;

      setState({
        viaje: viajeActivo,
        isLoading: false,
        isUpdating: false,
        error: null,
      });
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Error al cargar viaje';
      setState((prev) => ({
        ...prev,
        isLoading: false,
        error: message,
      }));
    }
  }, []);

  useEffect(() => {
    cargarViaje();
  }, [cargarViaje]);

  const avanzarEstado = useCallback(async () => {
    if (!state.viaje) return;

    const flujo = FLUJO_LOGISTICO[state.viaje.estado_actual];
    if (!flujo.siguiente) return;

    setState((prev) => ({ ...prev, isUpdating: true }));

    try {
      const viajeActualizado = await api.cambiarEstadoChofer(
        state.viaje.id_envio,
        flujo.siguiente
      );

      // El endpoint de cambio de estado devuelve Envio completo,
      // por eso mapeamos solo los campos que necesitamos
      setState((prev) => ({
        ...prev,
        viaje: prev.viaje
          ? { ...prev.viaje, estado_actual: viajeActualizado.estado_actual }
          : null,
        isUpdating: false,
      }));

      return viajeActualizado;
    } catch (error) {
      setState((prev) => ({ ...prev, isUpdating: false }));
      throw error;
    }
  }, [state.viaje]);

  const reportarIncidencia = useCallback(
    async (descripcion: string) => {
      if (!state.viaje) return;

      setState((prev) => ({ ...prev, isUpdating: true }));

      try {
        await api.reportarIncidencia(state.viaje!.id_envio, { descripcion });
        setState((prev) => ({ ...prev, isUpdating: false }));
      } catch (error) {
        setState((prev) => ({ ...prev, isUpdating: false }));
        throw error;
      }
    },
    [state.viaje]
  );

  const getSiguienteAccion = useCallback(() => {
    if (!state.viaje) return null;
    return FLUJO_LOGISTICO[state.viaje.estado_actual];
  }, [state.viaje]);

  return {
    ...state,
    recargar: cargarViaje,
    avanzarEstado,
    reportarIncidencia,
    getSiguienteAccion,
  };
}