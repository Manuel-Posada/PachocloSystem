import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Solo con sesión; si no, al login recordando la ruta pedida. */
export const authGuard: CanActivateFn = (_ruta, estado) =>
  inject(AuthService).autenticado() ||
  inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: estado.url } });

/** Solo sin sesión (pantalla de login); con sesión, al inicio. */
export const invitadoGuard: CanActivateFn = () =>
  !inject(AuthService).autenticado() || inject(Router).createUrlTree(['/']);
