import { HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService, URL_LOGIN } from './auth.service';

/** Peticiones a nuestra API que exigen token (todas menos el login). */
export function requiereToken(peticion: HttpRequest<unknown>): boolean {
  return peticion.url.startsWith('/api/') && peticion.url !== URL_LOGIN;
}

/** Añade `Authorization: Bearer <token>` a las peticiones de la API. */
export const authInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const token = inject(AuthService).token();
  if (!token || !requiereToken(peticion)) {
    return siguiente(peticion);
  }
  return siguiente(peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
