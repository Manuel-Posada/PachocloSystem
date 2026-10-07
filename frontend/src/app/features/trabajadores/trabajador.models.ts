import { RolTrabajador } from '../../core/roles';

export type NivelExperiencia = 'NOVATO' | 'PRINCIPIANTE' | 'AVANZADO';

export const NIVELES_EXPERIENCIA: readonly NivelExperiencia[] = [
  'NOVATO',
  'PRINCIPIANTE',
  'AVANZADO',
];

export const ETIQUETAS_NIVEL: Readonly<Record<NivelExperiencia, string>> = {
  NOVATO: 'Novato',
  PRINCIPIANTE: 'Principiante',
  AVANZADO: 'Avanzado',
};

/**
 * `TrabajadorResponse` del backend. Ojo: la respuesta trae `nombreCompleto`
 * y la petición `nombre`. Un doctor tiene `especialidad`; un enfermero,
 * `nivelExperiencia`; el otro campo llega a `null`.
 */
export interface Trabajador {
  idTrabajador: string;
  nombreCompleto: string;
  rol: RolTrabajador;
  especialidad: string | null;
  nivelExperiencia: NivelExperiencia | null;
}

/** Alta y edición (`TrabajadorRequest`). El rol no puede cambiar al editar. */
export interface TrabajadorRequest {
  nombre: string;
  rol: RolTrabajador;
  especialidad: string | null;
  nivelExperiencia: NivelExperiencia | null;
}
