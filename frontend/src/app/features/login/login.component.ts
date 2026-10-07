import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { mensajesDeError } from '../../core/http/api-error';
import { ErroresFormularioComponent } from '../../shared/errores-formulario/errores-formulario.component';
import { obligatorio, primerError } from '../../shared/validadores';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    ErroresFormularioComponent,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly parametros = inject(ActivatedRoute).snapshot.queryParamMap;

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    username: ['', obligatorio('El usuario es obligatorio.')],
    password: ['', obligatorio('La contraseña es obligatoria.')],
  });
  protected readonly primerError = primerError;
  protected readonly enviando = signal(false);
  protected readonly errores = signal<readonly string[]>([]);
  protected readonly verPassword = signal(false);
  protected readonly sesionExpirada = this.parametros.get('motivo') === 'expirada';

  protected iniciarSesion(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.errores.set([]);
    this.auth.iniciarSesion(this.formulario.getRawValue()).subscribe({
      next: () => void this.router.navigateByUrl(destinoSeguro(this.parametros.get('returnUrl'))),
      error: (error: unknown) => {
        this.errores.set(mensajesDeError(error));
        this.enviando.set(false);
      },
    });
  }
}

/** Solo rutas internas: evita redirecciones abiertas con `?returnUrl=//otro.sitio`. */
export function destinoSeguro(url: string | null): string {
  const interna = url?.startsWith('/') && !url.startsWith('//') && !url.startsWith('/login');
  return interna ? url! : '/';
}
