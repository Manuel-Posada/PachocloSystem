import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/http/api-error';
import { Trabajador } from '../trabajador.models';
import { TrabajadorService } from '../trabajador.service';
import { DatosTrabajadorDialogo, TrabajadorDialogoComponent } from './trabajador-dialogo.component';

describe('TrabajadorDialogoComponent', () => {
  const doctora: Trabajador = {
    idTrabajador: 'DOC-0001',
    nombreCompleto: 'Ana Ruiz',
    rol: 'Doctor',
    especialidad: 'Cardiología',
    nivelExperiencia: null,
  };
  const enfermero: Trabajador = {
    idTrabajador: 'ENF-0001',
    nombreCompleto: 'Luis Gil',
    rol: 'Enfermero',
    especialidad: null,
    nivelExperiencia: 'NOVATO',
  };
  const servicio = {
    registrar: vi.fn<() => Observable<Trabajador>>(),
    editar: vi.fn<() => Observable<Trabajador>>(),
  };
  const dialogo = { close: vi.fn() };

  async function renderizar(datos: DatosTrabajadorDialogo) {
    TestBed.configureTestingModule({
      imports: [TrabajadorDialogoComponent],
      providers: [
        { provide: TrabajadorService, useValue: servicio },
        { provide: MatDialogRef, useValue: dialogo },
        { provide: MAT_DIALOG_DATA, useValue: datos },
      ],
    });
    const fixture = TestBed.createComponent(TrabajadorDialogoComponent);
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;
    const estable = () => fixture.whenStable();
    return {
      elemento,
      escribir: async (nombre: string, valor: string) => {
        const input = elemento.querySelector<HTMLInputElement>(
          `input[formControlName="${nombre}"]`,
        )!;
        input.value = valor;
        input.dispatchEvent(new Event('input'));
        await estable();
      },
      /** Marca la opción de radio con esa etiqueta. */
      elegir: async (etiqueta: string) => {
        const opcion = Array.from(elemento.querySelectorAll('mat-radio-button')).find(
          (r) => r.textContent?.trim() === etiqueta,
        )!;
        opcion.querySelector<HTMLInputElement>('input')!.click();
        await estable();
      },
      radio: (etiqueta: string) =>
        Array.from(elemento.querySelectorAll('mat-radio-button'))
          .find((r) => r.textContent?.trim() === etiqueta)!
          .querySelector<HTMLInputElement>('input')!,
      enviar: async () => {
        elemento.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
        await estable();
      },
      tieneEspecialidad: () => !!elemento.querySelector('input[formControlName="especialidad"]'),
      tieneNivel: () => !!elemento.querySelector('[formControlName="nivelExperiencia"]'),
      errores: () =>
        Array.from(elemento.querySelectorAll('mat-error')).map((e) => e.textContent?.trim()),
    };
  }

  beforeEach(() => vi.resetAllMocks());

  it('sin rol no pide especialidad ni nivel, y exige elegir rol', async () => {
    const { tieneEspecialidad, tieneNivel, escribir, enviar, errores } = await renderizar({});

    expect(tieneEspecialidad()).toBe(false);
    expect(tieneNivel()).toBe(false);

    await escribir('nombre', 'Ana Ruiz');
    await enviar();

    expect(servicio.registrar).not.toHaveBeenCalled();
    expect(errores()).toEqual(['Debe seleccionar un rol.']);
  });

  it('un doctor pide especialidad y la valida', async () => {
    const { tieneEspecialidad, tieneNivel, escribir, elegir, enviar, errores } = await renderizar(
      {},
    );

    await escribir('nombre', 'Ana Ruiz');
    await elegir('Doctor');
    expect(tieneEspecialidad()).toBe(true);
    expect(tieneNivel()).toBe(false);

    await escribir('especialidad', '12');
    await enviar();

    expect(servicio.registrar).not.toHaveBeenCalled();
    expect(errores()).toEqual([
      'La especialidad debe ser un texto descriptivo (mínimo 3 caracteres).',
    ]);
  });

  it('registra un doctor solo con su especialidad', async () => {
    servicio.registrar.mockReturnValue(of(doctora));
    const { escribir, elegir, enviar } = await renderizar({});

    await escribir('nombre', ' Ana Ruiz ');
    await elegir('Doctor');
    await escribir('especialidad', ' Cardiología ');
    await enviar();

    expect(servicio.registrar).toHaveBeenCalledWith({
      nombre: 'Ana Ruiz',
      rol: 'Doctor',
      especialidad: 'Cardiología',
      nivelExperiencia: null,
    });
    expect(dialogo.close).toHaveBeenCalledWith(doctora);
  });

  it('un enfermero pide nivel de experiencia y se registra sin especialidad', async () => {
    servicio.registrar.mockReturnValue(of(enfermero));
    const { tieneEspecialidad, escribir, elegir, enviar, errores } = await renderizar({});

    await escribir('nombre', 'Luis Gil');
    await elegir('Doctor');
    await escribir('especialidad', 'Pediatría');
    await elegir('Enfermero');
    expect(tieneEspecialidad()).toBe(false);

    await enviar();
    expect(errores()).toEqual(['Debe seleccionar un nivel de experiencia.']);
    expect(servicio.registrar).not.toHaveBeenCalled();

    await elegir('Principiante');
    await enviar();

    expect(servicio.registrar).toHaveBeenCalledWith({
      nombre: 'Luis Gil',
      rol: 'Enfermero',
      especialidad: null,
      nivelExperiencia: 'PRINCIPIANTE',
    });
  });

  it('al editar bloquea el rol y conserva los datos de su rol', async () => {
    servicio.editar.mockReturnValue(of({ ...enfermero, nivelExperiencia: 'AVANZADO' }));
    const { radio, elegir, enviar } = await renderizar({ trabajador: enfermero });

    expect(radio('Doctor').disabled).toBe(true);
    expect(radio('Enfermero').disabled).toBe(true);
    expect(radio('Enfermero').checked).toBe(true);
    expect(radio('Novato').checked).toBe(true);

    await elegir('Avanzado');
    await enviar();

    expect(servicio.editar).toHaveBeenCalledWith('ENF-0001', {
      nombre: 'Luis Gil',
      rol: 'Enfermero',
      especialidad: null,
      nivelExperiencia: 'AVANZADO',
    });
  });

  it('muestra en el formulario todos los mensajes de un 400 y sigue abierto', async () => {
    servicio.editar.mockReturnValue(
      throwError(() => new ApiError(400, ['Primer mensaje.', 'Segundo mensaje.'])),
    );
    const { elemento, enviar } = await renderizar({ trabajador: doctora });

    await enviar();

    const mensajes = Array.from(elemento.querySelectorAll('[role="alert"] p')).map(
      (p) => p.textContent,
    );
    expect(mensajes).toEqual(['Primer mensaje.', 'Segundo mensaje.']);
    expect(dialogo.close).not.toHaveBeenCalled();
  });
});
