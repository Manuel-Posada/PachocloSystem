import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/*
 * Validadores con las mismas reglas y mensajes que el backend (Bean Validation
 * de los DTO). Cada uno devuelve `{ <clave>: <mensaje> }`, así la plantilla solo
 * tiene que mostrar el mensaje con `primerError(control)`.
 */

/** Nombres de personas (pacientes y trabajadores): letras y espacios, 3 a 60 caracteres. */
export const PATRON_NOMBRE_PERSONA = /^[A-Za-zÁÉÍÓÚÑÜáéíóúñü][A-Za-zÁÉÍÓÚÑÜáéíóúñü\s]{2,59}$/;

function vacio(valor: unknown): boolean {
  return valor === null || valor === undefined || String(valor).trim() === '';
}

/** Como `@NotBlank` / `@NotNull`: rechaza vacío y solo espacios (`Validators.required` acepta "   "). */
export function obligatorio(mensaje: string): ValidatorFn {
  return (control) => (vacio(control.value) ? { obligatorio: mensaje } : null);
}

/** Como `@Pattern`, sobre el texto sin espacios al inicio ni al final. Vacío lo valida `obligatorio`. */
export function patron(regex: RegExp, mensaje: string): ValidatorFn {
  return (control) =>
    vacio(control.value) || regex.test(String(control.value).trim()) ? null : { patron: mensaje };
}

/** Como `@Min` + `@Max` sobre un entero. Vacío lo valida `obligatorio`. */
export function enteroEntre(min: number, max: number, mensaje: string): ValidatorFn {
  return (control) => {
    if (vacio(control.value)) {
      return null;
    }
    const numero = Number(control.value);
    return Number.isInteger(numero) && numero >= min && numero <= max
      ? null
      : { enteroEntre: mensaje };
  };
}

export function nombrePersona(
  mensaje = 'El nombre debe tener letras y espacios (3 a 60 caracteres).',
): ValidatorFn {
  return patron(PATRON_NOMBRE_PERSONA, mensaje);
}

export const edad = enteroEntre(0, 120, 'La edad debe ser un número entero entre 0 y 120.');

export const habitacion = enteroEntre(
  1,
  999,
  'El número de habitación debe ser un entero entre 1 y 999.',
);

/** Mensaje del primer error del control, o `null` si es válido. */
export function primerError(control: AbstractControl): string | null {
  const errores: ValidationErrors | null = control.errors;
  if (!errores) {
    return null;
  }
  const valor: unknown = Object.values(errores)[0];
  return typeof valor === 'string' ? valor : 'El valor no es válido.';
}
