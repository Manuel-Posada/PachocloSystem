import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/http/api-error';
import { Paciente } from '../paciente.models';
import { PacienteService } from '../paciente.service';
import { DatosPacienteDialogo, PacienteDialogoComponent } from './paciente-dialogo.component';

describe('PacienteDialogoComponent', () => {
  const ana: Paciente = { idPaciente: 'PAC-0001', nombre: 'Ana Ruiz', edad: 40, habitacion: 12 };
  const servicio = {
    registrar: vi.fn<() => Observable<Paciente>>(),
    editar: vi.fn<() => Observable<Paciente>>(),
  };
  const dialogo = { close: vi.fn() };

  async function renderizar(datos: DatosPacienteDialogo) {
    TestBed.configureTestingModule({
      imports: [PacienteDialogoComponent],
      providers: [
        { provide: PacienteService, useValue: servicio },
        { provide: MatDialogRef, useValue: dialogo },
        { provide: MAT_DIALOG_DATA, useValue: datos },
      ],
    });
    const fixture = TestBed.createComponent(PacienteDialogoComponent);
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;
    const campo = (nombre: string) =>
      elemento.querySelector<HTMLInputElement>(`input[formControlName="${nombre}"]`)!;
    return {
      elemento,
      campo,
      escribir: (nombre: string, valor: string) => {
        campo(nombre).value = valor;
        campo(nombre).dispatchEvent(new Event('input'));
      },
      enviar: async () => {
        elemento.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
        await fixture.whenStable();
      },
      erroresCampos: () =>
        Array.from(elemento.querySelectorAll('mat-error')).map((e) => e.textContent?.trim()),
    };
  }

  beforeEach(() => vi.resetAllMocks());

  it('valida en local sin llamar al servidor', async () => {
    const { escribir, enviar, erroresCampos } = await renderizar({});

    escribir('nombre', 'Ana3');
    escribir('edad', '130');
    await enviar();

    expect(servicio.registrar).not.toHaveBeenCalled();
    expect(erroresCampos()).toEqual([
      'El nombre debe tener letras y espacios (3 a 60 caracteres).',
      'La edad debe ser un número entero entre 0 y 120.',
      'El número de habitación es obligatorio.',
    ]);
  });

  it('registra con el nombre recortado y se cierra con el paciente creado', async () => {
    servicio.registrar.mockReturnValue(of(ana));
    const { escribir, enviar } = await renderizar({});

    escribir('nombre', '  Ana Ruiz ');
    escribir('edad', '40');
    escribir('habitacion', '12');
    await enviar();

    expect(servicio.registrar).toHaveBeenCalledWith({
      nombre: 'Ana Ruiz',
      edad: 40,
      habitacion: 12,
    });
    expect(dialogo.close).toHaveBeenCalledWith(ana);
  });

  it('en edición parte de los datos actuales y guarda con PUT', async () => {
    servicio.editar.mockReturnValue(of({ ...ana, edad: 41 }));
    const { elemento, campo, escribir, enviar } = await renderizar({ paciente: ana });

    expect(elemento.querySelector('[mat-dialog-title]')?.textContent).toContain('PAC-0001');
    expect(campo('nombre').value).toBe('Ana Ruiz');
    escribir('edad', '41');
    await enviar();

    expect(servicio.editar).toHaveBeenCalledWith('PAC-0001', {
      nombre: 'Ana Ruiz',
      edad: 41,
      habitacion: 12,
    });
    expect(dialogo.close).toHaveBeenCalledWith({ ...ana, edad: 41 });
  });

  it('muestra en el formulario todos los mensajes de un 400 y sigue abierto', async () => {
    servicio.editar.mockReturnValue(
      throwError(() => new ApiError(400, ['Mensaje uno.', 'Mensaje dos.'])),
    );
    const { elemento, enviar } = await renderizar({ paciente: ana });

    await enviar();

    const mensajes = Array.from(elemento.querySelectorAll('[role="alert"] p')).map(
      (p) => p.textContent,
    );
    expect(mensajes).toEqual(['Mensaje uno.', 'Mensaje dos.']);
    expect(dialogo.close).not.toHaveBeenCalled();
    expect(elemento.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(
      false,
    );
  });
});
