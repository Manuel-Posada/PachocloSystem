import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { startWith } from 'rxjs';
import { mensajesDeError } from '../../../core/http/api-error';
import { ROLES_TRABAJADOR, RolTrabajador, etiquetaRol } from '../../../core/roles';
import { ErroresFormularioComponent } from '../../../shared/errores-formulario/errores-formulario.component';
import { especialidad, nombrePersona, obligatorio, primerError } from '../../../shared/validadores';
import {
  ETIQUETAS_NIVEL,
  NIVELES_EXPERIENCIA,
  NivelExperiencia,
  Trabajador,
  TrabajadorRequest,
} from '../trabajador.models';
import { TrabajadorService } from '../trabajador.service';

export interface DatosTrabajadorDialogo {
  /** Sin trabajador: alta. Con trabajador: edición (el rol queda bloqueado). */
  trabajador?: Trabajador;
}

/**
 * Alta y edición de un trabajador. El formulario depende del rol: un doctor
 * pide especialidad y un enfermero nivel de experiencia; el campo del otro
 * rol se deshabilita para que no cuente en la validación.
 */
@Component({
  selector: 'app-trabajador-dialogo',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    MatButtonModule,
    ErroresFormularioComponent,
  ],
  templateUrl: './trabajador-dialogo.component.html',
  styleUrl: './trabajador-dialogo.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrabajadorDialogoComponent {
  private readonly servicio = inject(TrabajadorService);
  private readonly dialogo =
    inject<MatDialogRef<TrabajadorDialogoComponent, Trabajador>>(MatDialogRef);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly trabajador = inject<DatosTrabajadorDialogo>(MAT_DIALOG_DATA).trabajador;
  protected readonly formulario = this.fb.group({
    nombre: [
      this.trabajador?.nombreCompleto ?? '',
      [
        obligatorio('El nombre completo es obligatorio.'),
        nombrePersona('El nombre debe tener solo letras y espacios (3 a 60 caracteres).'),
      ],
    ],
    rol: this.fb.control<RolTrabajador | null>(
      this.trabajador?.rol ?? null,
      obligatorio('Debe seleccionar un rol.'),
    ),
    especialidad: [
      this.trabajador?.especialidad ?? '',
      [obligatorio('La especialidad es obligatoria.'), especialidad],
    ],
    nivelExperiencia: this.fb.control<NivelExperiencia | null>(
      this.trabajador?.nivelExperiencia ?? null,
      obligatorio('Debe seleccionar un nivel de experiencia.'),
    ),
  });
  protected readonly rol = toSignal(
    this.formulario.controls.rol.valueChanges.pipe(startWith(this.formulario.controls.rol.value)),
    { requireSync: true },
  );
  protected readonly enviando = signal(false);
  protected readonly errores = signal<readonly string[]>([]);

  protected readonly roles = ROLES_TRABAJADOR;
  protected readonly niveles = NIVELES_EXPERIENCIA;
  protected readonly etiquetaRol = etiquetaRol;
  protected readonly etiquetasNivel = ETIQUETAS_NIVEL;
  protected readonly primerError = primerError;

  constructor() {
    this.formulario.controls.rol.valueChanges
      .pipe(startWith(this.formulario.controls.rol.value), takeUntilDestroyed())
      .subscribe((rol) => this.habilitarCamposDeRol(rol));
    if (this.trabajador) {
      this.formulario.controls.rol.disable();
    }
  }

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    const rol = valores.rol!;
    const datos: TrabajadorRequest = {
      nombre: valores.nombre.trim(),
      rol,
      especialidad: rol === 'Doctor' ? valores.especialidad.trim() : null,
      nivelExperiencia: rol === 'Enfermero' ? valores.nivelExperiencia : null,
    };
    this.enviando.set(true);
    this.errores.set([]);
    const peticion = this.trabajador
      ? this.servicio.editar(this.trabajador.idTrabajador, datos)
      : this.servicio.registrar(datos);
    peticion.subscribe({
      next: (guardado) => this.dialogo.close(guardado),
      error: (error: unknown) => {
        this.errores.set(mensajesDeError(error));
        this.enviando.set(false);
      },
    });
  }

  private habilitarCamposDeRol(rol: RolTrabajador | null): void {
    const { especialidad, nivelExperiencia } = this.formulario.controls;
    const opciones = { emitEvent: false };
    if (rol === 'Doctor') {
      especialidad.enable(opciones);
    } else {
      especialidad.disable(opciones);
    }
    if (rol === 'Enfermero') {
      nivelExperiencia.enable(opciones);
    } else {
      nivelExperiencia.disable(opciones);
    }
  }
}
