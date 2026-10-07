import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/*
 * Validadores con las mismas reglas y mensajes que el backend (Bean Validation
 * de los DTO). Cada uno devuelve `{ <clave>: <mensaje> }`, así la plantilla solo
 * tiene que mostrar el mensaje con `primerError(control)`.
 */

/** Nombres de personas (pacientes y trabajadores): letras y espacios, 3 a 60 caracteres. */
export const PATRON_NOMBRE_PERSONA = /^[A-Za-zÁÉÍÓÚÑÜáéíóúñü][A-Za-zÁÉÍÓÚÑÜáéíóúñü\s]{2,59}$/;

const CONTIENE_LETRA = /[A-Za-zÁÉÍÓÚÑÜáéíóúñü]/;

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

/** Como `@Size(max)`, sobre el texto sin espacios al inicio ni al final. */
export function longitudMaxima(maximo: number, mensaje: string): ValidatorFn {
  return (control) =>
    String(control.value ?? '').trim().length <= maximo ? null : { longitudMaxima: mensaje };
}

/** El valor debe ser uno de los permitidos (p. ej. un enum del backend). Vacío lo valida `obligatorio`. */
export function unoDe(permitidos: readonly unknown[], mensaje: string): ValidatorFn {
  return (control) =>
    vacio(control.value) || permitidos.includes(control.value) ? null : { unoDe: mensaje };
}

/**
 * Texto con contenido real: al menos `minimo` caracteres (sin contar los
 * espacios de los extremos) y alguna letra, no solo números o símbolos.
 * Vacío lo valida `obligatorio`.
 */
export function textoDescriptivo(minimo: number, mensaje: string): ValidatorFn {
  return (control) => {
    if (vacio(control.value)) {
      return null;
    }
    const texto = String(control.value).trim();
    return texto.length >= minimo && CONTIENE_LETRA.test(texto)
      ? null
      : { textoDescriptivo: mensaje };
  };
}

export function nombrePersona(
  mensaje = 'El nombre debe tener letras y espacios (3 a 60 caracteres).',
): ValidatorFn {
  return patron(PATRON_NOMBRE_PERSONA, mensaje);
}

/** Especialidad de un doctor (reglas de `TrabajadorService.validarDatosDeRol`). */
export const especialidad = textoDescriptivo(
  3,
  'La especialidad debe ser un texto descriptivo (mínimo 3 caracteres).',
);

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
