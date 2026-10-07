import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { UsuarioService } from './usuario.service';

describe('UsuarioService', () => {
  let servicio: UsuarioService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(UsuarioService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista con filtro recortado y omite el filtro en blanco', () => {
    servicio.listar(' eva ').subscribe();
    servicio.listar(' ').subscribe();

    http.expectOne('/api/usuarios?q=eva').flush([]);
    http.expectOne('/api/usuarios').flush([]);
  });

  it('crea con POST', () => {
    const datos = {
      username: 'eva.mora',
      password: 'clave-de-pruebas-10',
      rol: 'DOCTOR' as const,
      idTrabajador: 'DOC-0001',
    };

    servicio.crear(datos).subscribe();

    const peticion = http.expectOne('/api/usuarios');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual(datos);
    peticion.flush({});
  });

  it.each([
    ['desactivar', '/api/usuarios/USR-0002/desactivar'],
    ['activar', '/api/usuarios/USR-0002/activar'],
  ] as const)('%s con PATCH', (operacion, url) => {
    servicio[operacion]('USR-0002').subscribe();

    const peticion = http.expectOne(url);
    expect(peticion.request.method).toBe('PATCH');
    peticion.flush({});
  });

  it('restablece la contraseña con PATCH', () => {
    servicio.restablecerPassword('USR-0002', 'nueva-clave-segura').subscribe();

    const peticion = http.expectOne('/api/usuarios/USR-0002/password');
    expect(peticion.request.method).toBe('PATCH');
    expect(peticion.request.body).toEqual({ password: 'nueva-clave-segura' });
    peticion.flush(null, { status: 204, statusText: 'No Content' });
  });
});
