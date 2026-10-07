import { Rol } from '../../core/roles';

/**
 * Alta de un usuario (`POST /api/usuarios`). `idTrabajador` es obligatorio para
 * DOCTOR y ENFERMERO y debe ser `null` para ADMIN.
 */
export interface UsuarioRequest {
  username: string;
  password: string;
  rol: Rol;
  idTrabajador: string | null;
}

export const ROLES_USUARIO: readonly Rol[] = ['ADMIN', 'DOCTOR', 'ENFERMERO'];
