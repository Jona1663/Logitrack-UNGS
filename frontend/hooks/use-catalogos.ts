'use client';

import { useState, useEffect, useCallback } from 'react';
import type { Empresa, Establecimiento, Chofer, Camion, TipoGrano } from '@/types';
import { api } from '@/lib/api';
import { TIPOS_GRANO } from '@/lib/constants';

interface UseCatalogosState {
  empresas: Empresa[];
  choferes: Chofer[];
  camiones: Camion[];
  tiposGrano: TipoGrano[];
  isLoading: boolean;
  error: string | null;
}

export function useCatalogos() {
  const [state, setState] = useState<UseCatalogosState>({
    empresas: [],
    choferes: [],
    camiones: [],
    tiposGrano: [...TIPOS_GRANO],
    isLoading: true,
    error: null,
  });

  const [establecimientos, setEstablecimientos] = useState<Establecimiento[]>([]);
  const [loadingEstablecimientos, setLoadingEstablecimientos] = useState(false);

  // Cargar catalogos iniciales
  useEffect(() => {
    const cargarCatalogos = async () => {
      try {
        const [empresasData, choferesData, camionesData] = await Promise.all([
          api.getEmpresas(),
          api.getChoferes(),
          api.getCamiones(),
        ]);

        setState({
          empresas: empresasData,
          choferes: choferesData,
          camiones: camionesData,
          tiposGrano: [...TIPOS_GRANO],
          isLoading: false,
          error: null,
        });
      } catch (error) {
        const message = error instanceof Error ? error.message : 'Error al cargar catalogos';
        setState((prev) => ({
          ...prev,
          isLoading: false,
          error: message,
        }));
      }
    };

    cargarCatalogos();
  }, []);

  // Cargar establecimientos por CUIT de empresa
  const cargarEstablecimientos = useCallback(async (cuit: string) => {
    if (!cuit) {
      setEstablecimientos([]);
      return;
    }

    setLoadingEstablecimientos(true);
    try {
      const data = await api.getEstablecimientos(cuit);
      setEstablecimientos(data);
    } catch (error) {
      console.error('Error cargando establecimientos:', error);
      setEstablecimientos([]);
    } finally {
      setLoadingEstablecimientos(false);
    }
  }, []);

  // Buscar empresas por texto (para autocomplete)
  const buscarEmpresas = useCallback(
    (texto: string): Empresa[] => {
      if (!texto || texto.length < 2) return [];
      const textoLower = texto.toLowerCase();
      return state.empresas.filter(
        (e) =>
          e.razon_social.toLowerCase().includes(textoLower) ||
          e.cuit.includes(texto)
      );
    },
    [state.empresas]
  );

  // Buscar tipos de grano por texto
  const buscarGranos = useCallback(
    (texto: string): TipoGrano[] => {
      if (!texto) return state.tiposGrano;
      const textoLower = texto.toLowerCase();
      return state.tiposGrano.filter((g) =>
        g.toLowerCase().includes(textoLower)
      );
    },
    [state.tiposGrano]
  );

  return {
    ...state,
    establecimientos,
    loadingEstablecimientos,
    cargarEstablecimientos,
    buscarEmpresas,
    buscarGranos,
  };
}
