'use client';

import { Search, PlusCircle, FileText, Truck } from 'lucide-react';
import Link from 'next/link';
import { Card, CardContent } from '@/components/ui/card';
import { useAuth } from '@/contexts/auth-context';
import { cn } from '@/lib/utils';

interface MenuCardProps {
  href: string;
  icon: React.ReactNode;
  title: string;
  description: string;
}

function MenuCard({ href, icon, title, description }: MenuCardProps) {
  return (
    <Link href={href} className="block group">
      <Card
        className={cn(
          'h-full transition-all duration-300',
          'hover:-translate-y-1 hover:shadow-lg hover:border-primary/50',
          'active:scale-[0.98] active:shadow-md',
          'cursor-pointer'
        )}
      >
        <CardContent className="p-6 flex flex-col items-center text-center gap-4">
          <div className="p-4 rounded-2xl bg-gradient-to-br from-[#1b4332] to-[#2d6a4f] text-white shadow-md group-hover:shadow-lg transition-shadow">
            {icon}
          </div>
          <div>
            <h3 className="text-lg font-semibold text-foreground mb-1">{title}</h3>
            <p className="text-sm text-muted-foreground">{description}</p>
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}

export default function MenuPage() {
  const { usuario, permisos } = useAuth();

  // El layout padre ya verifica autenticacion, esto es solo por seguridad
  if (!usuario || !permisos) {
    return null;
  }

  const menuItems = [
    {
      href: '/envios/nuevo',
      icon: <PlusCircle className="h-8 w-8" />,
      title: 'Crear Envio',
      description: 'Registra un nuevo envio en el sistema',
      show: permisos.crearEnvio,
    },
    {
      href: '/busqueda',
      icon: <Search className="h-8 w-8" />,
      title: 'Buscar Envios',
      description: 'Consulta y rastrea los envios registrados',
      show: permisos.verMenu,
    },
    {
      href: '/auditoria',
      icon: <FileText className="h-8 w-8" />,
      title: 'Historial Completo',
      description: 'Revisa el registro completo de auditoria',
      show: permisos.verAuditoria,
    },
        {
      href: '/asignaciones',
      icon: <Truck className="h-8 w-8" />,
      title: 'Asignar Transporte',
      description: 'Asigná chofer y camión a los envíos pendientes',
      show: permisos.asignarTransporte,
    },
  ];

  const visibleItems = menuItems.filter((item) => item.show);

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="max-w-4xl mx-auto">
        {/* Titulo */}
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-foreground mb-2">
            Panel Principal
          </h1>
          <p className="text-muted-foreground">
            Bienvenido, {usuario.username}. Selecciona una opcion para continuar
          </p>
        </div>

        {/* Grid de opciones */}
        <div
          className={cn(
            'grid gap-6',
            visibleItems.length === 1 && 'md:grid-cols-1 max-w-md mx-auto',
            visibleItems.length === 2 && 'md:grid-cols-2',
            visibleItems.length >= 3 && 'md:grid-cols-3'
          )}
        >
          {visibleItems.map((item) => (
            <MenuCard
              key={item.href}
              href={item.href}
              icon={item.icon}
              title={item.title}
              description={item.description}
            />
          ))}
        </div>
      </div>
    </div>
  );
}
