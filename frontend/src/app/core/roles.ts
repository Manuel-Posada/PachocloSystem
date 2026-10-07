/*
 * Único sitio donde se relacionan los dos formatos de rol del backend:
 *  - Usuario (`/api/auth/*`, claim del JWT): 'ADMIN' | 'DOCTOR' | 'ENFERMERO'.
 *  - Trabajador (`/api/trabajadores`): 'Doctor' | 'Enfermero'.
 * Ningún otro archivo debe comparar ni convertir estos textos a mano.
 */

/** Rol de un usuario de acceso. */
export type Rol = 'ADMIN' | 'DOCTOR' | 'ENFERMERO';

/** Rol de un trabajador del hospital (no hay trabajadores administradores). */
export type RolTrabajador = 'Doctor' | 'Enfermero';

export const ROLES_TRABAJADOR: readonly RolTrabajador[] = ['Doctor', 'Enfermero'];

const ROL_USUARIO_DE_TRABAJADOR: Readonly<Record<RolTrabajador, Rol>> = {
  Doctor: 'DOCTOR',
  Enfermero: 'ENFERMERO',
};

const ETIQUETAS: Readonly<Record<Rol, string>> = {
  ADMIN: 'Administrador',
  DOCTOR: 'Doctor',
  ENFERMERO: 'Enfermero',
};

/** 'Doctor' → 'DOCTOR'. */
export function rolDeUsuario(rol: RolTrabajador): Rol {
  return ROL_USUARIO_DE_TRABAJADOR[rol];
}

/** 'DOCTOR' → 'Doctor'; `null` para 'ADMIN', que no es un trabajador. */
export function rolDeTrabajador(rol: Rol): RolTrabajador | null {
  return ROLES_TRABAJADOR.find((r) => ROL_USUARIO_DE_TRABAJADOR[r] === rol) ?? null;
}

/** Texto para mostrar, acepte el formato que acepte. */
export function etiquetaRol(rol: Rol | RolTrabajador): string {
  return rol in ETIQUETAS ? ETIQUETAS[rol as Rol] : ETIQUETAS[rolDeUsuario(rol as RolTrabajador)];
}
