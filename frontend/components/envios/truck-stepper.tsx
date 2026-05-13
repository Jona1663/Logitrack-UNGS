'use client';

import { cn } from '@/lib/utils';
import type { EstadoEnvio } from '@/types';
import {
  ClipboardList, Truck, Building2, Package, CheckCircle, Ban
} from 'lucide-react';

interface TruckStepperProps {
  estadoActual: EstadoEnvio;
}

const PASOS: { key: EstadoEnvio; label: string; Icon: React.ElementType }[] = [
  { key: 'PENDIENTE', Icon: ClipboardList, label: 'Pendiente' },
  { key: 'EN_TRANSITO', Icon: Truck, label: 'En tránsito' },
  { key: 'EN_PUNTO_DE_RECOLECCION', Icon: Building2, label: 'En recolección' },
  { key: 'EN_REPARTO', Icon: Package, label: 'En reparto' },
  { key: 'ENTREGADO', Icon: CheckCircle, label: 'Entregado' },
];

export function TruckStepper({ estadoActual }: TruckStepperProps) {
  if (estadoActual === 'CANCELADO') {
    return (
      <div className="flex items-center gap-3 bg-red-50 border border-red-100 rounded-xl p-4">
        <Ban className="h-6 w-6 text-red-500 shrink-0" />
        <p className="text-sm text-red-600 font-medium m-0">
          Envío cancelado — El viaje ha sido detenido.
        </p>
      </div>
    );
  }

  const N = PASOS.length; // 5

  // El centro de cada ícono está desplazado un "medio paso" desde los bordes
  const HALF_STEP = 100 / (N * 2);        // 10%
  const LINE_LEFT = HALF_STEP;           // la línea empieza en 10%
  const LINE_WIDTH = 100 - 2 * HALF_STEP; // la línea ocupa 80%

  // Centro exacto del paso i en % del contenedor
  function stepCenter(i: number) {
    return HALF_STEP + i * (LINE_WIDTH / (N - 1));
  }

  const activeIndex = PASOS.findIndex(p => p.key === estadoActual);
  const truckPct = stepCenter(activeIndex)+0.2;
  const fillWidth = activeIndex === 0 ? 0 : stepCenter(activeIndex) - stepCenter(0);

  return (
    <div className="py-2">
      {/* Track */}
      <div className="relative flex items-start justify-between">

        {/* Línea de fondo — empieza y termina en el centro del primer/último ícono */}
        <div className="absolute top-5 h-[3px] bg-gray-200 z-0"
          style={{ left: `${LINE_LEFT}%`, width: `${LINE_WIDTH}%` }} />

        {/* Línea de progreso */}
        <div className="absolute top-5 h-[3px] bg-[#198754] z-[1] transition-all duration-500"
          style={{ left: `${LINE_LEFT}%`, width: `${fillWidth}%` }} />

        {/* Camión */}
        <div className="absolute z-[3] -translate-x-1/2 transition-all duration-500"
          style={{ left: `${truckPct}%`, top: '5px' }}>
          <TruckIcon />
        </div>

        {/* Pasos */}
        {PASOS.map(({ key, label, Icon }, i) => {
          const isDone = i < activeIndex;
          const isActive = i === activeIndex;
          return (
            <div key={key} className="relative z-[2] flex flex-col items-center gap-2 flex-1">
              <div className={cn(
                'w-10 h-10 rounded-full border-[3px] flex items-center justify-center transition-all duration-300',
                isDone && 'bg-[#198754] border-[#198754] text-white',
                isActive && 'bg-white border-amber-400 text-amber-400 shadow-[0_0_0_4px_rgba(245,158,11,0.15)]',
                !isDone && !isActive && 'bg-white border-gray-200 text-gray-400'
              )}>
                <Icon className="h-4 w-4" strokeWidth={2.5} />
              </div>
              <span className={cn(
                'text-[11px] text-center leading-tight max-w-[72px]',
                isDone && 'text-[#198754] font-medium',
                isActive && 'text-gray-900 font-medium',
                !isDone && !isActive && 'text-gray-400'
              )}>
                {label}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function TruckIcon() {
  return (
    <svg width="36" height="28" viewBox="0 0 36 28" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect x="0" y="5" width="22" height="16" rx="2" fill="#198754" />
      <rect x="22" y="9" width="10" height="12" rx="1.5" fill="#198754" />
      <path d="M22 11 L30 11 L32 17 L22 17 Z" fill="#a7f3d0" />
      <rect x="2" y="8" width="6" height="4" rx="1" fill="#a7f3d0" opacity="0.8" />
      <rect x="10" y="8" width="10" height="4" rx="1" fill="#a7f3d0" opacity="0.8" />
      <circle cx="7" cy="23" r="3.5" fill="#1f2937" stroke="#d1fae5" strokeWidth="1.5" />
      <circle cx="7" cy="23" r="1.2" fill="#d1fae5" />
      <circle cx="26" cy="23" r="3.5" fill="#1f2937" stroke="#d1fae5" strokeWidth="1.5" />
      <circle cx="26" cy="23" r="1.2" fill="#d1fae5" />
    </svg>
  );
}