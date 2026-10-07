import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { mensajesDeError } from '../../../core/http/api-error';
import { ErroresFormularioComponent } from '../../../shared/errores-formulario/errores-formulario.component';
import { enteroEntre, obligatorio, primerError } from '../../../shared/validadores';
import { Medicamento, TipoMovimiento } from '../medicamento.models';
import { MedicamentoService } from '../medicamento.service';

export interface DatosMovimientoDialogo {
  medicamento: Medicamento;
  tipo: TipoMovimiento;
}

/**
 * Entrada o salida de stock. Si una salida supera el stock disponible (o el
 * medicamento está vencido) lo decide el servidor, que tiene el stock real,
 * y su mensaje se muestra en el formulario.
 */
@Component({
  selector: 'app-movimiento-dialogo',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    ErroresFormularioComponent,
  ],
  template: `
    <h2 mat-dialog-title>{{ esEntrada ? 'Entrada de stock' : 'Salida de stock' }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <app-errores-formulario [mensajes]="errores()" />
        <p>
          {{ medicamento.nombre }} {{ medicamento.concentracion }} ({{ medicamento.idMedicamento }},
          lote {{ medicamento.lote }}). Stock disponible: {{ medicamento.cantidadStock }}.
        </p>
        <mat-form-field class="campo-completo">
          <mat-label>{{ esEntrada ? 'Unidades que entran' : 'Unidades que salen' }}</mat-label>
          <input
            matInput
            type="number"
            formControlName="cantidad"
            min="1"
            max="1000000"
            step="1"
            required
            cdkFocusInitial
          />
          <mat-error>{{ primerError(formulario.controls.cantidad) }}</mat-error>
        </mat-form-field>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close [disabled]="enviando()">Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="enviando()">
          {{ esEntrada ? 'Registrar entrada' : 'Registrar salida' }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MovimientoDialogoComponent {
  private readonly servicio = inject(MedicamentoService);
  private readonly dialogo =
    inject<MatDialogRef<MovimientoDialogoComponent, Medicamento>>(MatDialogRef);
  private readonly datos = inject<DatosMovimientoDialogo>(MAT_DIALOG_DATA);

  protected readonly medicamento = this.datos.medicamento;
  protected readonly esEntrada = this.datos.tipo === 'entrada';
  protected readonly formulario = inject(NonNullableFormBuilder).group({
    cantidad: [
      null as number | null,
      [
        obligatorio('La cantidad es obligatoria.'),
        enteroEntre(1, 1_000_000, 'La cantidad debe ser un entero entre 1 y 1000000.'),
      ],
    ],
  });
  protected readonly enviando = signal(false);
  protected readonly errores = signal<readonly string[]>([]);
  protected readonly primerError = primerError;

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.errores.set([]);
    this.servicio
      .moverStock(
        this.medicamento.idMedicamento,
        this.datos.tipo,
        this.formulario.getRawValue().cantidad!,
      )
      .subscribe({
        next: (actualizado) => this.dialogo.close(actualizado),
        error: (error: unknown) => {
          this.errores.set(mensajesDeError(error));
          this.enviando.set(false);
        },
      });
  }
}
