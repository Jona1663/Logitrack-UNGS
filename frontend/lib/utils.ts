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
 * Formatea una fecha ISO a formato legible
 */
export function formatearFecha(fechaISO: string): string {
  if (!fechaISO) return '-';
  const fecha = new Date(fechaISO);
  return fecha.toLocaleDateString('es-AR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

/**
 * Formatea una fecha ISO a hora legible
 */
export function formatearHora(fechaISO: string): string {
  if (!fechaISO) return '-';
  const fecha = new Date(fechaISO);
  return fecha.toLocaleTimeString('es-AR', {
    hour: '2-digit',
    minute: '2-digit',
  });
}

/**
 * Formatea fecha y hora completas
 */
export function formatearFechaHora(fechaISO: string): string {
  if (!fechaISO) return '-';
  return `${formatearFecha(fechaISO)} ${formatearHora(fechaISO)}`;
}

/**
 * Obtiene el nombre completo del chofer
 */
export function getNombreChofer(chofer: { persona_asociada: { nombre: string; apellido: string } }): string {
  if (!chofer?.persona_asociada) return '-';
  return `${chofer.persona_asociada.nombre} ${chofer.persona_asociada.apellido}`;
}

/**
 * Formatea peso en kg
 */
export function formatearPeso(kg: number): string {
  if (!kg) return '0 kg';
  return `${kg.toLocaleString('es-AR')} kg`;
}
