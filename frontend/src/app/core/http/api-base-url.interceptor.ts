import { HttpInterceptorFn } from '@angular/common/http';
import { InjectionToken, inject } from '@angular/core';
import { environment } from '../../../environments/environment';

/**
 * Origen de la API sin barra final (p. ej. `https://api.example.com`), o vacío
 * si está en el mismo origen que el frontend.
 */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => environment.apiBaseUrl,
});

/**
 * Antepone {@link API_BASE_URL} a las peticiones relativas a `/api/`. Va el
 * último: los demás interceptores ven siempre la ruta relativa (`/api/...`), así
 * que decidir si una petición lleva token no depende de dónde esté la API.
 */
export const apiBaseUrlInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const base = inject(API_BASE_URL);
  if (!base || !peticion.url.startsWith('/api/')) {
    return siguiente(peticion);
  }
  return siguiente(peticion.clone({ url: base + peticion.url }));
};
