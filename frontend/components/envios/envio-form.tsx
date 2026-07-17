'use client';

import { useState, useEffect, useMemo } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';
import {
  Loader2, Building, MapPin, Truck, User, Wheat,
  Scale, FileText, AlertTriangle, CheckSquare, Pencil
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Checkbox } from '@/components/ui/checkbox';
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select';
import { Autocomplete } from '@/components/forms/autocomplete';
import { useCatalogos } from '@/hooks/use-catalogos';
import { envioSchema, type EnvioFormData } from '@/lib/validators';
import { api } from '@/lib/api';
import { normalizarEnum, getNombreChofer } from '@/lib/utils';
import type { Envio, TipoGrano } from '@/types';

interface EnvioFormProps {
  modo?: 'crear' | 'editar';
  envioInicial?: Envio;        // Solo en modo editar
  envioId?: string | number;   // Solo en modo editar
}

export function EnvioForm({ modo = 'crear', envioInicial, envioId }: EnvioFormProps) {
  const esEdicion = modo === 'editar';
  const router = useRouter();

  const {
    empresas, choferes, camiones, tiposGrano, establecimientos,
    loadingEstablecimientos, isLoading: loadingCatalogos,
    cargarEstablecimientos, buscarEmpresas, buscarGranos,
  } = useCatalogos();

  const [filteredEmpresas, setFilteredEmpresas] = useState(empresas);
  const [filteredGranos, setFilteredGranos] = useState<TipoGrano[]>(tiposGrano);

  // En edición, el CUIT viene del establecimiento origen del envío
  const cuitInicial = esEdicion ? envioInicial?.origen?.empresa?.cuit ?? '' : '';

  const {
    register, handleSubmit, control, watch, setValue,
    formState: { errors, isSubmitting },
  } = useForm<EnvioFormData>({
    resolver: zodResolver(envioSchema),
    defaultValues: {
      // trackingCtg: esEdicion ? envioInicial?.trackingCtg ?? '' : '',
      cpe: esEdicion ? envioInicial?.cpe ?? '' : '',
      clienteCuit: cuitInicial,
      idOrigen: esEdicion ? envioInicial?.origen?.idEstablecimiento ?? 0 : 0,
      idDestino: esEdicion ? envioInicial?.destino?.idEstablecimiento ?? 0 : 0,
      tipoGrano: esEdicion ? envioInicial?.tipoGrano ?? '' : '',
      kgOrigen: esEdicion ? (envioInicial?.kgOrigen ?? 0) / 1000 : 0,
      aceptaTerminos: false,
    },
  });

  const clienteCuit = watch('clienteCuit');
  const aceptaTerminos = watch('aceptaTerminos'); 

  // Cargar establecimientos al montar en modo edición
  useEffect(() => {
    if (esEdicion && cuitInicial) {
      cargarEstablecimientos(cuitInicial);
    }
  }, [esEdicion, cuitInicial, cargarEstablecimientos]);

  // Cargar establecimientos al cambiar cliente en modo creación
  useEffect(() => {
    if (!esEdicion && clienteCuit) {
      cargarEstablecimientos(clienteCuit);
      setValue('idOrigen', 0);
      setValue('idDestino', 0);
    }
  }, [clienteCuit, esEdicion, cargarEstablecimientos, setValue]);

  const empresaOptions = useMemo(
    () => filteredEmpresas.map((e) => ({
      value: e.cuit,
      label: e.razonSocial,
      description: `CUIT: ${e.cuit}`,
    })),
    [filteredEmpresas]
  );

  const granoOptions = useMemo(
    () => filteredGranos.map((g) => ({
      value: g,
      label: normalizarEnum(g),
    })),
    [filteredGranos]
  );

  const onSubmit = async (data: EnvioFormData) => {
    try {
      const payload = {
        // trackingCtg: data.trackingCtg,
        cpe: data.cpe,
        idOrigen: data.idOrigen,
        idDestino: data.idDestino,
        tipoGrano: data.tipoGrano as TipoGrano,
        kgOrigen: data.kgOrigen * 1000, // El form trabaja en Tn, backend en kg
      } as any;

      if (esEdicion && envioId) {
        await api.actualizarEnvio(envioId, payload);
        toast.success('Envío actualizado con éxito');
        router.push(`/envios/${envioId}`);
      } else {
        await api.crearEnvio(payload);
        toast.success('Viaje registrado con éxito');
        router.push('/menu');
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Error inesperado';
      toast.error(
        esEdicion ? 'No se pudo actualizar el envío' : 'No se pudo registrar el viaje',
        { description: message }
      );
    }
  };

  if (loadingCatalogos) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-[#1b4332]" />
      </div>
    );
  }

  return (
    <div className="bg-white rounded-2xl shadow-sm border-0 overflow-hidden mb-10">

      {/* Header — cambia título e ícono según el modo */}
      <div className="bg-[#198754]/10 px-6 py-5 md:px-10 md:py-6 border-b border-[#198754]/25">
        <h4 className="text-xl md:text-2xl font-bold text-gray-900 flex items-center gap-2 mb-1">
          {esEdicion
            ? <><Pencil className="h-6 w-6 text-[#198754]" /> Editar Envío #{envioId}</>
            : <><Truck className="h-6 w-6 text-[#198754]" /> Registrar Nuevo Envío</>
          }
        </h4>
        <p className="text-muted-foreground text-sm m-0">
          {esEdicion
            ? 'Modificá los datos del envío. Solo podés editar envíos en estado Pendiente.'
            : 'Complete la orden de transporte seleccionando el cliente y la ruta.'
          }
        </p>
      </div>

      {/* Cuerpo — idéntico al original, sin cambios */}
      <div className="p-6 md:p-10">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-10">

          {/* Sección: Cliente */}
          <div className="space-y-5">
            <h6 className="font-bold text-[#198754] mb-3 border-b border-[#198754]/20 pb-2 flex items-center gap-2">
              <Building className="h-5 w-5" /> Empresa Cliente
            </h6>
            <div className="space-y-2">
              <Label className="text-xs font-bold uppercase text-muted-foreground">
                Buscar Cliente <span className="text-destructive">*</span>
              </Label>
              <Controller
                name="clienteCuit"
                control={control}
                render={({ field }) => (
                  <Autocomplete
                    placeholder="Ingrese Razón Social o CUIT..."
                    options={empresaOptions}
                    value={field.value}
                    onChange={(value) => field.onChange(value)}
                    onSearch={(query) => setFilteredEmpresas(buscarEmpresas(query))}
                    error={errors.clienteCuit?.message}
                    className="bg-muted/30"
                  />
                )}
              />
            </div>
          </div>

          {/* Sección: Origen y Destino */}
          <div className="space-y-5">
            <div className="grid md:grid-cols-2 gap-8">
              <div className="space-y-2">
                <h6 className="font-bold text-[#198754] mb-3 border-b border-[#198754]/20 pb-2 flex items-center gap-2">
                  <MapPin className="h-5 w-5" /> Establecimiento Origen
                </h6>
                <Label className="text-xs font-bold uppercase text-muted-foreground">
                  Punto de Carga <span className="text-destructive">*</span>
                </Label>
                <Controller
                  name="idOrigen"
                  control={control}
                  render={({ field }) => (
                    <Select
                      value={field.value?.toString() || ''}
                      onValueChange={(v) => field.onChange(parseInt(v, 10))}
                      disabled={!clienteCuit || loadingEstablecimientos}
                    >
                      <SelectTrigger className={`w-full bg-muted/30 border-0 shadow-sm h-11 ${errors.idOrigen ? 'ring-2 ring-destructive' : ''}`}>
                        <SelectValue placeholder="Seleccione un cliente primero..." />
                      </SelectTrigger>
                      <SelectContent>
                        {establecimientos.map((est) => (
                          <SelectItem key={est.idEstablecimiento} value={est.idEstablecimiento.toString()}>
                            {est.nombreLugar} ({est.direccion})
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
                {errors.idOrigen && <p className="text-xs text-destructive">{errors.idOrigen.message}</p>}
              </div>

              <div className="space-y-2">
                <h6 className="font-bold text-[#198754] mb-3 border-b border-[#198754]/20 pb-2 flex items-center gap-2">
                  <MapPin className="h-5 w-5" /> Establecimiento Destino
                </h6>
                <Label className="text-xs font-bold uppercase text-muted-foreground">
                  Punto de Descarga <span className="text-destructive">*</span>
                </Label>
                <Controller
                  name="idDestino"
                  control={control}
                  render={({ field }) => (
                    <Select
                      value={field.value?.toString() || ''}
                      onValueChange={(v) => field.onChange(parseInt(v, 10))}
                      disabled={!clienteCuit || loadingEstablecimientos}
                    >
                      <SelectTrigger className={`w-full bg-muted/30 border-0 shadow-sm h-11 ${errors.idDestino ? 'ring-2 ring-destructive' : ''}`}>
                        <SelectValue placeholder="Seleccione un cliente primero..." />
                      </SelectTrigger>
                      <SelectContent>
                        {establecimientos.map((est) => (
                          <SelectItem key={est.idEstablecimiento} value={est.idEstablecimiento.toString()}>
                            {est.nombreLugar} ({est.direccion})
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
                {errors.idDestino && <p className="text-xs text-destructive">{errors.idDestino.message}</p>}
              </div>
            </div>
          </div>

          {/* Sección: Documentación Fiscal */}
          <div className="space-y-5">
            <h6 className="font-bold text-[#198754] mb-3 border-b border-[#198754]/20 pb-2 flex items-center gap-2">
              <FileText className="h-5 w-5" /> Documentación Fiscal
            </h6>
            <div className="grid md:grid-cols-2 gap-6">
              {/*<div className="space-y-2">
               <Label className="text-xs font-bold uppercase text-muted-foreground">
                  Código CTG <span className="text-destructive">*</span>
                </Label>
                 <Input
                  placeholder="Nro. de Trazabilidad"
                  {...register('trackingCtg')}
                  className={`bg-muted/30 border-0 shadow-sm h-11 ${errors.trackingCtg ? 'ring-2 ring-destructive' : ''}`}
                />
                {errors.trackingCtg && <p className="text-xs text-destructive">{errors.trackingCtg.message}</p>} 
              </div>*/}
              <div className="space-y-2">
                <Label className="text-xs font-bold uppercase text-muted-foreground">
                  CPE (Carta de Porte) <span className="text-destructive">*</span>
                </Label>
                <Input
                  placeholder="Nro. de Carta de Porte"
                  {...register('cpe')}
                  className={`bg-muted/30 border-0 shadow-sm h-11 ${errors.cpe ? 'ring-2 ring-destructive' : ''}`}
                />
                {errors.cpe && <p className="text-xs text-destructive">{errors.cpe.message}</p>}
              </div>
            </div>
          </div>

          {/* Sección: Carga */}
          <div className="space-y-5">
            <h6 className="font-bold text-[#198754] mb-3 border-b border-[#198754]/20 pb-2 flex items-center gap-2">
              <Wheat className="h-5 w-5" /> Detalle de Carga
            </h6>
            <div className="grid md:grid-cols-12 gap-6">
              <div className="col-span-12 md:col-span-4 space-y-2">
                <Label className="text-xs font-bold uppercase text-muted-foreground">
                  Peso Neto (Tn) <span className="text-destructive">*</span>
                </Label>
                <div className="relative">
                  <Input
                    type="number"
                    min="0.1"
                    step="0.1"
                    placeholder="Ej. 30.5"
                    className={`bg-muted/30 border-0 shadow-sm h-11 pr-12 ${errors.kgOrigen ? 'ring-2 ring-destructive' : ''}`}
                    onKeyDown={(e) => {
                      if (e.key === '-' || e.key === '+' || e.key === 'e') e.preventDefault();
                    }}
                    {...register('kgOrigen', { valueAsNumber: true })}
                  />
                  <span className="absolute right-4 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">Tn</span>
                </div>
                {errors.kgOrigen && <p className="text-xs text-destructive">{errors.kgOrigen.message}</p>}
              </div>
              <div className="col-span-12 md:col-span-8 space-y-2">
                <Label className="text-xs font-bold uppercase text-muted-foreground">
                  Tipo de Grano <span className="text-destructive">*</span>
                </Label>
                <Controller
                  name="tipoGrano"
                  control={control}
                  render={({ field }) => (
                    <Autocomplete
                      placeholder="Ej. Soja, Trigo..."
                      options={granoOptions}
                      value={field.value}
                      onChange={(value) => field.onChange(value)}
                      onSearch={(query) => setFilteredGranos(buscarGranos(query))}
                      error={errors.tipoGrano?.message}
                      className="bg-muted/30"
                    />
                  )}
                />
              </div>
            </div>
          </div>

          {/* Checkbox Legal (Réplica exacta de tu HTML) */}
          <div className="bg-[#198754]/10 p-4 rounded-lg border border-[#198754]/25">
            <Controller
              name="aceptaTerminos"
              control={control}
              render={({ field }) => (
                <div className="flex items-center space-x-3">
                  <Checkbox
                    id="aceptaTerminos"
                    checked={field.value}
                    onCheckedChange={field.onChange}
                    className="data-[state=checked]:bg-[#198754] data-[state=checked]:border-[#198754]"
                  />
                  <label
                    htmlFor="aceptaTerminos"
                    className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 cursor-pointer"
                  >
                    Acepta el tratamiento de los datos según la Ley 25.326.
                  </label>
                </div>
              )}
            />
            {errors.aceptaTerminos && <p className="text-xs text-destructive mt-2">{errors.aceptaTerminos.message}</p>}
          </div>

          {/* Solo el botón de submit cambia el texto */}
          <div className="flex flex-col md:flex-row justify-end gap-4 pt-6 border-t">
            <Button
              type="button"
              variant="outline"
              onClick={() => router.back()}
              className="px-8 h-11 text-muted-foreground font-semibold border-0 shadow-sm"
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              disabled={isSubmitting || !aceptaTerminos}
              className="px-10 h-11 font-bold text-white bg-gradient-to-r from-[#1b4332] to-[#2d6a4f] hover:from-[#2d6a4f] hover:to-[#40916c] border-none shadow-sm transition-all"
            >
              {isSubmitting ? (
                <><Loader2 className="mr-2 h-5 w-5 animate-spin" />
                  {esEdicion ? 'Guardando...' : 'Registrando...'}
                </>
              ) : (
                esEdicion ? 'Guardar Cambios' : 'Registrar Viaje'
              )}
            </Button>
          </div>

        </form>
      </div>
    </div>
  );
}