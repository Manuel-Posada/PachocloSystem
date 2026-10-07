import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-no-encontrado',
  imports: [RouterLink, MatButtonModule],
  template: `
    <main class="no-encontrado">
      <h1>Página no encontrada</h1>
      <p>La dirección que buscó no existe.</p>
      <a mat-flat-button routerLink="/">Ir al inicio</a>
    </main>
  `,
  styles: `
    .no-encontrado {
      padding: 48px 16px;
      text-align: center;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NoEncontradoComponent {}
