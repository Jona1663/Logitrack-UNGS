'use client';

import { useState } from 'react';
import { AlertTriangle, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
  SheetFooter,
  SheetClose,
} from '@/components/ui/sheet';

interface IncidenciaDrawerProps {
  onSubmit: (descripcion: string) => Promise<void>;
  isLoading?: boolean;
}

export function IncidenciaDrawer({ onSubmit, isLoading }: IncidenciaDrawerProps) {
  const [descripcion, setDescripcion] = useState('');
  const [open, setOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async () => {
    if (!descripcion.trim()) return;

    setSubmitting(true);
    try {
      await onSubmit(descripcion);
      setDescripcion('');
      setOpen(false);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button variant="outline" size="lg" className="w-full" disabled={isLoading}>
          <AlertTriangle className="mr-2 h-5 w-5 text-amber-500" />
          Reportar Incidencia
        </Button>
      </SheetTrigger>
      <SheetContent side="bottom" className="min-h-[50vh] max-h-[85vh] overflow-y-auto">
        <SheetHeader>
          <SheetTitle className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5 text-amber-500" />
            Reportar Incidencia
          </SheetTitle>
          <SheetDescription>
            Describe el problema o incidencia que encontraste durante el viaje.
          </SheetDescription>
        </SheetHeader>

        <div className="py-1 space-y-1 px-1">
          <div className="space-y-2 px-1">
            <Label htmlFor="descripcion">Descripcion de la incidencia</Label>
            <Textarea
              id="descripcion"
              placeholder="Describa detalladamente la incidencia..."
              value={descripcion}
              onChange={(e) => setDescripcion(e.target.value)}
              className="min-h-[120px] resize-none"
            />
          </div>
        </div>

        <SheetFooter className="flex-col sm:flex-row gap-2 px-1 pb-2">
          <SheetClose asChild>
            <Button variant="outline" className="w-full sm:w-auto">
              Cancelar
            </Button>
          </SheetClose>
          <Button
            onClick={handleSubmit}
            disabled={!descripcion.trim() || submitting}
            className="w-full sm:w-auto bg-amber-500 hover:bg-amber-600"
          >
            {submitting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Enviando...
              </>
            ) : (
              'Enviar Reporte'
            )}
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}
