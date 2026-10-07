import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Marcador de los módulos que llegan en fases posteriores (F2–F5). */
@Component({
  selector: 'app-en-construccion',
  template: `
    <h1>{{ titulo() }}</h1>
    <p>Este módulo todavía está en construcción.</p>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EnConstruccionComponent {
  /** Viene del `data` de la ruta (withComponentInputBinding). */
  readonly titulo = input.required<string>();
}
