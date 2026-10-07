import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { mensajesDeError } from '../../core/http/api-error';
import { ErroresFormularioComponent } from '../../shared/errores-formulario/errores-formulario.component';
import { obligatorio, primerError } from '../../shared/validadores';
import { distintaDe, passwordValida, repiteA } from '../usuarios/politica';

/**
 * Cambio de la contraseña propia (`POST /api/auth/password`). Es obligatorio
 * tras un alta o un restablecimiento (`authGuard` trae aquí y no deja salir) y
 * voluntario desde el menú de usuario. El backend invalida el token al
 * cambiarla, así que al terminar se vuelve al login.
 */
@Component({
  selector: 'app-cambiar-password',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    ErroresFormularioComponent,
  ],
  templateUrl: './cambiar-password.component.html',
  styleUrl: './cambiar-password.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CambiarPasswordComponent {
  private readonly auth = inject(AuthService);
  private readonly fb = inject(NonNullableFormBuilder);

  /** Si es obligatorio no hay vuelta atrás: solo cambiarla o cerrar sesión. */
  protected readonly obligatorio = this.auth.debeCambiarPassword();
  protected readonly usuario = this.auth.usuario;

  private readonly actualControl = this.fb.control(
    '',
    obligatorio('La contraseña actual es obligatoria.'),
  );
  private readonly nuevaControl = this.fb.control('', [
    passwordValida(() => this.auth.usuario()?.username ?? null),
    distintaDe(() => this.actualControl.value),
  ]);
  protected readonly formulario = this.fb.group({
    actual: this.actualControl,
    nueva: this.nuevaControl,
    repeticion: ['', repiteA(() => this.nuevaControl.value)],
  });
  protected readonly primerError = primerError;
  protected readonly enviando = signal(false);
  protected readonly errores = signal<readonly string[]>([]);

  constructor() {
    const { actual, nueva, repeticion } = this.formulario.controls;
    actual.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => nueva.updateValueAndValidity({ emitEvent: false }));
    nueva.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => repeticion.updateValueAndValidity({ emitEvent: false }));
  }

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { actual, nueva } = this.formulario.getRawValue();
    this.enviando.set(true);
    this.errores.set([]);
    // Si va bien, AuthService cierra la sesión y lleva al login.
    this.auth.cambiarPassword({ passwordActual: actual, passwordNueva: nueva }).subscribe({
      error: (error: unknown) => {
        this.errores.set(mensajesDeError(error));
        this.enviando.set(false);
      },
    });
  }

  protected cerrarSesion(): void {
    this.auth.cerrarSesion();
  }
}
