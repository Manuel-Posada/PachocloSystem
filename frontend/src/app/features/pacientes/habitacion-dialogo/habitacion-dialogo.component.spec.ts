import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/http/api-error';
import { Paciente } from '../paciente.models';
import { PacienteService } from '../paciente.service';
import { HabitacionDialogoComponent } from './habitacion-dialogo.component';

describe('HabitacionDialogoComponent', () => {
  const ana: Paciente = { idPaciente: 'PAC-0001', nombre: 'Ana Ruiz', edad: 40, habitacion: 12 };
  const servicio = { cambiarHabitacion: vi.fn<() => Observable<Paciente>>() };
  const dialogo = { close: vi.fn() };

  async function renderizar() {
    TestBed.configureTestingModule({
      imports: [HabitacionDialogoComponent],
      providers: [
        { provide: PacienteService, useValue: servicio },
        { provide: MatDialogRef, useValue: dialogo },
        { provide: MAT_DIALOG_DATA, useValue: { paciente: ana } },
      ],
    });
    const fixture = TestBed.createComponent(HabitacionDialogoComponent);
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;
    const input = elemento.querySelector<HTMLInputElement>('input')!;
    return {
      elemento,
      input,
      cambiarA: async (valor: string) => {
        input.value = valor;
        input.dispatchEvent(new Event('input'));
        elemento.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
        await fixture.whenStable();
      },
    };
  }

  beforeEach(() => vi.resetAllMocks());

  it('parte de la habitación actual', async () => {
    const { input } = await renderizar();

    expect(input.value).toBe('12');
  });

  it('cambia la habitación y se cierra con el paciente actualizado', async () => {
    servicio.cambiarHabitacion.mockReturnValue(of({ ...ana, habitacion: 30 }));
    const { cambiarA } = await renderizar();

    await cambiarA('30');

    expect(servicio.cambiarHabitacion).toHaveBeenCalledWith('PAC-0001', 30);
    expect(dialogo.close).toHaveBeenCalledWith({ ...ana, habitacion: 30 });
  });

  it('valida el rango en local', async () => {
    const { elemento, cambiarA } = await renderizar();

    await cambiarA('1000');

    expect(servicio.cambiarHabitacion).not.toHaveBeenCalled();
    expect(elemento.querySelector('mat-error')?.textContent?.trim()).toBe(
      'El número de habitación debe ser un entero entre 1 y 999.',
    );
  });

  it('muestra el error del servidor sin cerrarse', async () => {
    servicio.cambiarHabitacion.mockReturnValue(
      throwError(() => new ApiError(404, ['No se encontró el paciente PAC-0001.'])),
    );
    const { elemento, cambiarA } = await renderizar();

    await cambiarA('30');

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'No se encontró el paciente PAC-0001.',
    );
    expect(dialogo.close).not.toHaveBeenCalled();
  });
});
