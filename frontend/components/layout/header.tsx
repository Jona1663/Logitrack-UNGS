'use client';

import { Wheat, LogOut, User } from 'lucide-react';
import Image from 'next/image';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
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
import { useAuth } from '@/contexts/auth-context';
import { normalizarEnum } from '@/lib/utils';

export function Header() {
  const { usuario, logout } = useAuth();

  const rolFormateado = usuario?.rol
    ? normalizarEnum(usuario.rol.replace('ROLE_', ''))
    : '';

  return (
    <header className="sticky top-0 z-50 w-full bg-gradient-to-r from-[#1b4332] to-[#2d6a4f] text-white shadow-lg">
      <div className="container mx-auto px-4">
        <div className="flex h-16 items-center justify-between">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-3 hover:opacity-90 transition-opacity">
            {/* <div className="p-1.5 bg-white/10 rounded-lg">
              <Wheat className="h-7 w-7" />
            </div> */}
            <div className="relative w-[50px] h-[50px]">
              <Image src="/images/logo-white.png" alt="Logo" fill style={{ objectFit: 'contain' }} />
            </div>
            <div className="hidden sm:block">
              <span className="text-xl font-bold tracking-tight">LogiTrack</span>
              <span className="text-sm text-white/80 ml-1">Agro</span>
            </div>
          </Link>

          {/* Usuario y Logout */}
          <div className="flex items-center gap-4">
            {/* Info usuario - Desktop */}
            <div className="hidden md:flex items-center gap-2 text-sm">
              <User className="h-4 w-4 text-white/70" />
              <span className="font-medium">{usuario?.username}</span>
              <span className="text-white/60">|</span>
              <span className="text-white/80">{rolFormateado}</span>
            </div>

            {/* Info usuario - Mobile */}
            <div className="md:hidden text-right">
              <p className="text-sm font-medium">{usuario?.username}</p>
              <p className="text-xs text-white/70">{rolFormateado}</p>
            </div>

            {/* Logout */}
            <AlertDialog>
              <AlertDialogTrigger asChild>
                <Button
                  variant="ghost"
                  size="sm"
                  className="text-white hover:bg-white/10 hover:text-white"
                >
                  <LogOut className="h-4 w-4 mr-2" />
                  <span className="hidden sm:inline">Salir</span>
                </Button>
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Cerrar sesion</AlertDialogTitle>
                  <AlertDialogDescription>
                    Esta a punto de cerrar su sesion. Debera ingresar sus credenciales
                    nuevamente para acceder al sistema.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Cancelar</AlertDialogCancel>
                  <AlertDialogAction
                    onClick={logout}
                    className="bg-destructive text-white hover:bg-destructive/90"
                  >
                    Cerrar sesion
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          </div>
        </div>
      </div>
    </header>
  );
}
