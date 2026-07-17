'use client';

import { Truck, MapPin, Package, CheckCircle, Trophy, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import type { EstadoEnvio } from '@/types';
import { FLUJO_LOGISTICO } from '@/lib/constants';
import { cn } from '@/lib/utils';

interface ActionButtonProps {
  estadoActual: EstadoEnvio;
  onClick: () => void;
  isLoading?: boolean;
  disabled?: boolean;
}

const iconMap = {
  Truck,
  MapPin,
  Package,
  CheckCircle,
  Trophy,
};

export function ActionButton({
  estadoActual,
  onClick,
  isLoading,
  disabled,
}: ActionButtonProps) {
  const flujo = FLUJO_LOGISTICO[estadoActual];
  const IconComponent = iconMap[flujo.icon as keyof typeof iconMap] || Truck;
  const isCompleted = !flujo.siguiente;

  return (
    <Button
      onClick={onClick}
      disabled={disabled || isLoading || isCompleted}
      size="lg"
      className={cn(
        'w-full h-14 text-lg font-semibold transition-all',
        isCompleted
          ? 'bg-green-600 hover:bg-green-600 cursor-default'
          : 'bg-gradient-to-r from-[#1b4332] to-[#2d6a4f] hover:from-[#2d6a4f] hover:to-[#40916c]'
      )}
    >
      {isLoading ? (
        <>
          <Loader2 className="mr-2 h-5 w-5 animate-spin" />
          Procesando...
        </>
      ) : (
        <>
          <IconComponent className="mr-2 h-5 w-5" />
          {flujo.btnText}
        </>
      )}
    </Button>
  );
}
