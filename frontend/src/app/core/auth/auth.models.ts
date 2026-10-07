import { Rol } from '../roles';

export interface LoginRequest {
  username: string;
  password: string;
}

/** `POST /api/auth/login`. */
export interface LoginResponse {
  token: string;
  tipo: string;
  expiraEnSegundos: number;
  rol: Rol;
}

/**
 * `UsuarioResponse` (`GET /api/auth/me` y `/api/usuarios`). `idTrabajador` es
 * `null` para el administrador. Nunca trae el hash de la contraseña.
 */
export interface Usuario {
  idUsuario: string;
  username: string;
  rol: Rol;
  idTrabajador: string | null;
  activo: boolean;
}
