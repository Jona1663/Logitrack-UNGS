'use client';

import { toast } from 'sonner';
import { Package, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog';
import { ViajeCard } from '@/components/chofer/viaje-card';
import { ActionButton } from '@/components/chofer/action-button';
import { IncidenciaDrawer } from '@/components/chofer/incidencia-drawer';
import { useViajeChofer } from '@/hooks/use-viaje-chofer';
import { FLUJO_LOGISTICO } from '@/lib/constants';

export default function MiViajePage() {
  const {
    viaje,
    isLoading,
    isUpdating,
    error,
    recargar,
    avanzarEstado,
    reportarIncidencia,
  } = useViajeChofer();

  const handleAvanzarEstado = async () => {
    try {
      await avanzarEstado();
      const flujo = viaje ? FLUJO_LOGISTICO[viaje.estado_actual] : null;
      toast.success(flujo?.siguiente ? 'Estado actualizado' : 'Viaje completado');
    } catch (err) {
      toast.error('Error al actualizar el estado');
    }
  };

  const handleReportarIncidencia = async (descripcion: string) => {
    try {
      await reportarIncidencia(descripcion);
      toast.success('Incidencia reportada correctamente');
    } catch (err) {
      toast.error('Error al reportar la incidencia');
      throw err;
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <Spinner className="h-8 w-8" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="text-center py-12">
          <p className="text-destructive mb-4">{error}</p>
          <Button onClick={recargar} variant="outline">
            <RefreshCw className="mr-2 h-4 w-4" />
            Reintentar
          </Button>
        </div>
      </div>
    );
  }

  // Sin viaje asignado
  if (!viaje) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-md mx-auto text-center py-12">
          <div className="p-6 bg-muted rounded-full w-24 h-24 mx-auto mb-6 flex items-center justify-center">
            <Package className="h-12 w-12 text-muted-foreground" />
          </div>
          <h2 className="text-2xl font-semibold text-foreground mb-2">
            Sin viajes asignados
          </h2>
          <p className="text-muted-foreground mb-6">
            Actualmente no tienes ningun viaje pendiente. Consulta con tu supervisor si esperas una asignacion.
          </p>
          <Button onClick={recargar} variant="outline">
            <RefreshCw className="mr-2 h-4 w-4" />
            Actualizar
          </Button>
        </div>
      </div>
    );
  }

  const flujo = FLUJO_LOGISTICO[viaje.estado_actual];
  const isCompleted = !flujo.siguiente;

  return (
    <div className="container mx-auto px-4 py-6">
      <div className="max-w-lg mx-auto space-y-6">
        {/* Card del viaje */}
        <ViajeCard viaje={viaje} />

        {/* Acciones */}
        {!isCompleted && (
          <div className="space-y-3">
            {/* Boton de accion principal con confirmacion */}
            <AlertDialog>
              <AlertDialogTrigger asChild>
                <div>
                  <ActionButton
                    estadoActual={viaje.estado_actual}
                    onClick={() => { }}
                    isLoading={isUpdating}
                  />
                </div>
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Confirmar accion</AlertDialogTitle>
                  <AlertDialogDescription>
                    {flujo.confirmacionText}<br />Esta acción no se puede deshacer.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Cancelar</AlertDialogCancel>
                  <AlertDialogAction
                    onClick={handleAvanzarEstado}
                    className="bg-gradient-to-r from-[#1b4332] to-[#2d6a4f]"
                  >
                    Confirmar
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>

            {/* Boton de incidencia */}
            {/* <IncidenciaDrawer
              onSubmit={handleReportarIncidencia}
              isLoading={isUpdating}
            /> */}
          </div>
        )}

        {/* Mensaje de viaje completado */}
        {isCompleted && (
          <div className="text-center py-6 bg-green-50 rounded-lg border border-green-200">
            <p className="text-green-700 font-medium">
              Viaje completado exitosamente
            </p>
            <Button onClick={recargar} variant="link" className="mt-2">
              Buscar nuevo viaje
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
