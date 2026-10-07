import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { Observable, catchError, switchMap, tap, throwError } from 'rxjs';
import { LoginRequest, LoginResponse, Usuario } from './auth.models';

export const URL_LOGIN = '/api/auth/login';
export const URL_USUARIO_ACTUAL = '/api/auth/me';

/** Antelación con la que se avisa de que la sesión va a expirar (no hay refresh token). */
export const AVISO_EXPIRACION_MS = 5 * 60_000;

const CLAVE_SESION = 'pachoclosystem.sesion';

interface Sesion {
  token: string;
  /** Instante de expiración del token, en ms desde epoch. */
  expiraEn: number;
  usuario: Usuario | null;
}

export interface OpcionesCierre {
  /** `expirada`: el login muestra que la sesión caducó o dejó de ser válida. */
  motivo?: 'expirada';
  /** Ruta a la que volver tras iniciar sesión de nuevo. */
  returnUrl?: string;
}

/**
 * Sesión del usuario: token JWT en memoria (signal) copiado en `sessionStorage`
 * para sobrevivir a una recarga; se pierde al cerrar la pestaña.
 *
 * El token dura lo que diga el backend (`expiraEnSegundos`) y no se renueva:
 * se avisa {@link AVISO_EXPIRACION_MS} antes y al expirar se cierra la sesión.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  private readonly sesion = signal<Sesion | null>(leerSesionGuardada());
  private temporizadores: ReturnType<typeof setTimeout>[] = [];

  readonly token = computed(() => this.sesion()?.token ?? null);
  readonly usuario = computed(() => this.sesion()?.usuario ?? null);
  readonly autenticado = computed(() => this.sesion() !== null);

  constructor() {
    const sesion = this.sesion();
    if (sesion) {
      this.programarExpiracion(sesion.expiraEn);
    }
  }

  /** Inicia sesión y carga el usuario actual. Si algo falla, no queda sesión abierta. */
  iniciarSesion(credenciales: LoginRequest): Observable<Usuario> {
    return this.http.post<LoginResponse>(URL_LOGIN, credenciales).pipe(
      tap((respuesta) =>
        this.guardar({
          token: respuesta.token,
          expiraEn: Date.now() + respuesta.expiraEnSegundos * 1000,
          usuario: null,
        }),
      ),
      switchMap(() => this.cargarUsuario()),
      catchError((error: unknown) => {
        this.limpiar();
        return throwError(() => error);
      }),
    );
  }

  /** Relee el usuario actual (`/api/auth/me`) y lo guarda en la sesión. */
  cargarUsuario(): Observable<Usuario> {
    return this.http.get<Usuario>(URL_USUARIO_ACTUAL).pipe(
      tap((usuario) => {
        const sesion = this.sesion();
        if (sesion) {
          this.guardar({ ...sesion, usuario });
        }
      }),
    );
  }

  /** Borra la sesión y lleva al login. */
  cerrarSesion(opciones: OpcionesCierre = {}): void {
    this.limpiar();
    void this.router.navigate(['/login'], {
      queryParams: { motivo: opciones.motivo, returnUrl: opciones.returnUrl },
    });
  }

  private guardar(sesion: Sesion): void {
    const expiracionCambio = this.sesion()?.expiraEn !== sesion.expiraEn;
    this.sesion.set(sesion);
    try {
      sessionStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
    } catch {
      // Sin almacenamiento (p. ej. bloqueado): la sesión dura hasta recargar.
    }
    if (expiracionCambio) {
      this.programarExpiracion(sesion.expiraEn);
    }
  }

  private limpiar(): void {
    this.cancelarTemporizadores();
    this.sesion.set(null);
    try {
      sessionStorage.removeItem(CLAVE_SESION);
    } catch {
      // Nada que borrar.
    }
  }

  private programarExpiracion(expiraEn: number): void {
    this.cancelarTemporizadores();
    const restante = expiraEn - Date.now();
    const minutosAviso = Math.ceil(Math.min(restante, AVISO_EXPIRACION_MS) / 60_000);
    this.temporizadores = [
      setTimeout(
        () => this.avisarExpiracion(minutosAviso),
        Math.max(restante - AVISO_EXPIRACION_MS, 0),
      ),
      setTimeout(
        () => this.cerrarSesion({ motivo: 'expirada', returnUrl: this.router.url }),
        restante,
      ),
    ];
  }

  private avisarExpiracion(minutos: number): void {
    const plazo = minutos === 1 ? '1 minuto' : `${minutos} minutos`;
    this.snackBar.open(
      `Su sesión expira en ${plazo}. Guarde sus cambios: después deberá iniciar sesión de nuevo.`,
      'Entendido',
      { duration: 30_000 },
    );
  }

  private cancelarTemporizadores(): void {
    this.temporizadores.forEach(clearTimeout);
    this.temporizadores = [];
  }
}

/** Sesión de `sessionStorage`, o `null` si no hay, está corrupta o ya expiró. */
function leerSesionGuardada(): Sesion | null {
  try {
    const guardada = JSON.parse(sessionStorage.getItem(CLAVE_SESION) ?? 'null') as Sesion | null;
    if (
      typeof guardada?.token === 'string' &&
      typeof guardada.expiraEn === 'number' &&
      guardada.expiraEn > Date.now()
    ) {
      return {
        token: guardada.token,
        expiraEn: guardada.expiraEn,
        usuario: guardada.usuario ?? null,
      };
    }
    sessionStorage.removeItem(CLAVE_SESION);
  } catch {
    // JSON corrupto o almacenamiento no disponible: se empieza sin sesión.
  }
  return null;
}
