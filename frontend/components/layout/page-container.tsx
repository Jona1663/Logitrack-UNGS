'use client';

import { ArrowLeft } from 'lucide-react';
import Link from 'next/link';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { cn } from '@/lib/utils';

interface PageContainerProps {
  title: string;
  backHref?: string;
  backLabel?: string;
  children: React.ReactNode;
  className?: string;
  headerActions?: React.ReactNode;
}

export function PageContainer({
  title,
  backHref,
  backLabel = 'Volver',
  children,
  className,
  headerActions,
}: PageContainerProps) {
  return (
    <div className="container mx-auto px-4 py-6">
      {/* Enlace volver */}
      {backHref && (
        <Link
          href={backHref}
          className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors mb-4"
        >
          <ArrowLeft className="h-4 w-4" />
          {backLabel}
        </Link>
      )}

      {/* Card principal */}
      <Card className={cn('shadow-sm', className)}>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4">
          <CardTitle className="text-xl font-semibold">{title}</CardTitle>
          {headerActions}
        </CardHeader>
        <CardContent>{children}</CardContent>
      </Card>
    </div>
  );
}
