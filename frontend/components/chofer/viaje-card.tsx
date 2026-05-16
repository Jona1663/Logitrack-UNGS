'use client';

import { MapPin, ArrowDown, Wheat, Scale, Truck, FileText, User } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { EstadoBadge } from '@/components/envios/estado-badge';
import { PrioridadBadge } from '../envios/prioridad-badge';
import type { EnvioChofer } from '@/types';
import { normalizarEnum, formatearPeso } from '@/lib/utils';

interface ViajeCardProps {
  viaje: EnvioChofer;
}

export function ViajeCard({ viaje }: ViajeCardProps) {
  return (
    <Card className="shadow-lg">
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-lg">Mi Viaje Actual</CardTitle>
          <EstadoBadge estado={viaje.estadoActual} />
        </div>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Ruta: Origen -> Destino */}
        <div className="space-y-3">
          {/* Origen */}
          <div className="p-4 bg-green-50 rounded-lg border border-green-200">
            <div className="flex items-center gap-2 text-green-700 mb-2">
              <MapPin className="h-5 w-5" />
              <span className="text-sm font-semibold uppercase">Origen</span>
            </div>
            <p className="font-semibold text-lg">{viaje.origen.nombreLugar}</p>
            <p className="text-sm text-muted-foreground">{viaje.origen.direccion}</p>
          </div>

          {/* Flecha */}
          <div className="flex justify-center">
            <div className="p-2 bg-muted rounded-full">
              <ArrowDown className="h-5 w-5 text-muted-foreground" />
            </div>
          </div>

          {/* Destino */}
          <div className="p-4 bg-blue-50 rounded-lg border border-blue-200">
            <div className="flex items-center gap-2 text-blue-700 mb-2">
              <MapPin className="h-5 w-5" />
              <span className="text-sm font-semibold uppercase">Destino</span>
            </div>
            <p className="font-semibold text-lg">{viaje.destino.nombreLugar}</p>
            <p className="text-sm text-muted-foreground">{viaje.destino.direccion}</p>
          </div>
        </div>

        {/* Detalles de la carga */}
        <div className="grid grid-cols-2 gap-4 pt-4 border-t">
          <InfoItem
            icon={<Wheat className="h-4 w-4" />}
            label="Grano"
            value={normalizarEnum(viaje.tipoGrano)}
          />
          <InfoItem
            icon={<Scale className="h-4 w-4" />}
            label="Peso"
            value={formatearPeso(viaje.kgOrigen)}
          />
          <InfoItem
            icon={<Truck className="h-4 w-4" />}
            label="Camion"
            value={viaje.patenteCamion}
            mono
          />
          {/* <InfoItem
            icon={<FileText className="h-4 w-4" />}
            label="CTG"
            value={viaje.trackingCtg}
            mono
          /> */}
          <InfoItem
            icon={<FileText className="h-4 w-4" />}
            label="CPE"
            value={viaje.cpe}
            mono
          />
        </div>

        {/* Chofer */}
        {/* <div className="flex items-center justify-between pt-4 border-t">
          <span className="text-sm text-muted-foreground">Chofer asignado</span>
          <div className="flex items-center gap-1.5 text-sm font-medium">
            <User className="h-4 w-4 text-muted-foreground" />
            {viaje.nombreChofer}
          </div>
        </div> */}

        {/* Prioridad */}
        <div className="flex items-center justify-between pt-4 border-t">
          <span className="text-sm text-muted-foreground">Prioridad del envio</span>
          <PrioridadBadge prioridad={viaje.prioridadIa} />
        </div>

      </CardContent>
    </Card>
  );
}

function InfoItem({
  icon,
  label,
  value,
  mono,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  mono?: boolean;
}) {
  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-center gap-1.5 text-muted-foreground">
        {icon}
        <span className="text-xs">{label}</span>
      </div>
      <span className={`font-medium ${mono ? 'font-mono text-sm' : ''}`}>{value}</span>
    </div>
  );
}