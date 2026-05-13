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
  razon_social: string;
  tipo_empresa: string;
}

export interface Establecimiento {
  id_establecimiento: number;
  nombre_lugar: string;
  direccion: string;
  empresa: Empresa;
}

export interface PersonaAsociada {
  nombre: string;
  apellido: string;
}

export interface Chofer {
  id_chofer: number;
  nro_licencia: string;
  persona_asociada: PersonaAsociada;
}

export interface Camion {
  patente: string;
  tara_vacio_kg: number;
}

export interface Envio {
  id_envio: string | number;
  // tracking_ctg: string;
  cpe: string;
  estado_actual: EstadoEnvio;
  prioridad_ia: Prioridad;
  tipo_grano: TipoGrano;
  kg_origen: number;
  fecha_creacion: string;
  fecha_estimada_llegada?: string;  // fecha_entrega_estimada?: string;
  origen: Establecimiento;
  destino: Establecimiento;
  chofer: Chofer;
  camion: Camion;
}

// export interface EnvioDetalleResponseDTO {
//   id_envio: string | number;
//   cpe: string;
//   estado_actual: EstadoEnvio;
//   tipo_grano: TipoGrano;
//   kg_origen: number;
//   prioridad_ia: Prioridad;

//   // Datos de origen y destino
//   origen_nombre: string;
//   origen_direccion: string;
//   destino_nombre: string;
//   destino_direccion: string;

//   // Datos del chofer
//   chofer_nombre: string;
//   chofer_apellido: string;
// }

export interface RegistroHistorial {
  id_historial: number;
  id_envio: string;
  id_usuario?: number | null;
  username?: string;
  estado_anterior: string | null;
  estado_nuevo: string;
  fecha_hora: string;
}

// === DTOs ===
export interface EnvioRequestDTO {
  // tracking_ctg: string;
  cpe: string;
  id_origen: number;
  id_destino: number;
  id_chofer: number;
  patente_camion: string;
  tipo_grano: TipoGrano;
  prioridad_ia?: Prioridad; // Ahora es opcional
  kg_origen: number;
}

export interface EnvioUpdateDTO {
  estado?: EstadoEnvio;
  prioridad_ia?: Prioridad;
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
  nombre_lugar: string;
  direccion: string;
}

export interface EnvioChofer {
  id_envio: string;
  // tracking_ctg: string;
  cpe: string;
  estado_actual: EstadoEnvio;
  tipo_grano: TipoGrano;
  kg_origen: number;
  origen: LugarResumen;
  destino: LugarResumen;
  patente_camion: string;
  // nombre_chofer: string;
  prioridad_ia: Prioridad;   // ← agregar
}