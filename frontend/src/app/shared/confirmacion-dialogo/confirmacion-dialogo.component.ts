import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Observable, map } from 'rxjs';

export interface DatosConfirmacion {
  titulo: string;
  mensaje: string;
  /** Texto del botón que confirma, p. ej. "Eliminar". */
  accion: string;
}

/** Diálogo de confirmación; se cierra con `true` solo si se confirma. */
@Component({
  selector: 'app-confirmacion-dialogo',
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ datos.titulo }}</h2>
    <mat-dialog-content>
      <p>{{ datos.mensaje }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="false">Cancelar</button>
      <button mat-flat-button type="button" class="peligro" [mat-dialog-close]="true">
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
