import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Permiso, PermisosService } from '../permisos';
import { AuthService } from './auth.service';

/**
 * Solo con sesión; si no, al login recordando la ruta pedida. Con la
 * contraseña pendiente de cambio, a la pantalla de cambiarla: el backend
 * respondería 403 a todo lo demás.
 */
export const authGuard: CanActivateFn = (_ruta, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.autenticado()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: estado.url } });
  }
  return !auth.debeCambiarPassword() || router.createUrlTree(['/cambiar-password']);
};

/** Pantalla de cambio de contraseña: basta con tener sesión, esté o no pendiente el cambio. */
export const sesionGuard: CanActivateFn = () =>
  inject(AuthService).autenticado() || inject(Router).createUrlTree(['/login']);

/**
 * Solo si el rol del usuario tiene ese permiso; si no, al inicio. Evita entrar
 * por URL a pantallas cuyo backend respondería 403.
 */
export function permisoGuard(permiso: Permiso): CanActivateFn {
  return () => inject(PermisosService).puede(permiso) || inject(Router).createUrlTree(['/']);
}

/** Solo sin sesión (pantalla de login); con sesión, al inicio. */
export const invitadoGuard: CanActivateFn = () =>
  !inject(AuthService).autenticado() || inject(Router).createUrlTree(['/']);
