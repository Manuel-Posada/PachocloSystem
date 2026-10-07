import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { Observable, map } from 'rxjs';
import { mensajesDeError } from '../../core/http/api-error';
import { ErroresFormularioComponent } from '../errores-formulario/errores-formulario.component';

export interface DatosConfirmacion {
  titulo: string;
  mensaje: string;
  /** Texto del botón que confirma, p. ej. "Eliminar". */
  accion: string;
  /**
   * Opcional: la operación a ejecutar al confirmar. Si se da, el diálogo la
   * ejecuta él mismo, muestra dentro sus errores (p. ej. un 409) sin cerrarse y
   * solo se cierra con `true` si sale bien.
   */
  ejecutar?: () => Observable<unknown>;
  /** Estilo del botón de confirmar: de peligro (por defecto) o normal. */
  peligro?: boolean;
}

/** Diálogo de confirmación; se cierra con `true` solo si se confirma (y, si hay operación, sale bien). */
@Component({
  selector: 'app-confirmacion-dialogo',
  imports: [MatDialogModule, MatButtonModule, ErroresFormularioComponent],
  template: `
    <h2 mat-dialog-title>{{ datos.titulo }}</h2>
    <mat-dialog-content>
      <app-errores-formulario [mensajes]="errores()" />
      <p>{{ datos.mensaje }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="false" [disabled]="enviando()">
        Cancelar
      </button>
      <button
        mat-flat-button
        type="button"
        [class.peligro]="datos.peligro !== false"
        [disabled]="enviando()"
        (click)="confirmar()"
      >
        {{ datos.accion }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .peligro {
      background-color: var(--mat-sys-error);
      color: var(--mat-sys-on-error);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmacionDialogoComponent {
  protected readonly datos = inject<DatosConfirmacion>(MAT_DIALOG_DATA);
  private readonly dialogo =
    inject<MatDialogRef<ConfirmacionDialogoComponent, boolean>>(MatDialogRef);
  protected readonly enviando = signal(false);
  protected readonly errores = signal<readonly string[]>([]);

  protected confirmar(): void {
    if (!this.datos.ejecutar) {
      this.dialogo.close(true);
      return;
    }
    this.enviando.set(true);
    this.errores.set([]);
    this.datos.ejecutar().subscribe({
      next: () => this.dialogo.close(true),
      error: (error: unknown) => {
        this.errores.set(mensajesDeError(error));
        this.enviando.set(false);
      },
    });
  }
}

/** Abre la confirmación y emite `true` solo si el usuario confirma. */
export function confirmar(dialogo: MatDialog, datos: DatosConfirmacion): Observable<boolean> {
  return dialogo
    .open<ConfirmacionDialogoComponent, DatosConfirmacion, boolean>(ConfirmacionDialogoComponent, {
      data: datos,
      autoFocus: 'dialog',
    })
    .afterClosed()
    .pipe(map((confirmado) => confirmado === true));
}
