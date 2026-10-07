import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { MAT_ICON_DEFAULT_OPTIONS } from '@angular/material/icon';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { catchError, of } from 'rxjs';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { AuthService } from './core/auth/auth.service';
import { apiBaseUrlInterceptor } from './core/http/api-base-url.interceptor';
import { errorInterceptor } from './core/http/error.interceptor';

/**
 * Con una sesión recuperada de sessionStorage, relee el usuario antes de
 * arrancar. Si el token ya no vale (401), errorInterceptor cierra la sesión;
 * cualquier otro fallo se ignora y la app arranca con el usuario guardado.
 */
function refrescarUsuario() {
  const auth = inject(AuthService);
  return auth.autenticado() ? auth.cargarUsuario().pipe(catchError(() => of(null))) : undefined;
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    // apiBaseUrlInterceptor va el último: los demás ven siempre la ruta relativa /api/...
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor, apiBaseUrlInterceptor])),
    provideAppInitializer(refrescarUsuario),
    // index.html carga la fuente "Material Symbols Outlined", no la clásica "Material Icons".
    { provide: MAT_ICON_DEFAULT_OPTIONS, useValue: { fontSet: 'material-symbols-outlined' } },
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline' } },
  ],
};
