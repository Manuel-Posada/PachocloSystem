import { AbstractControl, ValidationErrors } from '@angular/forms';

/**
 * Como `@NotBlank` del backend: rechaza vacío y solo espacios. Error: `obligatorio`.
 * (`Validators.required` acepta "   ").
 */
export function obligatorio(control: AbstractControl<unknown>): ValidationErrors | null {
  const valor = control.value;
  const vacio = valor === null || valor === undefined || String(valor).trim() === '';
  return vacio ? { obligatorio: true } : null;
}
