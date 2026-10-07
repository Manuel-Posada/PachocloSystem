import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Permiso, PermisosService } from '../permisos';
import { AuthService } from './auth.service';

/** Solo con sesión; si no, al login recordando la ruta pedida. */
export const authGuard: CanActivateFn = (_ruta, estado) =>
  inject(AuthService).autenticado() ||
  inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: estado.url } });

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
