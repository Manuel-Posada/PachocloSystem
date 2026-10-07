import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/*
 * Copia de las reglas de UsuarioService.crearUsuario/validarPassword y de
 * Usuario.PATRON_USERNAME del backend, con los mismos mensajes y el mismo
 * orden. Si cambian allí, cambian aquí.
 */

export const LONGITUD_MINIMA_PASSWORD = 10;
/** Límite de BCrypt, en bytes UTF-8 (las letras con tilde y la ñ ocupan 2). */
export const MAXIMO_BYTES_PASSWORD = 72;

const PATRON_USERNAME = /^[a-z0-9._-]{3,30}$/;

/** Como `Usuario.normalizarUsername`: sin espacios en los extremos y en minúsculas. */
export function normalizarUsername(username: string | null | undefined): string | null {
  return username == null ? null : username.trim().toLowerCase();
}

/** Mensaje de error del username, o `null` si es válido (se valida ya normalizado). */
export function errorUsername(username: string | null | undefined): string | null {
  const normalizado = normalizarUsername(username);
  return normalizado !== null && PATRON_USERNAME.test(normalizado)
    ? null
    : 'El username debe tener entre 3 y 30 caracteres y solo puede contener minúsculas, ' +
        'dígitos, punto, guion bajo o guion.';
}

/**
 * Errores de la política de contraseña, como `validarPassword` del backend:
 * si es corta solo se informa eso; si no, el límite de bytes y la igualdad con
 * el username. La longitud cuenta caracteres sin recortar, igual que Java.
 */
export function erroresPassword(
  password: string | null | undefined,
  usernameNormalizado: string | null,
): string[] {
  if (password == null || password.length < LONGITUD_MINIMA_PASSWORD) {
    return [`La contraseña debe tener al menos ${LONGITUD_MINIMA_PASSWORD} caracteres.`];
  }
  const errores: string[] = [];
  if (new TextEncoder().encode(password).length > MAXIMO_BYTES_PASSWORD) {
    errores.push(
      `La contraseña no puede ocupar más de ${MAXIMO_BYTES_PASSWORD} bytes ` +
        '(las letras con tilde y la ñ ocupan 2).',
    );
  }
  if (usernameNormalizado !== null && password.trim().toLowerCase() === usernameNormalizado) {
    errores.push('La contraseña no puede ser igual al username.');
  }
  return errores;
}

/** Validador del username (formato del backend). */
export function usernameValido(control: AbstractControl): ValidationErrors | null {
  const error = errorUsername(control.value as string | null);
  return error ? { username: error } : null;
}

/**
 * Validador de la contraseña. `username` da el username con el que se compara
 * (en el alta cambia mientras se escribe: hay que revalidar al cambiarlo).
 */
export function passwordValida(username: () => string | null): ValidatorFn {
  return (control) => {
    const [primero] = erroresPassword(
      control.value as string | null,
      normalizarUsername(username()),
    );
    return primero ? { password: primero } : null;
  };
}

/** La repetición de la contraseña coincide (comprobación solo del frontend). */
export function repiteA(original: () => string): ValidatorFn {
  return (control) =>
    control.value === original() ? null : { repeticion: 'Las contraseñas no coinciden.' };
}
