import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/http/api-error';
import { Medicamento, TipoMovimiento } from '../medicamento.models';
import { MedicamentoService } from '../medicamento.service';
import { MovimientoDialogoComponent } from './movimiento-dialogo.component';

describe('MovimientoDialogoComponent', () => {
  const dolex = {
    idMedicamento: 'MED-0001',
    nombre: 'Dolex',
    concentracion: '500 mg',
    lote: 'L-1',
    cantidadStock: 5,
  } as Medicamento;
  const servicio = { moverStock: vi.fn<() => Observable<Medicamento>>() };
  const dialogo = { close: vi.fn() };

  async function renderizar(tipo: TipoMovimiento) {
    TestBed.configureTestingModule({
      imports: [MovimientoDialogoComponent],
      providers: [
        { provide: MedicamentoService, useValue: servicio },
        { provide: MatDialogRef, useValue: dialogo },
        { provide: MAT_DIALOG_DATA, useValue: { medicamento: dolex, tipo } },
      ],
    });
    const fixture = TestBed.createComponent(MovimientoDialogoComponent);
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;
    return {
      elemento,
      registrar: async (cantidad: string) => {
        const input = elemento.querySelector<HTMLInputElement>('input')!;
        input.value = cantidad;
        input.dispatchEvent(new Event('input'));
        elemento.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
        await fixture.whenStable();
      },
    };
  }

  beforeEach(() => vi.resetAllMocks());

  it('registra una entrada y se cierra con el medicamento actualizado', async () => {
    servicio.moverStock.mockReturnValue(of({ ...dolex, cantidadStock: 15 }));
    const { elemento, registrar } = await renderizar('entrada');

    expect(elemento.querySelector('[mat-dialog-title]')?.textContent).toBe('Entrada de stock');
    expect(elemento.textContent).toContain('Stock disponible: 5.');
    await registrar('10');

    expect(servicio.moverStock).toHaveBeenCalledWith('MED-0001', 'entrada', 10);
    expect(dialogo.close).toHaveBeenCalledWith({ ...dolex, cantidadStock: 15 });
  });

  it.each(['0', '1000001', '2.5'])('rechaza en local la cantidad %s', async (cantidad) => {
    const { elemento, registrar } = await renderizar('salida');

    await registrar(cantidad);

    expect(servicio.moverStock).not.toHaveBeenCalled();
    expect(elemento.querySelector('mat-error')?.textContent?.trim()).toBe(
      'La cantidad debe ser un entero entre 1 y 1000000.',
    );
  });

  it('deja al servidor decidir si hay stock y muestra su error', async () => {
    servicio.moverStock.mockReturnValue(
      throwError(() => new ApiError(400, ['Stock insuficiente: disponible 5, solicitado 8.'])),
    );
    const { elemento, registrar } = await renderizar('salida');

    await registrar('8');

    expect(servicio.moverStock).toHaveBeenCalledWith('MED-0001', 'salida', 8);
    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'Stock insuficiente: disponible 5, solicitado 8.',
    );
    expect(dialogo.close).not.toHaveBeenCalled();
  });
});
