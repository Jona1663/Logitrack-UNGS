import { Truck, AlertTriangle } from 'lucide-react';

interface EmptyStateProps {
  type: 'initial' | 'no-results';
}

export function EmptyState({ type }: EmptyStateProps) {
  if (type === 'initial') {
    return (
      <div className="bg-white rounded-2xl shadow-sm p-8 md:p-12 text-center border-0">
        <div className="py-6">
          <Truck className="h-16 w-16 mx-auto text-muted-foreground opacity-20 mb-4" strokeWidth={1.5} />
          <h6 className="text-lg font-bold text-gray-900">Panel de Monitoreo</h6>
          <p className="text-sm text-muted-foreground mt-1">
            Ingresa los parámetros arriba para localizar un envío.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-red-50 rounded-2xl shadow-sm p-8 md:p-12 text-center border-0">
      <div className="py-6">
        <AlertTriangle className="h-16 w-16 mx-auto text-red-500 opacity-75 mb-4" strokeWidth={1.5} />
        <h6 className="text-lg font-bold text-red-600">Sin coincidencias</h6>
        <p className="text-sm text-red-600/75 mt-1">
          No se encontraron remitos ni unidades con los criterios ingresados.
        </p>
      </div>
    </div>
  );
}