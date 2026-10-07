import { ComponentType } from '@angular/cdk/portal';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  EMPTY,
  Subject,
  catchError,
  debounceTime,
  distinctUntilChanged,
  filter,
  map,
  merge,
  startWith,
  switchMap,
  tap,
} from 'rxjs';
import { mensajesDeError } from '../../../core/http/api-error';
import { NotificacionService } from '../../../core/notificacion.service';
import { confirmar } from '../../../shared/confirmacion-dialogo/confirmacion-dialogo.component';
import { HabitacionDialogoComponent } from '../habitacion-dialogo/habitacion-dialogo.component';
import { PacienteDialogoComponent } from '../paciente-dialogo/paciente-dialogo.component';
import { Paciente } from '../paciente.models';
import { PacienteService } from '../paciente.service';

/** Espera tras la última tecla antes de buscar en el servidor. */
export const ESPERA_BUSQUEDA_MS = 300;

/**
 * Lista de pacientes con búsqueda en el servidor. Las altas y ediciones se
 * hacen en diálogos que guardan ellos mismos; al cerrarse con éxito, la lista
 * se recarga.
 */
@Component({
  selector: 'app-pacientes-lista',
  imports: [
    ReactiveFormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    MatTooltipModule,
  ],
  templateUrl: './pacientes-lista.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PacientesListaComponent {
  private readonly servicio = inject(PacienteService);
  private readonly dialogo = inject(MatDialog);
  private readonly notificaciones = inject(NotificacionService);
  private readonly recargas = new Subject<void>();

  protected readonly columnas = ['idPaciente', 'nombre', 'edad', 'habitacion', 'acciones'];
  protected readonly busqueda = new FormControl('', { nonNullable: true });
  protected readonly pacientes = signal<readonly Paciente[]>([]);
  protected readonly cargando = signal(true);
  protected readonly errores = signal<readonly string[]>([]);
  /** Filtro de la última carga, para el mensaje de lista vacía. */
  protected readonly filtro = signal('');

  constructor() {
    merge(
      this.busqueda.valueChanges.pipe(
        debounceTime(ESPERA_BUSQUEDA_MS),
        map((texto) => texto.trim()),
        distinctUntilChanged(),
      ),
      this.recargas.pipe(map(() => this.busqueda.value.trim())),
    )
      .pipe(
        startWith(''),
        tap(() => this.cargando.set(true)),
        switchMap((texto) =>
          this.servicio.listar(texto).pipe(
            tap({
              next: (pacientes) => {
                this.pacientes.set(pacientes);
                this.filtro.set(texto);
                this.errores.set([]);
                this.cargando.set(false);
              },
              error: (error: unknown) => {
                this.errores.set(mensajesDeError(error));
                this.cargando.set(false);
              },
            }),
            catchError(() => EMPTY),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe();
  }

  protected recargar(): void {
    this.recargas.next();
  }

  protected nuevo(): void {
    this.abrirDialogo(PacienteDialogoComponent, {}, (p) => `Paciente ${p.idPaciente} registrado.`);
  }

  protected editar(paciente: Paciente): void {
    this.abrirDialogo(
      PacienteDialogoComponent,
      { paciente },
      (p) => `Paciente ${p.idPaciente} actualizado.`,
    );
  }

  protected cambiarHabitacion(paciente: Paciente): void {
    this.abrirDialogo(
      HabitacionDialogoComponent,
      { paciente },
      (p) => `${p.nombre} pasa a la habitación ${p.habitacion}.`,
    );
  }

  protected eliminar(paciente: Paciente): void {
    confirmar(this.dialogo, {
      titulo: 'Eliminar paciente',
      mensaje:
        `Se eliminará a ${paciente.nombre} (${paciente.idPaciente}) junto con todo su ` +
        'historial clínico. Esta acción no se puede deshacer.',
      accion: 'Eliminar',
    })
      .pipe(
        filter(Boolean),
        switchMap(() => this.servicio.eliminar(paciente.idPaciente)),
      )
      .subscribe({
        next: () => {
          this.notificaciones.exito(`Paciente ${paciente.idPaciente} eliminado.`);
          this.recargar();
        },
        error: (error: unknown) => {
          this.notificaciones.error(mensajesDeError(error));
          this.recargar();
        },
      });
  }

  /** Abre un diálogo que guarda por su cuenta; si se cierra con un paciente, avisa y recarga. */
  private abrirDialogo<C, D>(
    componente: ComponentType<C>,
    datos: D,
    mensajeExito: (guardado: Paciente) => string,
  ): void {
    this.dialogo
      .open<C, D, Paciente>(componente, { data: datos, width: '480px', maxWidth: '95vw' })
      .afterClosed()
      .pipe(filter((guardado): guardado is Paciente => guardado !== undefined))
      .subscribe((guardado) => {
        this.notificaciones.exito(mensajeExito(guardado));
        this.recargar();
      });
  }
}
