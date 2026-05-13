'use client';

import { Wheat, MapPin, TrendingUp } from 'lucide-react';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { LoginForm } from '@/components/auth/login-form';
import { useAuth } from '@/contexts/auth-context';
import { Spinner } from '@/components/ui/spinner';
import Image from 'next/image';

export default function LoginPage() {
  const { isLoading, isAuthenticated } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-muted">
        <Spinner className="h-8 w-8 text-[#1b4332]" />
      </div>
    );
  }

  if (isAuthenticated) return null;

  return (
    <div className="min-h-screen flex bg-white">
      {/* Panel izquierdo - Branding (Fiel al index.html original) */}
      <div className="hidden lg:flex lg:w-7/12 flex-col justify-between p-12 text-white bg-gradient-to-br from-[#1b4332] to-[#081c15]">

        {/* Header Logo */}
        <div className="flex items-center gap-3">
          {/* Si quieres usar tu imagen original, descomenta las líneas de abajo y borra el div del icono Wheat */}
          <div className="relative w-[70px] h-[70px]">
            <Image src="/images/logo-white.png" alt="Logo" fill style={{ objectFit: 'contain' }} />
          </div>
          {/* <div className="p-2 bg-white/10 rounded-xl backdrop-blur-sm">
            <Wheat className="h-8 w-8 text-warning" />
          </div> */}
          <span className="text-3xl font-bold tracking-tight">LogiTrack Agro</span>
        </div>

        {/* Main Content */}
        <div className="max-w-2xl">
          <h1 className="text-5xl lg:text-6xl font-extrabold mb-6 leading-tight">
            Conectando al campo <br /> argentino.
          </h1>
          <p className="text-2xl font-light text-white/75 mb-12">
            Sistema Federal de Gestión de Logística y Distribución de Granos y Semillas.
          </p>

          <div className="flex gap-12 mt-12">
            <div className="text-center">
              <MapPin className="h-8 w-8 text-yellow-400 mx-auto mb-2" />
              <p className="text-sm font-medium">Trazabilidad</p>
            </div>
            <div className="w-px bg-white/20"></div>
            <div className="text-center">
              <TrendingUp className="h-8 w-8 text-yellow-400 mx-auto mb-2" />
              <p className="text-sm font-medium">Eficiencia</p>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="text-sm text-white/50">
          &copy; 2026 LogiTrack Agro. Potenciando la cadena agroindustrial.
        </div>
      </div>

      {/* Panel derecho - Login */}
      <div className="w-full lg:w-5/12 flex items-center justify-center p-6 lg:p-12 relative">
        <div className="w-full max-w-[500px]">

          <div className="text-center mb-10">
            {/* Logo móvil (visible solo en pantallas chicas) */}
            <div className="lg:hidden flex justify-center mb-6">
              {/* <div className="flex items-center gap-3 p-3 bg-gradient-to-br from-[#1b4332] to-[#2d6a4f] rounded-2xl shadow-lg">
                <Wheat className="h-10 w-10 text-white" />
              </div> */}
              <div className="relative w-[150px] h-[150px]">
                <Image src="/images/logo-logitrack.png" alt="Logo" fill style={{ objectFit: 'contain' }} />
              </div>
            </div>

            <h2 className="text-3xl font-bold text-gray-900 mb-2">
              Iniciar Sesión
            </h2>
            <p className="text-gray-500">
              Ingrese sus credenciales operativas.
            </p>
          </div>

          <LoginForm />

        </div>
      </div>
    </div>
  );
}