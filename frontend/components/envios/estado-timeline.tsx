'use client';

import {
  ClipboardCheck,
  Truck,
  Building2,
  Package,
  Flag,
  XCircle,
} from 'lucide-react';
import type { EstadoEnvio } from '@/types';
import { ESTADOS_TIMELINE, ESTADO_CONFIG } from '@/lib/constants';
import { cn } from '@/lib/utils';

interface EstadoTimelineProps {
  estadoActual: EstadoEnvio;
}

// Mapeamos los íconos de config a sus equivalentes de Lucide (similares a Bootstrap Icons)
const iconMap = {
  Clock: ClipboardCheck,   // Reemplaza bi-clipboard-check
  Truck: Truck,            // Reemplaza bi-truck
  MapPin: Building2,       // Reemplaza bi-buildings
  Package: Package,        // Reemplaza bi-box-seam
  CheckCircle: Flag,       // Reemplaza bi-flag
  XCircle: XCircle,
};

const descripciones = {
  PENDIENTE: "Orden de carga registrada. Esperando camión en el origen.",
  EN_TRANSITO: "El camión ha salido del establecimiento origen y se encuentra en ruta.",
  EN_PUNTO_DE_RECOLECCION: "Llegada a planta, control de calidad y pesaje.",
  EN_REPARTO: "La carga está en distribución hacia el destino final.",
  ENTREGADO: "Descarga de granos completada de forma exitosa.",
  CANCELADO: "El viaje ha sido cancelado y detenido."
};

export function EstadoTimeline({ estadoActual }: EstadoTimelineProps) {
  if (estadoActual === 'CANCELADO') {
    return (
      <div className="flex items-center justify-center py-8 bg-red-50/50 rounded-xl border border-red-100">
        <div className="flex flex-col items-center gap-3 text-red-600">
          <div className="p-4 bg-red-100 rounded-full">
            <XCircle className="h-8 w-8" />
          </div>
          <span className="font-bold text-lg">Envío Cancelado</span>
          <p className="text-sm text-red-600/75">{descripciones.CANCELADO}</p>
        </div>
      </div>
    );
  }

  const estadoIndex = ESTADOS_TIMELINE.indexOf(estadoActual);

  return (
    <div className="py-2">
      <div className="relative pl-14 md:pl-16 ml-2 md:ml-4">
        {ESTADOS_TIMELINE.map((estado, index) => {
          const config = ESTADO_CONFIG[estado];
          const IconComponent = iconMap[config.icon as keyof typeof iconMap];
          const isCompleted = index < estadoIndex;
          const isActive = index === estadoIndex;
          const isPending = index > estadoIndex;
          const isLast = index === ESTADOS_TIMELINE.length - 1;

          return (
            <div key={estado} className="relative mb-8 last:mb-0">
              
              {/* Línea Conectora (Fondo gris o verde si está completado) */}
              {!isLast && (
                <div 
                  className={cn(
                    "absolute left-[-2.25rem] top-10 bottom-[-2.5rem] w-[3px] -translate-x-1/2 z-0 transition-colors duration-300",
                    isCompleted ? "bg-[#198754]" : "bg-gray-200"
                  )} 
                />
              )}

              {/* Ícono Circular */}
              <div 
                className={cn(
                  "absolute left-[-3.5rem] top-0 w-10 h-10 rounded-full flex items-center justify-center border-[3px] z-10 transition-all duration-300",
                  isCompleted ? "bg-[#198754] border-[#198754] text-white" : 
                  isActive ? "bg-amber-400 border-amber-400 text-gray-900 animate-pulse-warning" : 
                  "bg-gray-50 border-gray-200 text-gray-400"
                )}
              >
                <IconComponent className="h-[18px] w-[18px]" strokeWidth={2.5} />
              </div>

              {/* Tarjeta de Contenido */}
              <div 
                className={cn(
                  "bg-white border rounded-xl p-4 md:p-5 transition-all duration-300",
                  isActive ? "border-amber-400/50 shadow-[0_0.25rem_0.5rem_rgba(245,157,11,0.15)]" : "border-gray-100 shadow-sm"
                )}
              >
                <div className="flex flex-wrap justify-between items-center mb-1 gap-2">
                  <h6 className={cn(
                    "mb-0 font-bold text-base md:text-lg",
                    isCompleted ? "text-[#198754]" :
                    isActive ? "text-gray-900" :
                    "text-gray-400"
                  )}>
                    {estado === 'PENDIENTE' && 'Pendiente - Carga Asignada'}
                    {estado === 'EN_TRANSITO' && 'En Tránsito'}
                    {estado === 'EN_PUNTO_DE_RECOLECCION' && 'En Punto de Recolección'}
                    {estado === 'EN_REPARTO' && 'En Reparto'}
                    {estado === 'ENTREGADO' && 'Entregado'}
                  </h6>
                  
                  {isActive && (
                    <span className="bg-amber-400 text-amber-950 text-xs font-bold px-2.5 py-1 rounded-md shadow-sm">
                      Actual
                    </span>
                  )}
                </div>
                
                <p className={cn(
                  "text-sm mb-0 mt-1",
                  isCompleted || isActive ? "text-gray-600" : "text-gray-400/80"
                )}>
                  {descripciones[estado as keyof typeof descripciones]}
                </p>
              </div>

            </div>
          );
        })}
      </div>
    </div>
  );
}