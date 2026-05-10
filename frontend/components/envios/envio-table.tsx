'use client';

import Link from 'next/link';
import { Eye, MapPin, Pencil, XCircle, AlertTriangle } from 'lucide-react';
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { EstadoBadge } from './estado-badge';
import type { Envio } from '@/types';
import { normalizarEnum } from '@/lib/utils';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { useState } from 'react';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { toast } from 'sonner';

interface EnvioTableProps {
  envios: Envio[];
  onCancelar: (id: string | number) => Promise<void>;
}

export function EnvioTable({ envios, onCancelar }: EnvioTableProps) {
  const [cancelando, setCancelando] = useState(false);
  const [envioAConfirmar, setEnvioAConfirmar] = useState<Envio | null>(null);
  const handleConfirmarCancelacion = async () => {
    if (!envioAConfirmar) return;
    setCancelando(true);
    try {
      await onCancelar(envioAConfirmar.id_envio);
      toast.success(`Envío #${envioAConfirmar.id_envio} cancelado correctamente`);
    } catch {
      toast.error('No se pudo cancelar el envío', {
        description: 'Intentá nuevamente o contactá al administrador.',
      });
    } finally {
      setCancelando(false);
      setEnvioAConfirmar(null);
    }
  };
  return (
    <>
      {/* Vista Desktop - 5 Columnas Originales */}
      <div className="hidden md:block overflow-x-auto">
        <Table>
          <TableHeader className="bg-muted/30">
            <TableRow className="hover:bg-transparent">
              <TableHead className="py-4 pl-6 font-semibold">ID Rastreo</TableHead>
              <TableHead className="py-4 font-semibold">Empresa Cliente</TableHead>
              <TableHead className="py-4 font-semibold">Carga / Grano</TableHead>
              <TableHead className="py-4 font-semibold">Estado</TableHead>
              <TableHead className="py-4 pr-6 text-right font-semibold">Acciones</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {envios.map((envio) => {
              const pesoTn = envio.kg_origen ? (envio.kg_origen / 1000).toFixed(1) : '0';
              return (
                <TableRow key={envio.id_envio} className="hover:bg-muted/30 transition-colors">
                  <TableCell className="pl-6">
                    <span className="font-bold text-[#198754] block">{envio.id_envio}</span>
                    <span className="text-xs text-muted-foreground">CTG: {envio.tracking_ctg}</span>
                  </TableCell>
                  <TableCell>
                    <span className="font-medium text-gray-900 block">{envio.origen?.empresa?.razon_social || 'Sin cliente'}</span>
                    <span className="text-xs text-muted-foreground flex items-center gap-1 mt-0.5">
                      <MapPin className="h-3 w-3" /> {envio.destino?.nombre_lugar || 'Destino pendiente'}
                    </span>
                  </TableCell>
                  <TableCell>
                    <span className="font-medium text-gray-900 block">{normalizarEnum(envio.tipo_grano)}</span>
                    <span className="text-xs text-muted-foreground">{pesoTn} Tn</span>
                  </TableCell>
                  <TableCell>
                    <EstadoBadge estado={envio.estado_actual} />
                  </TableCell>
                  <TableCell className="text-right pr-6">
                    <TooltipProvider>
                      <div className="flex items-center justify-end gap-2">

                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button variant="outline" size="sm" className="text-[#198754] border-[#198754]/30 hover:bg-[#198754]/10 shadow-sm" asChild>
                              <Link href={`/envios/${envio.id_envio}`}>
                                <Eye className="h-4 w-4" />
                              </Link>
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>Ver ficha</TooltipContent>
                        </Tooltip>

                        {envio.estado_actual === 'PENDIENTE' && (
                          <>
                            <Tooltip>
                              <TooltipTrigger asChild>
                                {/* <Button variant="outline" size="sm" className="text-amber-600 border-amber-400/40 hover:bg-amber-50 shadow-sm" asChild>
                                  <Link href={`/envios/${envio.id_envio}/editar`}>
                                    <Pencil className="h-4 w-4" />
                                  </Link>
                                </Button> */}
                                <Button variant="outline" size="sm" className="text-amber-600 border-amber-400/40 hover:bg-amber-50 shadow-sm">
                                  <Pencil className="h-4 w-4" />
                                </Button>
                              </TooltipTrigger>
                              <TooltipContent>Editar envío</TooltipContent>
                            </Tooltip>

                            <Tooltip>
                              <TooltipTrigger asChild>
                                <Button
                                  variant="outline"
                                  size="sm"
                                  className="text-red-500 border-red-300/40 hover:bg-red-50 shadow-sm"
                                  onClick={() => setEnvioAConfirmar(envio)}
                                >
                                  <XCircle className="h-4 w-4" />
                                </Button>
                              </TooltipTrigger>
                              <TooltipContent>Cancelar envío</TooltipContent>
                            </Tooltip>
                          </>
                        )}

                      </div>
                    </TooltipProvider>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </div>

      {/* Vista Mobile - Transformación a Tarjetas (Mobile-First de tu CSS) */}
      <div className="md:hidden p-4 space-y-4">
        {envios.map((envio) => {
          const pesoTn = envio.kg_origen ? (envio.kg_origen / 1000).toFixed(1) : '0';
          return (
            <div key={envio.id_envio} className="bg-white border rounded-xl shadow-sm p-4 flex flex-col gap-3">
              <div className="flex justify-between items-center border-b pb-2">
                <div>
                  <span className="font-bold text-[#198754] block">#{envio.id_envio}</span>
                  <span className="text-xs text-muted-foreground">CTG: {envio.tracking_ctg}</span>
                </div>
                <EstadoBadge estado={envio.estado_actual} showIcon={false} />
              </div>

              <div className="flex justify-between items-center border-b pb-2">
                <span className="text-xs font-semibold text-muted-foreground uppercase">Cliente</span>
                <div className="text-right">
                  <span className="font-medium text-gray-900 block">{envio.origen?.empresa?.razon_social || '-'}</span>
                  <span className="text-xs text-muted-foreground">{envio.destino?.nombre_lugar || '-'}</span>
                </div>
              </div>

              <div className="flex justify-between items-center border-b pb-2">
                <span className="text-xs font-semibold text-muted-foreground uppercase">Carga</span>
                <div className="text-right">
                  <span className="font-medium text-gray-900 block">{normalizarEnum(envio.tipo_grano)}</span>
                  <span className="text-xs text-muted-foreground">{pesoTn} Tn</span>
                </div>
              </div>
              <div className="pt-2 flex flex-col gap-2">

                {/* 1. Botón Detalles - siempre visible */}
                <Button variant="outline" className="w-full text-[#198754] border-[#198754]/30 hover:bg-[#198754]/10" asChild>
                  <Link href={`/envios/${envio.id_envio}`}>
                    <Eye className="h-4 w-4 mr-2" /> Ver Ficha Completa
                  </Link>
                </Button>

                {/* 2 y 3. Solo visibles si el envío está PENDIENTE */}
                {envio.estado_actual === 'PENDIENTE' && (
                  <>
                    {/* <Button variant="outline" className="w-full text-amber-600 border-amber-400/40 hover:bg-amber-50" asChild>
                      <Link href={`/envios/${envio.id_envio}/editar`}>
                        <Pencil className="h-4 w-4 mr-2" /> Editar Envío
                      </Link>
                    </Button> */}
                    <Button variant="outline" className="w-full text-amber-600 border-amber-400/40 hover:bg-amber-50">
                      <Pencil className="h-4 w-4 mr-2" /> Editar Envío
                    </Button>

                    <Button
                      variant="outline"
                      className="w-full text-red-500 border-red-300/40 hover:bg-red-50"
                      onClick={() => setEnvioAConfirmar(envio)}
                    >
                      <XCircle className="h-4 w-4 mr-2" /> Cancelar Envío
                    </Button>
                  </>
                )}

              </div>
            </div>
          );
        })}
      </div>

      <AlertDialog open={!!envioAConfirmar} onOpenChange={(open) => !open && setEnvioAConfirmar(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle className="flex items-center gap-2">
              <AlertTriangle className="h-5 w-5 text-red-500" />
              Cancelar envío #{envioAConfirmar?.id_envio}
            </AlertDialogTitle>
            <AlertDialogDescription>
              Esta acción cancelará el envío de forma permanente. El estado cambiará a{' '}
              <span className="font-semibold text-red-600">Cancelado</span> y no podrá revertirse.
              ¿Estás seguro?
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={cancelando}>Volver</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleConfirmarCancelacion}
              disabled={cancelando}
              className="bg-red-500 hover:bg-red-600 text-white"
            >
              {cancelando ? 'Cancelando...' : 'Sí, cancelar envío'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

    </>
  );
}