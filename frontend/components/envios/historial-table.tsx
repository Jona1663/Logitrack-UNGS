'use client';

import { ArrowRight, User } from 'lucide-react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import type { RegistroHistorial } from '@/types';
import { formatearFecha, formatearHora, normalizarEnum } from '@/lib/utils';

interface HistorialTableProps {
  historial: RegistroHistorial[];
}

export function HistorialTable({ historial }: HistorialTableProps) {
  if (historial.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground text-sm">
        Sin registros de cambios en la ruta
      </div>
    );
  }

  // Ordenar por fecha descendente (más reciente primero)
  const historialOrdenado = [...historial].sort(
    (a, b) => new Date(b.fecha_hora).getTime() - new Date(a.fecha_hora).getTime()
  );

  return (
    <>
      {/* Vista Desktop */}
      <div className="hidden md:block overflow-x-auto">
        <Table>
          <TableHeader className="bg-gray-50/80">
            <TableRow className="hover:bg-transparent">
              <TableHead className="py-3 pl-6 text-xs font-bold text-muted-foreground uppercase tracking-wider">Evento</TableHead>
              <TableHead className="py-3 text-xs font-bold text-muted-foreground uppercase tracking-wider">Fecha</TableHead>
              <TableHead className="py-3 text-xs font-bold text-muted-foreground uppercase tracking-wider">Hora</TableHead>
              <TableHead className="py-3 pr-6 text-xs font-bold text-muted-foreground uppercase tracking-wider">Responsable</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {historialOrdenado.map((registro, index) => (
              <TableRow key={registro.id_historial || `desk-${index}`} className="hover:bg-gray-50/50 transition-colors">
                <TableCell className="py-3 pl-6">
                  <EventoCell registro={registro} />
                </TableCell>
                <TableCell className="py-3 text-sm text-muted-foreground">
                  {formatearFecha(registro.fecha_hora)}
                </TableCell>
                <TableCell className="py-3 text-sm text-muted-foreground">
                  {formatearHora(registro.fecha_hora)}
                </TableCell>
                <TableCell className="py-3 pr-6 text-sm text-muted-foreground flex items-center gap-1.5">
                  <User className="h-3.5 w-3.5" />
                  {registro.username || 'Sistema'}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* Vista Mobile - Replicando tu CSS de Tarjetas Apiladas */}
      <div className="md:hidden space-y-4 p-4">
        {historialOrdenado.map((registro, index) => (
          <div
            key={registro.id_historial || `desk-${index}`}
            className="bg-white border rounded-xl shadow-sm p-4 flex flex-col"
          >
            <div className="flex justify-between items-center border-b border-dashed border-gray-100 pb-2 mb-2">
              <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Evento</span>
              <div className="text-right">
                <EventoCell registro={registro} isMobile />
              </div>
            </div>
            <div className="flex justify-between items-center border-b border-dashed border-gray-100 pb-2 mb-2">
              <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Fecha</span>
              <span className="text-sm text-muted-foreground">{formatearFecha(registro.fecha_hora)}</span>
            </div>
            <div className="flex justify-between items-center border-b border-dashed border-gray-100 pb-2 mb-2">
              <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Hora</span>
              <span className="text-sm text-muted-foreground">{formatearHora(registro.fecha_hora)}</span>
            </div>
            <div className="flex justify-between items-center pt-1">
              <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Responsable</span>
              <span className="text-sm text-muted-foreground flex items-center gap-1">
                <User className="h-3.5 w-3.5" />
                {registro.username || 'Sistema'}
              </span>
            </div>
          </div>
        ))}
      </div>
    </>
  );
}

function EventoCell({ registro, isMobile = false }: { registro: RegistroHistorial, isMobile?: boolean }) {
  const estadoAnterior = registro.estado_anterior
    ? normalizarEnum(registro.estado_anterior)
    : null;
  const estadoNuevo = normalizarEnum(registro.estado_nuevo);

  if (estadoAnterior) {
    return (
      <span className="text-sm font-medium text-gray-900 flex items-center gap-1.5 justify-end md:justify-start text-right md:text-left">
        {!isMobile && <ArrowRight className="h-4 w-4 text-[#198754]" />}
        De {estadoAnterior} a {estadoNuevo}
      </span>
    );
  }

  return (
    <span className="text-sm font-medium text-gray-900 flex items-center gap-1.5 justify-end md:justify-start text-right md:text-left">
      {!isMobile && <ArrowRight className="h-4 w-4 text-[#198754]" />}
      Envío creado y puesto en {estadoNuevo}
    </span>
  );
}