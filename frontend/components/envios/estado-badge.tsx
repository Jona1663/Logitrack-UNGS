import {
  Clock,
  Truck,
  MapPin,
  Package,
  CheckCircle,
  XCircle,
} from 'lucide-react';
import type { EstadoEnvio } from '@/types';
import { ESTADO_CONFIG } from '@/lib/constants';
import { cn } from '@/lib/utils';

interface EstadoBadgeProps {
  estado: EstadoEnvio;
  showIcon?: boolean;
  className?: string;
}

const iconMap = {
  Clock,
  Truck,
  MapPin,
  Package,
  CheckCircle,
  XCircle,
};

export function EstadoBadge({ estado, showIcon = true, className }: EstadoBadgeProps) {
  const config = ESTADO_CONFIG[estado];
  const IconComponent = iconMap[config.icon as keyof typeof iconMap];

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium',
        config.bgColor,
        config.color,
        className
      )}
    >
      {showIcon && IconComponent && <IconComponent className="h-3.5 w-3.5" />}
      {config.label}
    </span>
  );
}
