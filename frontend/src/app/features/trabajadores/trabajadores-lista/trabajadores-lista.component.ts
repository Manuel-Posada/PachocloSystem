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
import { etiquetaRol } from '../../../core/roles';
import { confirmar } from '../../../shared/confirmacion-dialogo/confirmacion-dialogo.component';
import { ESPERA_BUSQUEDA_MS } from '../../../shared/listas';
import {
  DatosTrabajadorDialogo,
  TrabajadorDialogoComponent,
} from '../trabajador-dialogo/trabajador-dialogo.component';
import { ETIQUETAS_NIVEL, Trabajador } from '../trabajador.models';
import { TrabajadorService } from '../trabajador.service';

/** Lista de trabajadores; mismo patrón que la de pacientes (ver README). */
@Component({
  selector: 'app-trabajadores-lista',
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
  templateUrl: './trabajadores-lista.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrabajadoresListaComponent {
  private readonly servicio = inject(TrabajadorService);
  private readonly dialogo = inject(MatDialog);
  private readonly notificaciones = inject(NotificacionService);
  private readonly recargas = new Subject<void>();

  protected readonly columnas = ['idTrabajador', 'nombreCompleto', 'rol', 'detalle', 'acciones'];
  protected readonly busqueda = new FormControl('', { nonNullable: true });
  protected readonly trabajadores = signal<readonly Trabajador[]>([]);
  protected readonly cargando = signal(true);
  protected readonly errores = signal<readonly string[]>([]);
  /** Filtro de la última carga, para el mensaje de lista vacía. */
  protected readonly filtro = signal('');
  protected readonly etiquetaRol = etiquetaRol;

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
              next: (trabajadores) => {
                this.trabajadores.set(trabajadores);
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

  /** Especialidad del doctor o nivel del enfermero. */
  protected detalle(trabajador: Trabajador): string {
    return trabajador.nivelExperiencia
      ? `Nivel ${ETIQUETAS_NIVEL[trabajador.nivelExperiencia].toLowerCase()}`
      : (trabajador.especialidad ?? '');
  }

  protected recargar(): void {
    this.recargas.next();
  }

  protected nuevo(): void {
    this.abrirDialogo({}, (t) => `Trabajador ${t.idTrabajador} registrado.`);
  }

  protected editar(trabajador: Trabajador): void {
    this.abrirDialogo({ trabajador }, (t) => `Trabajador ${t.idTrabajador} actualizado.`);
  }

  protected eliminar(trabajador: Trabajador): void {
    confirmar(this.dialogo, {
      titulo: 'Eliminar trabajador',
      mensaje:
        `Se eliminará a ${trabajador.nombreCompleto} (${trabajador.idTrabajador}). Si tiene un ` +
        'usuario de acceso, también quedará desactivado y no podrá volver a iniciar sesión. ' +
        'Esta acción no se puede deshacer.',
      accion: 'Eliminar',
    })
      .pipe(
        filter(Boolean),
        switchMap(() => this.servicio.eliminar(trabajador.idTrabajador)),
      )
      .subscribe({
        next: () => {
          this.notificaciones.exito(`Trabajador ${trabajador.idTrabajador} eliminado.`);
          this.recargar();
        },
        error: (error: unknown) => {
          this.notificaciones.error(mensajesDeError(error));
          this.recargar();
        },
      });
  }

  /** Abre el diálogo, que guarda por su cuenta; si se cierra con un trabajador, avisa y recarga. */
  private abrirDialogo(
    datos: DatosTrabajadorDialogo,
    mensajeExito: (guardado: Trabajador) => string,
  ): void {
    this.dialogo
      .open<TrabajadorDialogoComponent, DatosTrabajadorDialogo, Trabajador>(
        TrabajadorDialogoComponent,
        { data: datos, width: '480px', maxWidth: '95vw' },
      )
      .afterClosed()
      .pipe(filter((guardado): guardado is Trabajador => guardado !== undefined))
      .subscribe((guardado) => {
        this.notificaciones.exito(mensajeExito(guardado));
        this.recargar();
      });
  }
}
