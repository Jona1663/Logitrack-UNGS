'use client';

import Link from 'next/link';
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

interface GlobalHistorialTableProps {
  historial: RegistroHistorial[];
}

export function GlobalHistorialTable({ historial }: GlobalHistorialTableProps) {
  // Ordenar por fecha descendente (más reciente primero)
  const historialOrdenado = [...historial].sort(
    (a, b) => new Date(b.fecha_hora).getTime() - new Date(a.fecha_hora).getTime()
  );

  return (
    <>
      {/* Vista Desktop - 6 Columnas */}
      <div className="hidden md:block overflow-x-auto">
        <Table>
          <TableHeader className="bg-gray-50/80">
            <TableRow className="hover:bg-transparent">
              <TableHead className="py-3 pl-6 text-xs font-bold text-muted-foreground uppercase tracking-wider">ID Reg.</TableHead>
              <TableHead className="py-3 text-xs font-bold text-muted-foreground uppercase tracking-wider">ID Rastreo</TableHead>
              <TableHead className="py-3 text-xs font-bold text-muted-foreground uppercase tracking-wider">Tipo de Modificación</TableHead>
              <TableHead className="py-3 text-xs font-bold text-muted-foreground uppercase tracking-wider">Fecha</TableHead>
              <TableHead className="py-3 text-xs font-bold text-muted-foreground uppercase tracking-wider">Hora</TableHead>
              <TableHead className="py-3 pr-6 text-right text-xs font-bold text-muted-foreground uppercase tracking-wider">Responsable Operativo</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {historialOrdenado.map((registro, index) => (
              <TableRow key={registro.id_historial || `desk-${index}`} className="hover:bg-gray-50/50 transition-colors">
                <TableCell className="py-3 pl-6 text-sm font-bold text-muted-foreground">
                  #{registro.id_historial}
                </TableCell>
                <TableCell className="py-3">
                  <Link href={`/envios/${registro.id_envio}`} className="text-[#198754] font-bold hover:underline">
                    {registro.id_envio}
                  </Link>
                </TableCell>
                <TableCell className="py-3">
                  <EventoCell registro={registro} />
                </TableCell>
                <TableCell className="py-3 text-sm text-muted-foreground">
                  {formatearFecha(registro.fecha_hora)}
                </TableCell>
                <TableCell className="py-3 text-sm text-muted-foreground">
                  {formatearHora(registro.fecha_hora)}
                </TableCell>
                <TableCell className="py-3 pr-6 text-sm text-muted-foreground text-right">
                  <span className="inline-flex items-center justify-center gap-1.5 bg-gray-50 border rounded-md px-2.5 py-1">
                    <User className="h-3.5 w-3.5" />
                    {registro.username || 'Sistema'}
                  </span>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* Vista Mobile - Replicando tu CSS de Tarjetas Apiladas */}
      <div className="md:hidden space-y-4 p-4">
        {historialOrdenado.map((registro, index) => (
          <div key={registro.id_historial || `desk-${index}`} className="bg-white border rounded-xl shadow-sm p-4 flex flex-col">
            <div className="flex justify-between items-center border-b border-dashed border-gray-100 pb-2 mb-2">
              <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">ID Reg.</span>
              <span className="text-sm font-bold text-muted-foreground">#{registro.id_historial}</span>
            </div>
            <div className="flex justify-between items-center border-b border-dashed border-gray-100 pb-2 mb-2">
              <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">ID Rastreo</span>
              <Link href={`/envios/${registro.id_envio}`} className="text-[#198754] font-bold text-sm">
                {registro.id_envio}
              </Link>
            </div>
            <div className="flex flex-col border-b border-dashed border-gray-100 pb-2 mb-2 gap-1">
              <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Modificación</span>
              <div className="text-right flex justify-end">
                <EventoCell registro={registro} isMobile />
              </div>
            </div>
            <div className="flex justify-between items-center border-b border-dashed border-gray-100 pb-2 mb-2">
              <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Fecha y Hora</span>
              <span className="text-sm text-muted-foreground">{formatearFecha(registro.fecha_hora)} - {formatearHora(registro.fecha_hora)}</span>
            </div>
            <div className="flex justify-between items-center pt-1">
              <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Responsable</span>
              <span className="text-sm text-muted-foreground flex items-center gap-1 bg-gray-50 border rounded-md px-2 py-0.5">
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
  const estadoAnterior = registro.estado_anterior ? normalizarEnum(registro.estado_anterior) : null;
  const estadoNuevo = normalizarEnum(registro.estado_nuevo);

  if (estadoAnterior) {
    return (
      <span className="text-sm text-gray-900 flex items-center gap-1.5">
        <span className="font-bold">{estadoAnterior}</span>
        <ArrowRight className="h-4 w-4 text-muted-foreground" />
        <span className="font-bold">{estadoNuevo}</span>
      </span>
    );
  }

  return (
    <span className="text-sm font-bold text-gray-900">
      Creación <ArrowRight className="h-4 w-4 text-muted-foreground inline mx-1" /> {estadoNuevo}
    </span>
  );
}