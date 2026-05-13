'use client';

import { use, useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';
import {
  ArrowLeftCircle,
  FileText,
  MapPin,
  ClipboardList
} from 'lucide-react';
import { EstadoTimeline } from '@/components/envios/estado-timeline';
import { HistorialTable } from '@/components/envios/historial-table';
import { Spinner } from '@/components/ui/spinner';
import { Button } from '@/components/ui/button';
import { TruckStepper } from '@/components/envios/truck-stepper';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useEnvioDetail } from '@/hooks/use-envio-detail';
import { useAuth } from '@/contexts/auth-context';
import { ESTADO_CONFIG, PRIORIDAD_CONFIG } from '@/lib/constants';
import type { EstadoEnvio, Prioridad } from '@/types';
import { normalizarEnum } from '@/lib/utils';

export default function DetalleEnvioPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const router = useRouter();
  const { id } = use(params);

  // Pasamos el "id" directamente como string sin usar parseInt
  const { envio, historial, isLoading, isUpdating, error, actualizarEnvio } = useEnvioDetail(id);
  const { permisos } = useAuth();

  const [nuevoEstado, setNuevoEstado] = useState<EstadoEnvio | ''>('');
  const [nuevaPrioridad, setNuevaPrioridad] = useState<Prioridad | ''>('');

  // Sincronizar el estado local cuando se carga el envío
  useEffect(() => {
    if (envio) {
      setNuevoEstado(envio.estado_actual);
      setNuevaPrioridad(envio.prioridad_ia);
    }
  }, [envio]);

  // Verificar si hay cambios reales para habilitar el botón de guardado
  const hayCambios = envio && (nuevoEstado !== envio.estado_actual || nuevaPrioridad !== envio.prioridad_ia);

  const handleGuardarCambios = async () => {
    if (!nuevoEstado || !nuevaPrioridad) return;

    try {
      // Usamos los nombres que espera el DTO de Java
      await actualizarEnvio({
        estado: nuevoEstado,
        prioridad_ia: nuevaPrioridad
      });
      // IMPORTANTE: Recargamos la página igual que en detalleEnvio.js para actualizar el historial
      toast.success('Operación actualizada con éxito');
      // setTimeout(() => window.location.reload(), 1000);
    } catch (err) {
      toast.error('Error al actualizar los datos');
    }
  };

  if (isLoading) {
    return (
      <div className="w-full max-w-5xl mx-auto p-4 md:p-6 lg:py-8 flex justify-center items-center min-h-[60vh]">
        <Spinner className="h-10 w-10 text-blue-600" />
      </div>
    );
  }

  if (error || !envio) {
    return (
      <div className="w-full max-w-5xl mx-auto p-4 md:p-6 lg:py-8 text-center pt-20">
        <p className="text-destructive font-bold text-lg mb-4">{error || 'Envío no encontrado'}</p>
        <Button variant="outline" onClick={() => router.push('/busqueda')}>Volver a Rastreo</Button>
      </div>
    );
  }

  const pesoTn = envio.kg_origen ? (envio.kg_origen / 1000).toFixed(1) : '0';

  return (
    <div className="w-full max-w-5xl mx-auto p-4 md:p-6 lg:py-8">
      {/* Botón Volver */}
      <Link
        href="/busqueda"
        className="text-[#198754] font-semibold mb-3 md:mb-4 inline-flex items-center gap-2 hover:opacity-80 transition-opacity"
      >
        <ArrowLeftCircle className="h-5 w-5" /> Volver a Rastreo
      </Link>

      <div className="bg-white border-0 shadow-sm rounded-2xl overflow-hidden mt-2 md:mt-3 mb-10">

        {/* Header Azul - Ficha Operativa */}
        <div className="bg-blue-600/10 px-6 py-5 md:px-8 md:py-6 border-b border-blue-600/20">
          <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
            <div>
              <h4 className="font-bold mb-1 text-xl md:text-2xl flex items-center gap-2 text-gray-900">
                <FileText className="text-blue-600 h-6 w-6" /> Ficha Operativa
              </h4>
              <p className="text-muted-foreground text-sm m-0">Detalles del transporte de carga.</p>
            </div>
            <div className="bg-white px-6 py-3 rounded-xl border shadow-sm text-center w-full md:w-auto">
              <span className="block text-[10px] text-muted-foreground font-bold uppercase tracking-wider mb-1">
                ID Rastreo
              </span>
              <span className="font-extrabold text-2xl text-blue-600 leading-none">
                {envio.id_envio}
              </span>
            </div>
          </div>
        </div>

        <div className="p-6 md:p-10">

          {/* Empresa Cliente */}
          <h6 className="font-bold text-[#198754] mb-4 border-b border-[#198754]/20 pb-2">
            Empresa Cliente
          </h6>
          <div className="grid md:grid-cols-12 gap-6 mb-8">
            <div className="col-span-12 md:col-span-8">
              <label className="text-[11px] font-bold text-muted-foreground uppercase tracking-wider block mb-1">
                Razón Social
              </label>
              <input
                type="text"
                disabled
                className="w-full bg-transparent border-b border-dashed border-gray-300 pb-2 text-gray-700 font-medium outline-none"
                value={envio.origen?.empresa?.razon_social || "No especificado"}
              />
            </div>
          </div>

          {/* Ruta del Viaje */}
          <h6 className="font-bold text-[#198754] mb-4 border-b border-[#198754]/20 pb-2">
            Ruta del Viaje
          </h6>
          <div className="grid md:grid-cols-2 gap-6 mb-10">
            <div>
              <label className="text-[11px] font-bold text-muted-foreground uppercase tracking-wider block mb-1">
                Establecimiento Origen
              </label>
              <input
                disabled
                className="w-full bg-transparent border-b border-dashed border-gray-300 pb-2 text-gray-700 font-medium outline-none"
                value={envio.origen?.nombre_lugar || "No especificado"}
              />
            </div>
            <div>
              <label className="text-[11px] font-bold text-muted-foreground uppercase tracking-wider block mb-1">
                Establecimiento Destino
              </label>
              <input
                disabled
                className="w-full bg-transparent border-b border-dashed border-gray-300 pb-2 text-gray-700 font-medium outline-none"
                value={envio.destino?.nombre_lugar || "No especificado"}
              />
            </div>
          </div>

          {/* Detalle de Carga */}
          <h6 className="font-bold text-[#198754] mb-4 border-b border-[#198754]/20 pb-2">
            Detalle de Carga
          </h6>
          <div className="grid md:grid-cols-12 gap-6 mb-10">
            <div className="col-span-12 md:col-span-4">
              <label className="text-[11px] font-bold text-muted-foreground uppercase tracking-wider block mb-1">
                Peso Neto (Tn)
              </label>
              <input
                disabled
                className="w-full bg-transparent border-b border-dashed border-gray-300 pb-2 text-gray-700 font-medium outline-none"
                value={pesoTn}
              />
            </div>
            <div className="col-span-12 md:col-span-8">
              <label className="text-[11px] font-bold text-muted-foreground uppercase tracking-wider block mb-1">
                Tipo de Grano
              </label>
              <input
                disabled
                className="w-full bg-transparent border-b border-dashed border-gray-300 pb-2 text-gray-700 font-medium outline-none"
                value={normalizarEnum(envio.tipo_grano) || "General"}
              />
            </div>
          </div>

          {/* Estado del Recorrido (Timeline) */}
          <h6 className="font-bold text-[#198754] mb-6 border-b border-[#198754]/20 pb-2">
            Estado del Recorrido
          </h6>
          <div className="mb-12">
            <EstadoTimeline estadoActual={envio.estado_actual} />

            <div className="mt-6">
              <TruckStepper estadoActual={envio.estado_actual} />
            </div>
          </div>


          {/* Gestión Operativa (Roles y Permisos) */}
          {(permisos?.editarEstado || permisos?.editarPrioridad) && (
            <div className="bg-amber-500/10 border border-amber-500/30 rounded-2xl p-6 md:p-8 mb-10">
              <h6 className="font-bold mb-5 text-lg flex items-center gap-2 text-gray-900">
                <ClipboardList className="text-amber-500 h-6 w-6" /> Gestión Operativa
              </h6>

              <div className="grid md:grid-cols-2 gap-6">
                <div>
                  <label className="text-[11px] font-bold text-gray-700 uppercase tracking-wider block mb-2">
                    Estado
                  </label>
                  <Select
                    value={nuevoEstado}
                    onValueChange={(v) => setNuevoEstado(v as EstadoEnvio)}
                    disabled={!permisos?.editarEstado || isUpdating}
                  >
                    <SelectTrigger className="w-full h-11 bg-white border-0 shadow-sm focus:ring-amber-500">
                      <SelectValue placeholder="Seleccionar estado" />
                    </SelectTrigger>
                    <SelectContent>
                      {Object.entries(ESTADO_CONFIG).map(([key, config]) => (
                        <SelectItem key={key} value={key}>{config.label}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div>
                  <label className="text-[11px] font-bold text-gray-700 uppercase tracking-wider block mb-2">
                    Prioridad
                  </label>
                  <Select
                    value={nuevaPrioridad}
                    onValueChange={(v) => setNuevaPrioridad(v as Prioridad)}
                    disabled={!permisos?.editarPrioridad || isUpdating || envio.estado_actual !== 'PENDIENTE'}                  >
                    <SelectTrigger className="w-full h-11 bg-white border-0 shadow-sm focus:ring-amber-500">
                      <SelectValue placeholder="Seleccionar prioridad" />
                    </SelectTrigger>
                    <SelectContent>
                      {Object.entries(PRIORIDAD_CONFIG).map(([key, config]) => (
                        <SelectItem key={key} value={key}>{config.label}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="mt-8 flex justify-end">
                <Button
                  onClick={handleGuardarCambios}
                  disabled={!hayCambios || isUpdating}
                  className="w-full md:w-auto h-11 px-8 bg-amber-500 hover:bg-amber-600 text-white font-bold shadow-sm transition-all"
                >
                  {isUpdating ? (
                    <><Spinner className="mr-2 h-4 w-4 border-white" /> Actualizando...</>
                  ) : (
                    "Guardar Cambios"
                  )}
                </Button>
              </div>
            </div>
          )}

          {/* Auditoría de Ruta */}
          <h6 className="font-bold text-gray-900 mb-4">Auditoría de Ruta</h6>
          <div className="border border-gray-100 rounded-2xl overflow-hidden shadow-sm">
            <HistorialTable historial={historial} />
          </div>

        </div>
      </div>
    </div>
  );
}