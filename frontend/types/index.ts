// === ENUMS ===
export type EstadoEnvio =
  | 'PENDIENTE'
  | 'EN_TRANSITO'
  | 'EN_PUNTO_DE_RECOLECCION'
  | 'EN_REPARTO'
  | 'ENTREGADO'
  | 'CANCELADO';

export type Prioridad = 'ALTA' | 'MEDIA' | 'BAJA';

export type TipoGrano =
  | 'SOJA'
  | 'TRIGO'
  | 'MAIZ'
  | 'GIRASOL'
  | 'SORGO'
  | 'CEBADA'
  | 'AVENA';

export type RolUsuario =
  | 'ROLE_OPERADOR'
  | 'ROLE_SUPERVISOR'
  | 'ROLE_ADMIN'
  | 'ROLE_CHOFER';

// === AUTH ===
export interface Usuario {
  username: string;
  rol: RolUsuario;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string;
  rol: string;
}

// === ENTIDADES ===
export interface Empresa {
  cuit: string;
  razonSocial: string;
  tipoEmpresa: string;
}

export interface Establecimiento {
  idEstablecimiento: number;
  nombreLugar: string;
  direccion: string;
  empresa: Empresa;
}

export interface PersonaAsociada {
  nombre: string;
  apellido: string;
}

export interface Chofer {
  idChofer: number;
  nroLicencia: string;
  personaAsociada: PersonaAsociada;
}

export interface Camion {
  patente: string;
  taraVacioKg: number;
}

export interface Envio {
  idEnvio: string | number;
  // trackingCtg: string;
  cpe: string;
  estadoActual: EstadoEnvio;
  prioridadIa: Prioridad;
  tipoGrano: TipoGrano;
  kgOrigen: number;
  fechaCreacion: string;
  fechaEstimadaLlegada?: string;
  origen: Establecimiento;
  destino: Establecimiento;
  chofer: Chofer;
  camion: Camion;
}

export interface RegistroHistorial {
  idHistorial: number;
  idEnvio: string;
  idUsuario?: number | null;
  username?: string;
  estadoAnterior: string | null;
  estadoNuevo: string;
  fechaHora: string;
}

// === DTOs ===
export interface EnvioRequestDTO {
  // trackingCtg: string;
  cpe: string;
  idOrigen: number;
  idDestino: number;
  idChofer: number;
  patenteCamion: string;
  tipoGrano: TipoGrano;
  prioridadIa?: Prioridad; // Ahora es opcional
  kgOrigen: number;
}

export interface EnvioUpdateDTO {
  estado?: EstadoEnvio;
  prioridadIa?: Prioridad;
}
export interface IncidenciaDTO {
  descripcion: string;
}

export interface BusquedaEnviosParams {
  query?: string;
  estado?: EstadoEnvio | '';
  fecha?: string; // <-- Unificamos a una sola fecha
  page: number;
  size: number;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

// === CATALOGOS ===
export interface MetadatosCatalogo {
  tiposGrano: TipoGrano[];
  estados: EstadoEnvio[];
  prioridades: Prioridad[];
}

// === CHOFER ===
export interface LugarResumen {
  nombreLugar: string;
  direccion: string;
}

export interface EnvioChofer {
  idEnvio: string;
  // trackingCtg: string;
  cpe: string;
  estadoActual: EstadoEnvio;
  tipoGrano: TipoGrano;
  kgOrigen: number;
  origen: LugarResumen;
  destino: LugarResumen;
  patenteCamion: string;
  // nombreChofer: string;
  prioridadIa: Prioridad;   // ← agregar
}