import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Caja con todos los mensajes de error que devolvió el servidor para un formulario. */
@Component({
  selector: 'app-errores-formulario',
  template: `
    @if (mensajes().length > 0) {
      <div class="errores" role="alert">
        @for (mensaje of mensajes(); track $index) {
          <p>{{ mensaje }}</p>
        }
      </div>
    }
  `,
  styles: `
    .errores {
      margin: 0 0 16px;
      padding: 8px 16px;
      border-radius: 8px;
      background-color: var(--mat-sys-error-container);
      color: var(--mat-sys-on-error-container);
    }

    p {
      margin: 4px 0;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErroresFormularioComponent {
  readonly mensajes = input.required<readonly string[]>();
}
