import type {
  LoginRequest,
  LoginResponse,
  Envio,
  EnvioRequestDTO,
  EnvioUpdateDTO,
  IncidenciaDTO,
  BusquedaEnviosParams,
  PaginatedResponse,
  RegistroHistorial,
  Empresa,
  Establecimiento,
  Chofer,
  Camion,
  MetadatosCatalogo,
  EnvioChofer,
} from '@/types';

// Base URL de la API - usar variable de entorno en produccion
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

// === CLIENTE HTTP ===
class ApiClient {
  private getToken(): string | null {
    if (typeof window === 'undefined') return null;
    return sessionStorage.getItem('jwt');
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const token = this.getToken();

    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      ...options.headers,
    };

    if (token) {
      (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    if (response.status === 401) {
      // 1. Si el 401 viene del intento de login, no recargamos. 
      // Solo lanzamos el error para que el formulario lo atrape y muestre el toast rojo.
      if (endpoint === '/auth/login') {
        throw new Error('Credenciales incorrectas o usuario inactivo');
      }

      // 2. Si el 401 viene de CUALQUIER otra ruta, el token expiró. Limpiamos y redirigimos.
      if (typeof window !== 'undefined') {
        sessionStorage.removeItem('jwt');
        sessionStorage.removeItem('usuario');

        // Evitamos recargar si, por algún motivo extraño, ya estamos en /login
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
      }
      throw new Error('Sesión expirada');
    }

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `Error ${response.status}`);
    }

    // Verificar si hay contenido para parsear
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return response.json();
    }

    return {} as T;
  }

  // === AUTH ===
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    return this.request<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    });
  }

  // === ENVIOS ===
  async buscarEnvios(params: BusquedaEnviosParams): Promise<PaginatedResponse<Envio>> {
    const searchParams = new URLSearchParams();
    if (params.query) searchParams.append('query', params.query);
    if (params.estado) searchParams.append('estado', params.estado);
    if (params.fecha) searchParams.append('fecha', params.fecha); // <-- Solo enviamos 'fecha'
    searchParams.append('page', params.page.toString());
    searchParams.append('size', params.size.toString());

    return this.request<PaginatedResponse<Envio>>(
      `/envios/search?${searchParams.toString()}`
    );
  }

  async getEnvio(id: string | number): Promise<Envio> {
    return this.request<Envio>(`/envios/${id}`);
  }

  async getEnvioCompleto(id: string | number): Promise<Envio> {
    return this.request<Envio>(`/envios/buscar/${id}`);
  }

  async crearEnvio(envio: EnvioRequestDTO): Promise<Envio> {
    return this.request<Envio>('/envios', {
      method: 'POST',
      body: JSON.stringify(envio),
    });
  }

  // async actualizarEnvio(id: string | number, data: EnvioUpdateDTO): Promise<Envio> {
  //   return this.request<Envio>(`/envios/${id}`, {
  //     method: 'PUT',
  //     body: JSON.stringify(data),
  //   });
  // }

  async actualizarEnvio(id: string | number, data: EnvioUpdateDTO): Promise<Envio> {
    return this.request<Envio>(`/envios/${id}/operativo`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  }

  async cancelarEnvio(id: string | number): Promise<Envio> {
    return this.request<Envio>(`/envios/${id}/cancelar`, {
      method: 'PUT',
    });
  }

  async getHistorialEnvio(id: string | number): Promise<RegistroHistorial[]> {
    return this.request<RegistroHistorial[]>(`/envios/${id}/historial`);
  }

  async getHistorialCompleto(): Promise<RegistroHistorial[]> {
    return this.request<RegistroHistorial[]>('/envios/historial-completo');
  }

  // === CHOFER ===
  async getMisAsignaciones(): Promise<EnvioChofer[]> {
    return this.request<EnvioChofer[]>('/chofer/envios');
  }

  async cambiarEstadoChofer(id: string | number, nuevoEstado: string): Promise<Envio> {
    return this.request<Envio>(`/envios/${id}/estado?nuevoEstado=${nuevoEstado}`, {
      method: 'PATCH',
    });
  }

  async reportarIncidencia(id: string | number, incidencia: IncidenciaDTO): Promise<void> {
    return this.request<void>(`/envios/${id}/incidencias`, {
      method: 'POST',
      body: JSON.stringify(incidencia),
    });
  }


  // === CATALOGOS ===
  async getMetadatos(): Promise<MetadatosCatalogo> {
    return this.request<MetadatosCatalogo>('/catalogos/metadatos');
  }

  async getEmpresas(): Promise<Empresa[]> {
    return this.request<Empresa[]>('/catalogos/empresas');
  }

  async getEstablecimientos(cuit: string): Promise<Establecimiento[]> {
    return this.request<Establecimiento[]>(`/catalogos/establecimientos?cuit=${cuit}`);
  }

  async getChoferes(): Promise<Chofer[]> {
    return this.request<Chofer[]>('/catalogos/choferes');
  }

  async getCamiones(): Promise<Camion[]> {
    return this.request<Camion[]>('/catalogos/camiones');
  }

  // Envíos pendientes de asignación (sin chofer ni camión)
  async getEnviosSinAsignar(): Promise<Envio[]> {
    return this.request<Envio[]>('/envios/sin-asignar');
  }

  // Asignar chofer y camión juntos
  async asignarTransporte(
    idEnvio: string,
    data: { id_chofer: number; patente_camion: string }
  ): Promise<Envio> {
    return this.request<Envio>(`/envios/${idEnvio}/asignar-transporte`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  }
}

export const api = new ApiClient();
