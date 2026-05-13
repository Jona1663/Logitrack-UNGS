import type { EstadoEnvio, Prioridad, RolUsuario } from '@/types';

// === FLUJO LOGISTICO DEL CHOFER ===
export const FLUJO_LOGISTICO: Record<
  EstadoEnvio,
  { siguiente: EstadoEnvio | null; btnText: string; confirmacionText: string; icon: string }
> = {
  PENDIENTE: { siguiente: 'EN_TRANSITO', btnText: 'Iniciar Viaje', confirmacionText: 'El viaje comenzará y quedará registrado como en tránsito.', icon: 'Truck' },
  EN_TRANSITO: { siguiente: 'EN_PUNTO_DE_RECOLECCION', btnText: 'Confirmar llegada a carga', confirmacionText: 'Se registrará tu llegada al punto de recolección.', icon: 'MapPin' },
  EN_PUNTO_DE_RECOLECCION: { siguiente: 'EN_REPARTO', btnText: 'Iniciar entrega', confirmacionText: 'Se confirmará que la carga está completa y comenzará el reparto hacia el destino.', icon: 'Package' },
  EN_REPARTO: { siguiente: 'ENTREGADO', btnText: 'Confirmar entrega al destino', confirmacionText: 'Se registrará la entrega exitosa de la mercadería en el destino.', icon: 'CheckCircle' },
  ENTREGADO: { siguiente: null, btnText: 'Viaje finalizado', confirmacionText: '', icon: 'Trophy' },
  CANCELADO: { siguiente: null, btnText: 'Envío cancelado', confirmacionText: '', icon: 'XCircle' },
};

// === CONFIGURACION DE ESTADOS ===
export const ESTADO_CONFIG: Record<
  EstadoEnvio,
  { label: string; color: string; bgColor: string; icon: string }
> = {
  PENDIENTE: {
    label: 'Pendiente',
    color: 'text-gray-700',
    bgColor: 'bg-gray-100',
    icon: 'Clock',
  },
  EN_TRANSITO: {
    label: 'En Transito',
    color: 'text-blue-700',
    bgColor: 'bg-blue-100',
    icon: 'Truck',
  },
  EN_PUNTO_DE_RECOLECCION: {
    label: 'En Punto de Recolección',
    color: 'text-orange-700',
    bgColor: 'bg-orange-100',
    icon: 'MapPin',
  },
  EN_REPARTO: {
    label: 'En Reparto',
    color: 'text-purple-700',
    bgColor: 'bg-purple-100',
    icon: 'Package',
  },
  ENTREGADO: {
    label: 'Entregado',
    color: 'text-green-700',
    bgColor: 'bg-green-100',
    icon: 'CheckCircle',
  },
  CANCELADO: {
    label: 'Cancelado',
    color: 'text-red-700',
    bgColor: 'bg-red-100',
    icon: 'XCircle',
  },
};

// === CONFIGURACION DE PRIORIDADES ===
export const PRIORIDAD_CONFIG: Record<
  Prioridad,
  { label: string; color: string; bgColor: string }
> = {
  ALTA: { label: 'Alta', color: 'text-red-700', bgColor: 'bg-red-100' },
  MEDIA: { label: 'Media', color: 'text-yellow-700', bgColor: 'bg-yellow-100' },
  BAJA: { label: 'Baja', color: 'text-green-700', bgColor: 'bg-green-100' },
};

// === PERMISOS POR ROL ===
export const PERMISOS_POR_ROL: Record<
  RolUsuario,
  {
    crearEnvio: boolean;
    editarEstado: boolean;
    editarPrioridad: boolean;
    verAuditoria: boolean;
    panelChofer: boolean;
    verMenu: boolean;
    asignarTransporte: boolean;
  }
> = {
  ROLE_OPERADOR: {
    crearEnvio: true,
    editarEstado: true,
    editarPrioridad: false,
    verAuditoria: false,
    panelChofer: false,
    verMenu: true,
    asignarTransporte: true,
  },
  ROLE_SUPERVISOR: {
    crearEnvio: true,
    editarEstado: true,
    editarPrioridad: true,
    verAuditoria: true,
    panelChofer: false,
    verMenu: true,
    asignarTransporte: true,
  },
  ROLE_ADMIN: {
    crearEnvio: true,
    editarEstado: true,
    editarPrioridad: true,
    verAuditoria: true,
    panelChofer: false,
    verMenu: true,
    asignarTransporte: true,
  },
  ROLE_CHOFER: {
    crearEnvio: false,
    editarEstado: false,
    editarPrioridad: false,
    verAuditoria: false,
    panelChofer: true,
    verMenu: false,
    asignarTransporte: false,
  },
};

// === ORDEN DE ESTADOS PARA TIMELINE ===
export const ESTADOS_TIMELINE: EstadoEnvio[] = [
  'PENDIENTE',
  'EN_TRANSITO',
  'EN_PUNTO_DE_RECOLECCION',
  'EN_REPARTO',
  'ENTREGADO',
];

// === TIPOS DE GRANO ===
export const TIPOS_GRANO = [
  'SOJA',
  'TRIGO',
  'MAIZ',
  'GIRASOL',
  'SORGO',
  'CEBADA',
  'AVENA',
] as const;

// === ESTADOS PARA FILTROS ===
export const ESTADOS_FILTRO = [
  { value: '', label: 'Todos los estados' },
  { value: 'PENDIENTE', label: 'Pendiente' },
  { value: 'EN_TRANSITO', label: 'En Transito' },
  { value: 'EN_PUNTO_DE_RECOLECCION', label: 'En Punto de Carga' },
  { value: 'EN_REPARTO', label: 'En Reparto' },
  { value: 'ENTREGADO', label: 'Entregado' },
  { value: 'CANCELADO', label: 'Cancelado' },
];
