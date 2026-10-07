import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

/** Avisos breves al pie de la pantalla (snack bar). */
@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private readonly snackBar = inject(MatSnackBar);

  exito(mensaje: string): void {
    this.snackBar.open(mensaje, 'Cerrar', { duration: 4000 });
  }

  error(mensajes: readonly string[]): void {
    this.snackBar.open(mensajes.join(' '), 'Cerrar', {
      duration: 8000,
      politeness: 'assertive',
    });
  }
}
