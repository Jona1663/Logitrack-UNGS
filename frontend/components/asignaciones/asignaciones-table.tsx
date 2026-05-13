'use client';

import { useState, useEffect, useCallback } from 'react';
import { Truck, User, MapPin, AlertCircle, Loader2, CheckCircle2 } from 'lucide-react';
import { toast } from 'sonner';
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table';
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription,
} from '@/components/ui/dialog';
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { EstadoBadge } from '@/components/envios/estado-badge';
import { api } from '@/lib/api';
import { getNombreChofer, normalizarEnum } from '@/lib/utils';
import type { Envio, Chofer, Camion } from '@/types';

export function AsignacionesTable() {
  // ── Datos ──────────────────────────────────────────────────────────────────
  const [envios, setEnvios] = useState<Envio[]>([]);
  const [choferes, setChoferes] = useState<Chofer[]>([]);
  const [camiones, setCamiones] = useState<Camion[]>([]);
  const [loadingDatos, setLoadingDatos] = useState(true);

  // ── Modal ──────────────────────────────────────────────────────────────────
  const [envioSeleccionado, setEnvioSeleccionado] = useState<Envio | null>(null);
  const [choferSeleccionado, setChoferSeleccionado] = useState('');
  const [camionSeleccionado, setCamionSeleccionado] = useState('');
  const [guardando, setGuardando] = useState(false);

  // ── Carga inicial ──────────────────────────────────────────────────────────
  const cargarDatos = useCallback(async () => {
    setLoadingDatos(true);
    try {
      const [enviosData, choferesData, camionesData] = await Promise.all([
        api.getEnviosSinAsignar(),
        api.getChoferes(),
        api.getCamiones(),
      ]);
      setEnvios(enviosData);
      setChoferes(choferesData);
      setCamiones(camionesData);
    } catch (error) {
      toast.error('Error al cargar los datos');
    } finally {
      setLoadingDatos(false);
    }
  }, []);

  useEffect(() => {
    cargarDatos();
  }, [cargarDatos]);

  // ── Abrir / cerrar modal ───────────────────────────────────────────────────
  const abrirModal = (envio: Envio) => {
    setEnvioSeleccionado(envio);
    setChoferSeleccionado('');
    setCamionSeleccionado('');
  };

  const cerrarModal = () => {
    if (guardando) return; // No cerrar mientras guarda
    setEnvioSeleccionado(null);
    setChoferSeleccionado('');
    setCamionSeleccionado('');
  };

  // ── Confirmar asignación ───────────────────────────────────────────────────
  const confirmarAsignacion = async () => {
    if (!envioSeleccionado || !choferSeleccionado || !camionSeleccionado) return;

    setGuardando(true);
    try {
      await api.asignarTransporte(String(envioSeleccionado.id_envio), {
        id_chofer: parseInt(choferSeleccionado, 10),
        patente_camion: camionSeleccionado,
      });

      toast.success(`Transporte asignado al envío ${envioSeleccionado.id_envio}`);

      // Quitar el envío de la lista (ya quedó asignado)
      setEnvios((prev) => prev.filter((e) => e.id_envio !== envioSeleccionado.id_envio));
      cerrarModal();
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Error al asignar el transporte';
      toast.error('No se pudo asignar', { description: message });
    } finally {
      setGuardando(false);
    }
  };

  const puedeConfirmar = !!choferSeleccionado && !!camionSeleccionado && !guardando;

  // ── Loading ────────────────────────────────────────────────────────────────
  if (loadingDatos) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="h-8 w-8 animate-spin text-[#1b4332]" />
      </div>
    );
  }

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <>
      {/* Encabezado de sección */}
      <div className="bg-white rounded-2xl shadow-sm overflow-hidden mb-6">
        <div className="bg-[#198754]/10 px-6 py-5 md:px-10 md:py-6 border-b border-[#198754]/25">
          <h4 className="text-xl md:text-2xl font-bold text-gray-900 flex items-center gap-2 mb-1">
            <Truck className="h-6 w-6 text-[#198754]" /> Asignación de Transporte
          </h4>
          <p className="text-muted-foreground text-sm">
            Envíos pendientes de asignación de chofer y camión.
            {envios.length > 0 && (
              <span className="ml-2 font-semibold text-[#198754]">
                {envios.length} {envios.length === 1 ? 'envío pendiente' : 'envíos pendientes'}
              </span>
            )}
          </p>
        </div>

        {/* Estado vacío */}
        {envios.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 text-center px-4">
            <CheckCircle2 className="h-12 w-12 text-[#198754]/40 mb-4" />
            <p className="font-semibold text-gray-700">Todo asignado</p>
            <p className="text-sm text-muted-foreground mt-1">
              No hay envíos pendientes de asignación de transporte.
            </p>
          </div>
        ) : (
          <>
            {/* Tabla Desktop */}
            <div className="hidden md:block overflow-x-auto">
              <Table>
                <TableHeader className="bg-muted/30">
                  <TableRow className="hover:bg-transparent">
                    {/* <TableHead className="py-4 pl-6 font-semibold">ID / CTG</TableHead> */}
                    <TableHead className="py-4 pl-6 font-semibold">ID</TableHead>
                    <TableHead className="py-4 font-semibold">Cliente</TableHead>
                    <TableHead className="py-4 font-semibold">Ruta</TableHead>
                    <TableHead className="py-4 font-semibold">Carga</TableHead>
                    <TableHead className="py-4 font-semibold">Estado</TableHead>
                    <TableHead className="py-4 pr-6 text-right font-semibold">Acción</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {envios.map((envio) => {
                    const pesoTn = envio.kg_origen ? (envio.kg_origen / 1000).toFixed(1) : '0';
                    return (
                      <TableRow key={envio.id_envio} className="hover:bg-muted/30 transition-colors">
                        <TableCell className="pl-6">
                          <span className="font-bold text-[#198754] block">{envio.id_envio}</span>
                          {/* <span className="text-xs text-muted-foreground">CTG: {envio.tracking_ctg}</span> */}
                        </TableCell>
                        <TableCell>
                          <span className="font-medium text-gray-900 block">
                            {envio.origen?.empresa?.razon_social || 'Sin cliente'}
                          </span>
                          <span className="text-xs text-muted-foreground">
                            CPE: {envio.cpe}
                          </span>
                        </TableCell>
                        <TableCell>
                          <span className="text-sm text-gray-700 flex items-center gap-1">
                            <MapPin className="h-3 w-3 text-muted-foreground shrink-0" />
                            {envio.origen?.nombre_lugar || '-'}
                          </span>
                          <span className="text-xs text-muted-foreground ml-4">
                            → {envio.destino?.nombre_lugar || '-'}
                          </span>
                        </TableCell>
                        <TableCell>
                          <span className="font-medium text-gray-900 block">
                            {normalizarEnum(envio.tipo_grano)}
                          </span>
                          <span className="text-xs text-muted-foreground">{pesoTn} Tn</span>
                        </TableCell>
                        <TableCell>
                          <EstadoBadge estado={envio.estado_actual} />
                        </TableCell>
                        <TableCell className="text-right pr-6">
                          <Button
                            size="sm"
                            onClick={() => abrirModal(envio)}
                            className="bg-[#198754] hover:bg-[#157347] text-white shadow-sm"
                          >
                            <Truck className="h-4 w-4 mr-1.5" /> Asignar
                          </Button>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>

            {/* Tarjetas Mobile */}
            <div className="md:hidden p-4 space-y-4">
              {envios.map((envio) => {
                const pesoTn = envio.kg_origen ? (envio.kg_origen / 1000).toFixed(1) : '0';
                return (
                  <div
                    key={envio.id_envio}
                    className="bg-white border rounded-xl shadow-sm p-4 flex flex-col gap-3"
                  >
                    <div className="flex justify-between items-center border-b pb-2">
                      <div>
                        <span className="font-bold text-[#198754] block">{envio.id_envio}</span>
                        {/* <span className="text-xs text-muted-foreground">CTG: {envio.tracking_ctg}</span> */}
                      </div>
                      <EstadoBadge estado={envio.estado_actual} showIcon={false} />
                    </div>

                    <div className="flex justify-between items-center border-b pb-2">
                      <span className="text-xs font-semibold text-muted-foreground uppercase">Cliente</span>
                      <span className="font-medium text-gray-900 text-right">
                        {envio.origen?.empresa?.razon_social || '-'}
                      </span>
                    </div>

                    <div className="flex justify-between items-center border-b pb-2">
                      <span className="text-xs font-semibold text-muted-foreground uppercase">Ruta</span>
                      <div className="text-right">
                        <span className="text-sm text-gray-700 block">{envio.origen?.nombre_lugar || '-'}</span>
                        <span className="text-xs text-muted-foreground">→ {envio.destino?.nombre_lugar || '-'}</span>
                      </div>
                    </div>

                    <div className="flex justify-between items-center border-b pb-2">
                      <span className="text-xs font-semibold text-muted-foreground uppercase">Carga</span>
                      <div className="text-right">
                        <span className="font-medium text-gray-900 block">{normalizarEnum(envio.tipo_grano)}</span>
                        <span className="text-xs text-muted-foreground">{pesoTn} Tn</span>
                      </div>
                    </div>

                    <Button
                      onClick={() => abrirModal(envio)}
                      className="w-full bg-[#198754] hover:bg-[#157347] text-white"
                    >
                      <Truck className="h-4 w-4 mr-2" /> Asignar Transporte
                    </Button>
                  </div>
                );
              })}
            </div>
          </>
        )}
      </div>

      {/* ── Modal de Asignación ──────────────────────────────────────────────── */}
      <Dialog open={!!envioSeleccionado} onOpenChange={(open) => !open && cerrarModal()}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-gray-900">
              <Truck className="h-5 w-5 text-[#198754]" />
              Asignar Transporte
            </DialogTitle>
            {envioSeleccionado && (
              <DialogDescription>
                Envío <span className="font-semibold text-[#198754]">{envioSeleccionado.id_envio}</span>
                {' '}— {envioSeleccionado.origen?.empresa?.razon_social || ''}
              </DialogDescription>
            )}
          </DialogHeader>

          <div className="space-y-5 py-2">
            {/* Aviso: ambos obligatorios */}
            <div className="flex items-start gap-2 bg-amber-50 border border-amber-200 rounded-lg p-3">
              <AlertCircle className="h-4 w-4 text-amber-600 mt-0.5 shrink-0" />
              <p className="text-xs text-amber-700">
                Debe seleccionar <strong>ambos</strong> (chofer y camión) para confirmar la asignación.
              </p>
            </div>

            {/* Select Chofer */}
            <div className="space-y-2">
              <Label className="text-xs font-bold uppercase text-muted-foreground flex items-center gap-1">
                <User className="h-3.5 w-3.5" /> Chofer <span className="text-destructive">*</span>
              </Label>
              <Select value={choferSeleccionado} onValueChange={setChoferSeleccionado}>
                <SelectTrigger className="bg-muted/30 border-0 shadow-sm h-11">
                  <SelectValue placeholder="Seleccione un chofer..." />
                </SelectTrigger>
                <SelectContent>
                  {choferes.map((chofer) => (
                    <SelectItem key={chofer.id_chofer} value={chofer.id_chofer.toString()}>
                      {getNombreChofer(chofer)} — Lic: {chofer.nro_licencia}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Select Camión */}
            <div className="space-y-2">
              <Label className="text-xs font-bold uppercase text-muted-foreground flex items-center gap-1">
                <Truck className="h-3.5 w-3.5" /> Camión <span className="text-destructive">*</span>
              </Label>
              <Select value={camionSeleccionado} onValueChange={setCamionSeleccionado}>
                <SelectTrigger className="bg-muted/30 border-0 shadow-sm h-11">
                  <SelectValue placeholder="Seleccione un camión..." />
                </SelectTrigger>
                <SelectContent>
                  {camiones.map((camion) => (
                    <SelectItem key={camion.patente} value={camion.patente}>
                      Patente: {camion.patente} — Tara: {camion.tara_vacio_kg} kg
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button
              type="button"
              variant="outline"
              onClick={cerrarModal}
              disabled={guardando}
              className="border-0 shadow-sm"
            >
              Cancelar
            </Button>
            <Button
              type="button"
              onClick={confirmarAsignacion}
              disabled={!puedeConfirmar}
              className="bg-gradient-to-r from-[#1b4332] to-[#2d6a4f] hover:from-[#2d6a4f] hover:to-[#40916c] text-white border-none shadow-sm"
            >
              {guardando ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Guardando...
                </>
              ) : (
                <>
                  <CheckCircle2 className="mr-2 h-4 w-4" /> Confirmar Asignación
                </>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}