/** `PacienteResponse` del backend. */
export interface Paciente {
  idPaciente: string;
  nombre: string;
  edad: number;
  habitacion: number;
}

/** Alta y edición (`PacienteRequest`). */
export interface PacienteRequest {
  nombre: string;
  edad: number;
  habitacion: number;
}
