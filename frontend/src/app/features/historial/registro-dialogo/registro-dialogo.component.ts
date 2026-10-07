import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { startWith } from 'rxjs';
import { mensajesDeError } from '../../../core/http/api-error';
import { ErroresFormularioComponent } from '../../../shared/errores-formulario/errores-formulario.component';
import {
  contieneLetra,
  enteroEntre,
  longitudMinima,
  numeroEntre,
  obligatorio,
  primerError,
} from '../../../shared/validadores';
import { Paciente } from '../../pacientes/paciente.models';
import {
  ETIQUETAS_TIPO,
  Registro,
  RegistroRequest,
  TIPOS_REGISTRO,
  TipoRegistro,
} from '../historial.models';
import { HistorialService } from '../historial.service';

export interface DatosRegistroDialogo {
  paciente: Paciente;
  /** Trabajador del usuario actual (`me.idTrabajador`): regla provisional hasta B3. */
  idAutor: string;
}

const MENSAJE_PRESION = 'La presión diastólica debe ser menor que la sistólica.';

/** Como `HistorialClinicoService`: la diastólica debe ser menor que la sistólica. */
function diastolicaMenorQueSistolica(grupo: AbstractControl): ValidationErrors | null {
  const sistolica = grupo.get('presionSistolica')?.value as number | null;
  const diastolica = grupo.get('presionDiastolica')?.value as number | null;
  if (sistolica === null || diastolica === null) {
    return null;
  }
  return Number(diastolica) >= Number(sistolica) ? { presion: MENSAJE_PRESION } : null;
}

/**
 * Nuevo registro del historial de un paciente. Las validaciones son las de
 * `RegistroRequest`, `SignosVitalesRequest` y `HistorialClinicoService`, con sus
 * mensajes: SIGNOS_VITALES pide los signos estructurados; el resto, contenido.
 */
@Component({
  selector: 'app-registro-dialogo',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    MatButtonModule,
    ErroresFormularioComponent,
  ],
  templateUrl: './registro-dialogo.component.html',
  styleUrl: './registro-dialogo.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegistroDialogoComponent {
  private readonly servicio = inject(HistorialService);
  private readonly dialogo = inject<MatDialogRef<RegistroDialogoComponent, Registro>>(MatDialogRef);
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly datos = inject<DatosRegistroDialogo>(MAT_DIALOG_DATA);

  protected readonly formulario = this.fb.group({
    tipo: this.fb.control<TipoRegistro | null>(
      null,
      obligatorio('Debe seleccionar un tipo de registro.'),
    ),
    contenido: [
      '',
      [
        obligatorio('El contenido no puede estar vacío.'),
        longitudMinima(5, 'El contenido es demasiado corto (mínimo 5 caracteres).'),
        contieneLetra('El contenido debe incluir texto descriptivo, no solo números.'),
      ],
    ],
    signos: this.fb.group(
      {
        temperatura: this.fb.control<number | null>(null, [
          obligatorio('Temperatura: es obligatoria.'),
          numeroEntre(30, 45, 'Temperatura: debe ser un número entre 30.0 y 45.0 °C.'),
        ]),
        frecCardiaca: this.fb.control<number | null>(null, [
          obligatorio('Frecuencia Cardíaca: es obligatoria.'),
          enteroEntre(20, 250, 'Frecuencia Cardíaca: debe ser entre 20 y 250 lpm.'),
        ]),
        presionSistolica: this.fb.control<number | null>(null, [
          obligatorio('Presión Sistólica: es obligatoria.'),
          enteroEntre(50, 250, 'Presión Sistólica: debe ser entre 50 y 250 mmHg.'),
        ]),
        presionDiastolica: this.fb.control<number | null>(null, [
          obligatorio('Presión Diastólica: es obligatoria.'),
          enteroEntre(30, 150, 'Presión Diastólica: debe ser entre 30 y 150 mmHg.'),
        ]),
        frecRespiratoria: this.fb.control<number | null>(null, [
          obligatorio('Frecuencia Respiratoria: es obligatoria.'),
          enteroEntre(5, 60, 'Frecuencia Respiratoria: debe ser entre 5 y 60 rpm.'),
        ]),
        saturacion: this.fb.control<number | null>(null, [
          obligatorio('Saturación de Oxígeno: es obligatoria.'),
          enteroEntre(0, 100, 'Saturación de Oxígeno: debe ser entre 0 y 100%.'),
        ]),
        observaciones: ['', contieneLetra('Las observaciones no pueden ser solo números.')],
      },
      { validators: diastolicaMenorQueSistolica },
    ),
  });
  protected readonly tipo = toSignal(
    this.formulario.controls.tipo.valueChanges.pipe(startWith(this.formulario.controls.tipo.value)),
    { requireSync: true },
  );
  protected readonly enviando = signal(false);
  protected readonly errores = signal<readonly string[]>([]);

  protected readonly tipos = TIPOS_REGISTRO;
  protected readonly etiquetasTipo = ETIQUETAS_TIPO;
  protected readonly primerError = primerError;
  protected readonly signos = this.formulario.controls.signos.controls;

  constructor() {
    this.formulario.controls.tipo.valueChanges
      .pipe(startWith(this.formulario.controls.tipo.value), takeUntilDestroyed())
      .subscribe((tipo) => this.habilitarCamposDeTipo(tipo));
  }

  /** Error de presión del grupo, cuando ya se tocó alguna de las dos. */
  protected errorPresion(): string | null {
    const grupo = this.formulario.controls.signos;
    const tocada = this.signos.presionSistolica.touched || this.signos.presionDiastolica.touched;
    return tocada && grupo.hasError('presion') ? MENSAJE_PRESION : null;
  }

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.errores.set([]);
    this.servicio.crear(this.datos.paciente.idPaciente, this.solicitud()).subscribe({
      next: (creado) => this.dialogo.close(creado),
      error: (error: unknown) => {
        this.errores.set(mensajesDeError(error));
        this.enviando.set(false);
      },
    });
  }

  private solicitud(): RegistroRequest {
    const v = this.formulario.getRawValue();
    const tipo = v.tipo!;
    const esSignos = tipo === 'SIGNOS_VITALES';
    const observaciones = v.signos.observaciones.trim();
    return {
      tipo,
      idAutor: this.datos.idAutor,
      contenido: esSignos ? null : v.contenido.trim(),
      signosVitales: esSignos
        ? {
            temperatura: v.signos.temperatura!,
            frecCardiaca: v.signos.frecCardiaca!,
            presionSistolica: v.signos.presionSistolica!,
            presionDiastolica: v.signos.presionDiastolica!,
            frecRespiratoria: v.signos.frecRespiratoria!,
            saturacion: v.signos.saturacion!,
            observaciones: observaciones || null,
          }
        : null,
      idMedicamento: null,
      cantidad: null,
    };
  }

  /** Solo cuentan para la validación los campos del tipo elegido. */
  private habilitarCamposDeTipo(tipo: TipoRegistro | null): void {
    const { contenido, signos } = this.formulario.controls;
    const opciones = { emitEvent: false };
    if (tipo !== null && tipo !== 'SIGNOS_VITALES') {
      contenido.enable(opciones);
    } else {
      contenido.disable(opciones);
    }
    if (tipo === 'SIGNOS_VITALES') {
      signos.enable(opciones);
    } else {
      signos.disable(opciones);
    }
  }
}
