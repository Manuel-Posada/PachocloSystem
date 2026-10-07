import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { RegistroRequest } from './historial.models';
import { HistorialService } from './historial.service';

describe('HistorialService', () => {
  let servicio: HistorialService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(HistorialService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista el historial general con filtro y, si hay, texto recortado', () => {
    servicio.listar('autor', ' ana ').subscribe();
    servicio.listar('todos', '  ').subscribe();

    http.expectOne('/api/historial?filtro=autor&q=ana').flush([]);
    http.expectOne('/api/historial?filtro=todos').flush([]);
  });

  it('lista el historial de un paciente, opcionalmente filtrado por autor', () => {
    servicio.listarDePaciente('PAC-0001').subscribe();
    servicio.listarDePaciente('PAC-0001', 'DOC').subscribe();

    http.expectOne('/api/pacientes/PAC-0001/historial').flush([]);
    http.expectOne('/api/pacientes/PAC-0001/historial?q=DOC').flush([]);
  });

  it('crea un registro con POST', () => {
    const registro: RegistroRequest = {
      tipo: 'MEDICACION',
      contenido: 'Paracetamol 500 mg vía oral',
      signosVitales: null,
      idMedicamento: 'MED-0001',
      cantidad: 2,
    };

    servicio.crear('PAC-0001', registro).subscribe();

    const peticion = http.expectOne('/api/pacientes/PAC-0001/historial');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual(registro);
    peticion.flush({});
  });
});
