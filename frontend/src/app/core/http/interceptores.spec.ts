import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { authInterceptor } from '../auth/auth.interceptor';
import { AuthService, URL_LOGIN } from '../auth/auth.service';
import { ApiError } from './api-error';
import { errorInterceptor } from './error.interceptor';

describe('authInterceptor y errorInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  const token = signal<string | null>(null);
  const auth = {
    token,
    autenticado: () => token() !== null,
    cerrarSesion: vi.fn(),
  };

  beforeEach(() => {
    token.set(null);
    auth.cerrarSesion.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    vi.spyOn(TestBed.inject(Router), 'url', 'get').mockReturnValue('/pacientes?q=ana');
  });

  afterEach(() => backend.verify());

  describe('cabecera Authorization', () => {
    it('se añade a las peticiones de la API cuando hay sesión', () => {
      token.set('tkn');

      http.get('/api/pacientes').subscribe();

      expect(backend.expectOne('/api/pacientes').request.headers.get('Authorization')).toBe(
        'Bearer tkn',
      );
    });

    it('no se añade al login', () => {
      token.set('tkn');

      http.post(URL_LOGIN, {}).subscribe();

      expect(backend.expectOne(URL_LOGIN).request.headers.has('Authorization')).toBe(false);
    });

    it('no se añade fuera de /api ni sin sesión', () => {
      http.get('/api/pacientes').subscribe();
      token.set('tkn');
      http.get('/assets/x.json').subscribe();

      expect(backend.expectOne('/api/pacientes').request.headers.has('Authorization')).toBe(false);
      expect(backend.expectOne('/assets/x.json').request.headers.has('Authorization')).toBe(false);
    });
  });

  describe('errores', () => {
    function pedirYFallar(url: string, status: number, cuerpo: object | null = null): unknown {
      let recibido: unknown;
      http.get(url).subscribe({ error: (e: unknown) => (recibido = e) });
      backend.expectOne(url).flush(cuerpo, { status, statusText: 'x' });
      return recibido;
    }

    it('convierte el ErrorResponse en ApiError', () => {
      const error = pedirYFallar('/api/medicamentos', 503, {
        status: 503,
        error: 'Service Unavailable',
        mensajes: ['El servicio de medicamentos no está disponible.'],
      });

      expect(error).toEqual(new ApiError(503, ['El servicio de medicamentos no está disponible.']));
      expect(auth.cerrarSesion).not.toHaveBeenCalled();
    });

    it('un 401 con sesión cierra la sesión recordando la ruta actual', () => {
      token.set('tkn');

      const error = pedirYFallar('/api/pacientes', 401);

      expect(error).toBeInstanceOf(ApiError);
      expect(auth.cerrarSesion).toHaveBeenCalledWith({
        motivo: 'expirada',
        returnUrl: '/pacientes?q=ana',
      });
    });

    it('un 401 del login no cierra nada (son credenciales inválidas)', () => {
      let recibido: unknown;
      http.post(URL_LOGIN, {}).subscribe({ error: (e: unknown) => (recibido = e) });
      backend
        .expectOne(URL_LOGIN)
        .flush(
          { status: 401, error: 'Unauthorized', mensajes: ['Credenciales inválidas.'] },
          { status: 401, statusText: 'Unauthorized' },
        );

      expect(recibido).toEqual(new ApiError(401, ['Credenciales inválidas.']));
      expect(auth.cerrarSesion).not.toHaveBeenCalled();
    });

    it('un 403 no cierra la sesión', () => {
      token.set('tkn');

      pedirYFallar('/api/pacientes', 403);

      expect(auth.cerrarSesion).not.toHaveBeenCalled();
    });
  });
});
