'use client';

import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from 'react';
import { useRouter, usePathname } from 'next/navigation';
import type { Usuario, RolUsuario, LoginRequest } from '@/types';
import { PERMISOS_POR_ROL } from '@/lib/constants';
import { api } from '@/lib/api';

// Normaliza el rol del backend al formato esperado
function normalizarRol(rol: string): RolUsuario {
  const rolUpper = rol.toUpperCase();
  
  // Si ya tiene el prefijo ROLE_, usarlo directamente
  if (rolUpper.startsWith('ROLE_')) {
    if (rolUpper in PERMISOS_POR_ROL) {
      return rolUpper as RolUsuario;
    }
  }
  
  // Si no tiene prefijo, agregarlo
  const rolConPrefijo = `ROLE_${rolUpper}` as RolUsuario;
  if (rolConPrefijo in PERMISOS_POR_ROL) {
    return rolConPrefijo;
  }
  
  // Mapeo de variantes comunes
  const mapeoRoles: Record<string, RolUsuario> = {
    'OPERADOR': 'ROLE_OPERADOR',
    'SUPERVISOR': 'ROLE_SUPERVISOR',
    'ADMIN': 'ROLE_ADMIN',
    'ADMINISTRADOR': 'ROLE_ADMIN',
    'CHOFER': 'ROLE_CHOFER',
    'DRIVER': 'ROLE_CHOFER',
  };
  
  if (rolUpper in mapeoRoles) {
    return mapeoRoles[rolUpper];
  }
  
  // Default a operador si no se reconoce
  console.warn(`Rol no reconocido: ${rol}, usando ROLE_OPERADOR por defecto`);
  return 'ROLE_OPERADOR';
}

interface AuthContextType {
  usuario: Usuario | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => void;
  permisos: typeof PERMISOS_POR_ROL[RolUsuario] | null;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();
  const pathname = usePathname();

  // Cargar usuario desde sessionStorage al inicio
  useEffect(() => {
    const cargarSesion = () => {
      try {
        const token = sessionStorage.getItem('jwt');
        const usuarioStr = sessionStorage.getItem('usuario');

        if (token && usuarioStr) {
          const usuarioData = JSON.parse(usuarioStr) as Usuario;
          // Normalizar el rol al cargar desde storage
          usuarioData.rol = normalizarRol(usuarioData.rol);
          setUsuario(usuarioData);
        }
      } catch (error) {
        console.error('Error cargando sesion:', error);
        sessionStorage.removeItem('jwt');
        sessionStorage.removeItem('usuario');
      } finally {
        setIsLoading(false);
      }
    };

    cargarSesion();
  }, []);

  // Redirigir segun rol y ruta
  useEffect(() => {
    if (isLoading) return;

    const esRutaPublica = pathname === '/login' || pathname === '/';
    const esRutaChofer = pathname.startsWith('/mi-viaje');
    const esRutaDashboard =
      pathname.startsWith('/menu') ||
      pathname.startsWith('/busqueda') ||
      pathname.startsWith('/envios') ||
      pathname.startsWith('/asignaciones')||
      pathname.startsWith('/auditoria');

    // Verificar si hay sesion en sessionStorage (puede que el estado aun no se haya actualizado)
    const tieneTokenEnStorage = typeof window !== 'undefined' && sessionStorage.getItem('jwt');

    // Si no esta autenticado y no esta en ruta publica, redirigir a login
    // Pero solo si tampoco hay token en sessionStorage (para evitar loop durante navegacion)
    if (!usuario && !esRutaPublica && !tieneTokenEnStorage) {
      router.replace('/login');
      return;
    }

    if (usuario) {
      const permisos = PERMISOS_POR_ROL[usuario.rol];

      // Chofer solo puede acceder a /mi-viaje
      if (usuario.rol === 'ROLE_CHOFER' && esRutaDashboard) {
        router.replace('/mi-viaje');
        return;
      }

      // Otros roles no pueden acceder a /mi-viaje
      if (usuario.rol !== 'ROLE_CHOFER' && esRutaChofer) {
        router.replace('/menu');
        return;
      }

      // Verificar acceso a auditoria
      if (pathname.startsWith('/auditoria') && !permisos.verAuditoria) {
        router.replace('/menu');
        return;
      }

      // Si esta autenticado y en login, redirigir segun rol
      if (pathname === '/login') {
        if (usuario.rol === 'ROLE_CHOFER') {
          router.replace('/mi-viaje');
        } else {
          router.replace('/menu');
        }
      }
    }
  }, [usuario, isLoading, pathname, router]);

  const login = useCallback(
    async (credentials: LoginRequest) => {
      const response = await api.login(credentials);

      // Normalizar el rol del backend
      const rolNormalizado = normalizarRol(response.rol);

      const nuevoUsuario: Usuario = {
        username: response.username,
        rol: rolNormalizado,
      };

      sessionStorage.setItem('jwt', response.token);
      sessionStorage.setItem('usuario', JSON.stringify(nuevoUsuario));
      setUsuario(nuevoUsuario);

      // Redirigir segun rol
      if (nuevoUsuario.rol === 'ROLE_CHOFER') {
        router.push('/mi-viaje');
      } else {
        router.push('/menu');
      }
    },
    [router]
  );

  const logout = useCallback(() => {
    sessionStorage.removeItem('jwt');
    sessionStorage.removeItem('usuario');
    setUsuario(null);
    router.push('/login');
  }, [router]);

  const permisos = usuario ? PERMISOS_POR_ROL[usuario.rol] : null;

  return (
    <AuthContext.Provider
      value={{
        usuario,
        isLoading,
        isAuthenticated: !!usuario,
        login,
        logout,
        permisos,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth debe usarse dentro de un AuthProvider');
  }
  return context;
}
