import { ApplicationRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { confirmar } from './confirmacion-dialogo.component';

describe('confirmar', () => {
  const datos = { titulo: 'Eliminar paciente', mensaje: '¿Seguro?', accion: 'Eliminar' };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } }],
    });
  });

  afterEach(() => TestBed.inject(MatDialog).closeAll());

  /** Devuelve un objeto (no la promesa) para que `await abrir()` no espere al cierre. */
  async function abrir(): Promise<{ resultado: Promise<boolean> }> {
    const resultado = firstValueFrom(confirmar(TestBed.inject(MatDialog), datos));
    await TestBed.inject(ApplicationRef).whenStable();
    return { resultado };
  }

  const boton = (texto: string) =>
    Array.from(document.querySelectorAll<HTMLButtonElement>('mat-dialog-actions button')).find(
      (b) => b.textContent?.trim() === texto,
    )!;

  it('muestra el título y el mensaje', async () => {
    await abrir();

    expect(document.querySelector('[mat-dialog-title]')?.textContent).toBe('Eliminar paciente');
    expect(document.querySelector('mat-dialog-content')?.textContent).toContain('¿Seguro?');
  });

  it('emite true al confirmar', async () => {
    const { resultado } = await abrir();

    boton('Eliminar').click();

    expect(await resultado).toBe(true);
  });

  it('emite false al cancelar', async () => {
    const { resultado } = await abrir();

    boton('Cancelar').click();

    expect(await resultado).toBe(false);
  });
});
