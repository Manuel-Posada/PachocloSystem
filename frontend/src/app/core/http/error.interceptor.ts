import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { requiereToken } from '../auth/auth.interceptor';
import { AuthService } from '../auth/auth.service';
import { ApiError } from './api-error';

/**
 * Convierte todo error HTTP en {@link ApiError}. Un 401 fuera del login
 * significa que el token expiró o el usuario fue desactivado: se cierra la
 * sesión y se vuelve al login recordando la ruta actual.
 */
export const errorInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return siguiente(peticion).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) {
        return throwError(() => error);
      }
      if (error.status === 401 && requiereToken(peticion) && auth.autenticado()) {
        auth.cerrarSesion({ motivo: 'expirada', returnUrl: router.url });
      }
      return throwError(() => ApiError.desde(error));
    }),
  );
};
