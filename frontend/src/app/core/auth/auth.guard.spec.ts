import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { PermisosService } from '../permisos';
import { authGuard, invitadoGuard, permisoGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('guards de sesión', () => {
  let autenticado = false;
  let permitido = false;
  const ruta = {} as ActivatedRouteSnapshot;
  const estado = { url: '/trabajadores' } as RouterStateSnapshot;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { autenticado: () => autenticado } },
        { provide: PermisosService, useValue: { puede: () => permitido } },
      ],
    });
  });

  const ejecutar = (guard: typeof authGuard) =>
    TestBed.runInInjectionContext(() => guard(ruta, estado));
  const url = (resultado: unknown) => TestBed.inject(Router).serializeUrl(resultado as UrlTree);

  it('authGuard deja pasar con sesión', () => {
    autenticado = true;
    expect(ejecutar(authGuard)).toBe(true);
  });

  it('authGuard sin sesión lleva al login con la ruta pedida', () => {
    autenticado = false;
    expect(url(ejecutar(authGuard))).toBe('/login?returnUrl=%2Ftrabajadores');
  });

  it('permisoGuard deja pasar con el permiso y, sin él, lleva al inicio', () => {
    permitido = true;
    expect(ejecutar(permisoGuard('trabajadores.leer'))).toBe(true);

    permitido = false;
    expect(url(ejecutar(permisoGuard('trabajadores.leer')))).toBe('/');
  });

  it('invitadoGuard deja pasar sin sesión', () => {
    autenticado = false;
    expect(ejecutar(invitadoGuard)).toBe(true);
  });

  it('invitadoGuard con sesión lleva al inicio', () => {
    autenticado = true;
    expect(url(ejecutar(invitadoGuard))).toBe('/');
  });
});
