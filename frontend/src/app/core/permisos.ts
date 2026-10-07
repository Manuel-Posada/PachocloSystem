import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth/auth.service';
import { Rol } from './roles';

/*
 * Único sitio con los permisos por rol del frontend. Es copia de la tabla
 * "Permisos por rol" del README del backend (que la comprueba en
 * AutorizacionPorRolTest): si cambia allí, cambia aquí.
 *
 * Solo sirve para ocultar o desactivar lo que el rol no puede hacer; quien
 * decide es el backend, y un 403 se sigue mostrando como cualquier error.
 * Leer pacientes, historial y medicamentos lo pueden todos los roles, así que
 * no tiene permiso propio.
 */

export type Permiso =
  /** Registrar y editar pacientes. */
  | 'pacientes.escribir'
  | 'pacientes.habitacion'
  /** Dar de baja pacientes (baja lógica: se conserva su historial). */
  | 'pacientes.eliminar'
  /** Crear registros de historial (además hace falta un trabajador vinculado). */
  | 'historial.crear'
  /** Crear registros de tipo DIAGNOSTICO. */
  | 'historial.diagnosticar'
  | 'trabajadores.leer'
  /** Registrar, editar y eliminar trabajadores. */
  | 'trabajadores.escribir'
  /** Alta, edición, borrado y entradas de medicamentos. */
  | 'medicamentos.escribir'
  /** Salidas directas de stock (el descuento desde MEDICACION va con historial.crear). */
  | 'medicamentos.salidas'
  | 'usuarios.gestionar';

const PERMISOS: Readonly<Record<Rol, ReadonlySet<Permiso>>> = {
  ADMIN: new Set<Permiso>([
    'pacientes.escribir',
    'pacientes.habitacion',
    'pacientes.eliminar',
    'trabajadores.leer',
    'trabajadores.escribir',
    'medicamentos.escribir',
    'medicamentos.salidas',
    'usuarios.gestionar',
  ]),
  DOCTOR: new Set<Permiso>([
    'pacientes.escribir',
    'pacientes.habitacion',
    'historial.crear',
    'historial.diagnosticar',
    'trabajadores.leer',
  ]),
  ENFERMERO: new Set<Permiso>(['pacientes.habitacion', 'historial.crear', 'medicamentos.salidas']),
};

/** ¿Puede ese rol? Sin rol (usuario aún sin cargar), nada. */
export function tienePermiso(rol: Rol | null | undefined, permiso: Permiso): boolean {
  return rol ? PERMISOS[rol].has(permiso) : false;
}

/** Permisos del usuario autenticado. Lee un signal: en plantillas se actualiza solo. */
@Injectable({ providedIn: 'root' })
export class PermisosService {
  private readonly auth = inject(AuthService);

  puede(permiso: Permiso): boolean {
    return tienePermiso(this.auth.usuario()?.rol, permiso);
  }
}
