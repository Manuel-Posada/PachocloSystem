export type Rol = 'ADMIN' | 'DOCTOR' | 'ENFERMERO';

export const ETIQUETAS_ROL: Readonly<Record<Rol, string>> = {
  ADMIN: 'Administrador',
  DOCTOR: 'Doctor',
  ENFERMERO: 'Enfermero',
};

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

/** `GET /api/auth/me`. `idTrabajador` es `null` para el administrador. */
export interface Usuario {
  idUsuario: string;
  username: string;
  rol: Rol;
  idTrabajador: string | null;
}
