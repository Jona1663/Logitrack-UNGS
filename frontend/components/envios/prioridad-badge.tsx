import type { Prioridad } from '@/types';
import { PRIORIDAD_CONFIG } from '@/lib/constants';
import { cn } from '@/lib/utils';

interface PrioridadBadgeProps {
  prioridad: Prioridad;
  className?: string;
}

export function PrioridadBadge({ prioridad, className }: PrioridadBadgeProps) {
  const config = PRIORIDAD_CONFIG[prioridad];

  return (
    <span
      className={cn(
        'inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium',
        config.bgColor,
        config.color,
        className
      )}
    >
      {config.label}
    </span>
  );
}
