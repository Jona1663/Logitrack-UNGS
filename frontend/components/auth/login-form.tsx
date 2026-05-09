'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Eye, EyeOff, User, Lock, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useAuth } from '@/contexts/auth-context';
import { cn } from '@/lib/utils';

const loginSchema = z.object({
  username: z.string().min(1, 'El usuario es requerido'),
  password: z.string().min(1, 'La contraseña es requerida'),
});

type LoginFormData = z.infer<typeof loginSchema>;

// Agregá esta función arriba del componente LoginForm
function obtenerMensajeError(error: unknown): string {
  if (!(error instanceof Error)) return 'Error de autenticación';

  // Sin conexión al servidor
  if (error.message === 'Failed to fetch') {
    return 'No se pudo conectar con el servidor. Verifique su conexión o intente más tarde.';
  }

  // Credenciales incorrectas (viene de api.ts)
  if (error.message === 'Credenciales incorrectas o usuario inactivo') {
    return 'El usuario o la contraseña son incorrectos.';
  }

  // Cualquier otro error técnico
  return 'Ocurrió un error inesperado. Intente nuevamente.';
}

export function LoginForm() {
  const [showPassword, setShowPassword] = useState(false);
  const { login } = useAuth();

  const {
    register,
    handleSubmit,
    setError,
    reset,
    setFocus,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({ resolver: zodResolver(loginSchema) });

  const camposCompletos = watch('username') && watch('password');

  const onSubmit = async (data: LoginFormData) => {
    try {
      await login(data);
      toast.success('Bienvenido al sistema');
    } catch (error) {
      // Mostrar el mensaje de error
      const esSinConexion = error instanceof Error && error.message === 'Failed to fetch';
      toast.error(esSinConexion ? 'Sin conexión con el servidor' : 'Credenciales inválidas', {
        description: obtenerMensajeError(error),
      });

      if (!esSinConexion) {
        // Marcar campos con error para que se pongan rojos
        setError('username', { type: 'manual' });
        setError('password', { type: 'manual' });

        // Limpiar los campos (puedes elegir limpiar ambos o solo la contraseña)
        reset({ username: '', password: '' }, { keepErrors: true });
      }

      // Devolver el foco al campo de usuario (como en tu versión original)
      // setTimeout(() => setFocus('username'), 100);
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <div className="space-y-2">
        <Label htmlFor="username" className="text-foreground/80">
          Usuario
        </Label>
        <div className="relative">
          <User className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
          <Input
            id="username"
            type="text"
            placeholder="Ingrese su usuario"
            className={cn(
              "pl-10 h-12 bg-muted/50 border-0 focus-visible:ring-2",
              errors.username ? "ring-2 ring-destructive" : "focus-visible:ring-primary"
            )}
            {...register('username')}
            disabled={isSubmitting}
          />
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="password" className="text-foreground/80">
          Contraseña
        </Label>
        <div className="relative">
          <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
          <Input
            id="password"
            type={showPassword ? 'text' : 'password'}
            placeholder="Ingrese su contraseña"
            className={cn(
              "pl-10 pr-10 h-12 bg-muted/50 border-0 focus-visible:ring-2",
              errors.password ? "ring-2 ring-destructive" : "focus-visible:ring-primary"
            )}
            {...register('password')}
            disabled={isSubmitting}
          />
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
            tabIndex={-1}
          >
            {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
          </button>
        </div>
        {/* {(errors.username || errors.password) && (
          <p className="text-sm text-destructive font-medium">
            Verifique sus credenciales e intente nuevamente.
          </p>
        )} */}
      </div>

      <Button
        type="submit"
        className="w-full h-12 text-base font-semibold bg-gradient-to-r from-[#1b4332] to-[#2d6a4f] hover:from-[#2d6a4f] hover:to-[#40916c] transition-all duration-300"
        disabled={isSubmitting || !camposCompletos}
      >
        {isSubmitting ? (
          <>
            <Loader2 className="mr-2 h-5 w-5 animate-spin" />
            Autenticando...
          </>
        ) : (
          'INGRESAR AL SISTEMA'
        )}
      </Button>
    </form>
  );
}