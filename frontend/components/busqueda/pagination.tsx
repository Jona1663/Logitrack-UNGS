'use client';

import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface PaginationProps {
  currentPage: number; totalPages: number; totalElements: number;
  onPreviousPage: () => void; onNextPage: () => void;
  hasPreviousPage: boolean; hasNextPage: boolean;
}

export function Pagination({
  currentPage, totalPages, totalElements, onPreviousPage, onNextPage, hasPreviousPage, hasNextPage,
}: PaginationProps) {
  if (totalPages <= 1) return null;

  return (
    <div className="bg-muted/20 border-t p-4 flex flex-col sm:flex-row justify-between items-center gap-4">
      <span className="text-sm text-muted-foreground font-medium">
        Mostrando página {currentPage + 1} de {totalPages} <span className="opacity-70">({totalElements} resultados)</span>
      </span>

      <div className="flex bg-white rounded-lg shadow-sm border overflow-hidden">
        <Button
          variant="ghost" size="sm" onClick={onPreviousPage} disabled={!hasPreviousPage}
          className="text-[#198754] hover:text-[#198754] hover:bg-muted/50 rounded-none h-9 px-3"
        >
          <ChevronLeft className="h-4 w-4 mr-1" /> Anterior
        </Button>
        <div className="w-px bg-border" />
        <Button
          variant="ghost" size="sm" onClick={onNextPage} disabled={!hasNextPage}
          className="text-[#198754] hover:text-[#198754] hover:bg-muted/50 rounded-none h-9 px-3"
        >
          Siguiente <ChevronRight className="h-4 w-4 ml-1" />
        </Button>
      </div>
    </div>
  );
}