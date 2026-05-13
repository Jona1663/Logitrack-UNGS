'use client';

import { Search, Eraser, Calendar } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button'; // <-- ¡Esta es la línea que falta!
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select';
import { ESTADOS_FILTRO } from '@/lib/constants';
import type { EstadoEnvio } from '@/types';

interface SearchFiltersProps {
  query: string; estado: EstadoEnvio | ''; fecha: string;
  onQueryChange: (value: string) => void; onEstadoChange: (value: EstadoEnvio | '') => void;
  onFechaChange: (value: string) => void;
  onSearch: () => void; onClear: () => void; isLoading?: boolean;
}
export function SearchFilters({
  query, estado, fecha, onQueryChange, onEstadoChange,
  onFechaChange, onSearch, onClear, isLoading,
}: SearchFiltersProps) {
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSearch();
  };

  const hasFilters = Boolean(query || estado || fecha);

  return (
    <div className="bg-white rounded-2xl shadow-sm border-0 p-6 md:p-8">
      <div className="flex items-center gap-3 mb-2">
        <div className="bg-blue-600/10 text-blue-600 p-2 rounded-lg">
          <Search className="h-6 w-6" />
        </div>
        <h4 className="text-xl font-bold text-gray-900 m-0">Rastreo de Envío</h4>
      </div>
      <p className="text-muted-foreground text-sm mb-6 md:ml-14">
        Busca por ID Interno, Código CTG, Empresa de Origen/Destino o Tipo de Grano.
      </p>

      <form onSubmit={handleSubmit} className="md:ml-14">
        <div className="grid grid-cols-1 md:grid-cols-12 gap-4 items-end">
          {/* Fila 1: Búsqueda */}
          <div className="md:col-span-12 space-y-1">
            <Label className="text-xs font-bold uppercase text-muted-foreground">Término de búsqueda</Label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-[#198754]" />
              <Input
                type="text"
                placeholder="Ej: LT-001, 89456123, Los Grobo, Rosario, Soja..."
                value={query}
                onChange={(e) => onQueryChange(e.target.value)}
                className="pl-10 h-11 border-[#198754]/25 focus-visible:ring-[#198754]"
              />
            </div>
          </div>

          {/* Fila 2: Selectores y Botones */}
          <div className="md:col-span-4 space-y-1">
            <Label className="text-xs font-bold uppercase text-muted-foreground">Estado Operativo</Label>
            <Select value={estado || 'all'} onValueChange={(v) => onEstadoChange(v === 'all' ? '' : v as EstadoEnvio)}>
              <SelectTrigger className="w-full h-11 bg-muted/30 border-0 shadow-sm focus:ring-[#198754]">
                <SelectValue placeholder="Cualquier Estado" />
              </SelectTrigger>
              <SelectContent>
                {ESTADOS_FILTRO.map((item) => (
                  <SelectItem key={item.value || 'all'} value={item.value || 'all'}>{item.label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="md:col-span-4 space-y-1">
            <Label className="text-xs font-bold uppercase text-muted-foreground">Fecha de Operación</Label>
            <div className="relative">
              <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-[#198754]" />
              <Input
                type="date"
                value={fecha} // <-- Usamos la variable unificada
                onChange={(e) => onFechaChange(e.target.value)} // <-- Usamos el handler correcto
                className="pl-10 h-11 bg-muted/30 border-0 shadow-sm focus-visible:ring-[#198754] text-muted-foreground"
              />
            </div>
          </div>

          <div className="md:col-span-4 flex gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={onClear}
              disabled={!hasFilters}
              className="h-11 px-3 border-0 shadow-sm bg-gray-100 hover:bg-red-50 hover:text-red-600 text-gray-500 transition-colors"
              title="Limpiar filtros"
            >
              <Eraser className="h-5 w-5" />
            </Button>
            <Button type="submit" disabled={isLoading} className="h-11 flex-1 font-bold bg-gradient-to-r from-[#1b4332] to-[#2d6a4f] hover:from-[#2d6a4f] hover:to-[#40916c] border-0 shadow-sm">
              Buscar
            </Button>
          </div>
        </div>
      </form>
    </div>
  );
}