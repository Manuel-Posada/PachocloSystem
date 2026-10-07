import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Trabajador, TrabajadorRequest } from './trabajador.models';
import { TrabajadorService } from './trabajador.service';

describe('TrabajadorService', () => {
  let servicio: TrabajadorService;
  let http: HttpTestingController;
  const doctora: Trabajador = {
    idTrabajador: 'DOC-0001',
    nombreCompleto: 'Ana Ruiz',
    rol: 'Doctor',
    especialidad: 'Cardiología',
    nivelExperiencia: null,
  };
  const datos: TrabajadorRequest = {
    nombre: 'Ana Ruiz',
    rol: 'Doctor',
    especialidad: 'Cardiología',
    nivelExperiencia: null,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(TrabajadorService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista con filtro recortado y omite el filtro en blanco', () => {
    let recibido: Trabajador[] = [];
    servicio.listar(' ana ').subscribe((l) => (recibido = l));
    servicio.listar('  ').subscribe();

    http.expectOne('/api/trabajadores?q=ana').flush([doctora]);
    http.expectOne('/api/trabajadores').flush([]);
    expect(recibido).toEqual([doctora]);
  });

  it('registra con POST', () => {
    servicio.registrar(datos).subscribe();

    const peticion = http.expectOne('/api/trabajadores');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual(datos);
    peticion.flush(doctora);
  });

  it('edita con PUT', () => {
    servicio.editar('DOC-0001', datos).subscribe();

    const peticion = http.expectOne('/api/trabajadores/DOC-0001');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual(datos);
    peticion.flush(doctora);
  });

  it('elimina con DELETE', () => {
    servicio.eliminar('DOC-0001').subscribe();

    const peticion = http.expectOne('/api/trabajadores/DOC-0001');
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush(null, { status: 204, statusText: 'No Content' });
  });
});
