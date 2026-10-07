import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Registro } from '../historial.models';
import { RegistrosComponent } from './registros.component';

describe('RegistrosComponent', () => {
  const signos: Registro = {
    idRegistro: 'r1',
    idPaciente: 'PAC-0001',
    nombrePaciente: 'Ana Ruiz',
    fecha: '2026-10-06T20:21:00.915452',
    tipo: 'SIGNOS_VITALES',
    autor: {
      idTrabajador: 'ENF-0001',
      nombreCompleto: 'Luis Gil',
      rol: 'Enfermero',
      especialidad: null,
      nivelExperiencia: 'NOVATO',
    },
    contenido:
      'Signos vitales - Temp: 36.5°C | FC: 80 lpm | PA: 120/80 mmHg | FR: 16 rpm | SpO2: 98%',
  };
  const medicacion: Registro = {
    ...signos,
    idRegistro: 'r2',
    tipo: 'MEDICACION',
    contenido: 'Paracetamol 500 mg vía oral',
    medicacion: { idMedicamento: 'MED-0001', cantidad: 2 },
  };

  async function renderizar(registros: Registro[], mostrarPaciente = false) {
    TestBed.configureTestingModule({
      imports: [RegistrosComponent],
      providers: [provideRouter([])],
    });
    const fixture = TestBed.createComponent(RegistrosComponent);
    fixture.componentRef.setInput('registros', registros);
    fixture.componentRef.setInput('mostrarPaciente', mostrarPaciente);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  const texto = (e: Element | null) => e?.textContent?.replace(/\s+/g, ' ').trim();

  it('muestra tipo, fecha sin convertir, contenido tal cual y autor', async () => {
    const elemento = await renderizar([signos]);

    expect(texto(elemento.querySelector('.tipo'))).toBe('Signos vitales');
    expect(texto(elemento.querySelector('time'))).toBe('06/10/2026 20:21');
    expect(elemento.querySelector('time')?.getAttribute('datetime')).toBe(signos.fecha);
    expect(texto(elemento.querySelector('.contenido'))).toBe(signos.contenido);
    expect(texto(elemento.querySelector('.autor'))).toBe('Luis Gil (ENF-0001) · Enfermero');
    expect(elemento.querySelector('.paciente')).toBeNull();
    expect(elemento.querySelector('.medicacion')).toBeNull();
  });

  it('indica el stock descontado en una medicación', async () => {
    const elemento = await renderizar([medicacion]);

    expect(texto(elemento.querySelector('.medicacion'))).toBe(
      'Descontó 2 unidades de MED-0001 del inventario.',
    );
  });

  it('en el historial general enlaza al historial del paciente', async () => {
    const elemento = await renderizar([signos], true);

    const enlace = elemento.querySelector<HTMLAnchorElement>('.paciente a')!;
    expect(texto(enlace)).toBe('Ana Ruiz (PAC-0001)');
    expect(enlace.getAttribute('href')).toBe('/pacientes/PAC-0001/historial');
  });
});
