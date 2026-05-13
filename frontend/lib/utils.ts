import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'
import type { EstadoEnvio } from '@/types'
import { ESTADO_CONFIG } from './constants'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * Normaliza un enum de Java para mostrar en UI
 * "EN_TRANSITO" -> "En Transito"
 */
export function normalizarEnum(valorEnum: string): string {
  if (!valorEnum) return '';
  return valorEnum
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

/**
 * Convierte texto UI a enum Java
 * "En Transito" -> "EN_TRANSITO"
 */
export function enumParaJava(textoSelect: string): string {
  if (!textoSelect) return '';
  return textoSelect.toUpperCase().replace(/ /g, '_');
}

/**
 * Retorna la configuracion de estado (label, color, bgColor, icon)
 */
export function getEstadoConfig(estado: EstadoEnvio) {
  return ESTADO_CONFIG[estado] || ESTADO_CONFIG.PENDIENTE;
}

/**
 * Fuerza interpretación UTC agregando 'Z' si el backend no lo incluye,
 * luego convierte a hora de Argentina (UTC-3).
 */
function parsearFechaUTC(fechaISO: string): Date {
  if (!fechaISO) return new Date();
  // Si ya tiene zona horaria (Z, +00:00, -03:00, etc.), no tocar
  const tieneZona = /Z|[+-]\d{2}:\d{2}$/.test(fechaISO);
  return new Date(tieneZona ? fechaISO : fechaISO + 'Z');
}

export function formatearFecha(fechaISO: string): string {
  if (!fechaISO) return '-';
  return parsearFechaUTC(fechaISO).toLocaleDateString('es-AR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    timeZone: 'America/Argentina/Buenos_Aires',
  });
}

export function formatearHora(fechaISO: string): string {
  if (!fechaISO) return '-';
  return parsearFechaUTC(fechaISO).toLocaleTimeString('es-AR', {
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'America/Argentina/Buenos_Aires',
  });
}

export function formatearFechaHora(fechaISO: string): string {
  if (!fechaISO) return '-';
  return `${formatearFecha(fechaISO)} ${formatearHora(fechaISO)}`;
}
/**
 * Obtiene el nombre completo del chofer
 */
export function getNombreChofer(chofer: { personaAsociada: { nombre: string; apellido: string } }): string {
  if (!chofer?.personaAsociada) return '-';
  return `${chofer.personaAsociada.nombre} ${chofer.personaAsociada.apellido}`;
}
/**
 * Formatea peso en kg
 */
export function formatearPeso(kg: number): string {
  if (!kg) return '0 kg';
  return `${kg.toLocaleString('es-AR')} kg`;
}

