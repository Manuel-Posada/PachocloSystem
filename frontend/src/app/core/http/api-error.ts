import { HttpErrorResponse } from '@angular/common/http';

/** Cuerpo de error uniforme de la API (`ErrorResponse` del backend). */
export interface ErrorResponse {
  status: number;
  error: string;
  mensajes: string[];
}

const MENSAJE_INESPERADO = 'Se produjo un error inesperado. Vuelva a intentarlo más tarde.';
const MENSAJE_SIN_CONEXION =
  'No se pudo conectar con el servidor. Compruebe su conexión y vuelva a intentarlo.';

/**
 * Error de una petición a la API ya normalizado: el estado HTTP y los mensajes
 * listos para mostrar. Es lo único que reciben los componentes al fallar una
 * petición (lo produce `errorInterceptor`).
 */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly mensajes: readonly string[],
  ) {
    super(mensajes.join(' '));
    this.name = 'ApiError';
  }

  /**
   * El backend responde siempre con el `ErrorResponse` uniforme (incluso en
   * 502/503): sus mensajes se conservan tal cual. Sin ese cuerpo (estado 0,
   * o un 502/504 vacío del proxy de desarrollo con el backend apagado) la
   * respuesta no viene de la API, así que no se pudo conectar con ella.
   */
  static desde(respuesta: HttpErrorResponse): ApiError {
    if (esErrorResponse(respuesta.error) && respuesta.error.mensajes.length > 0) {
      return new ApiError(respuesta.status, respuesta.error.mensajes);
    }
    return new ApiError(respuesta.status, [MENSAJE_SIN_CONEXION]);
  }
}

/** Mensajes para mostrar de cualquier error recibido en un `subscribe`. */
export function mensajesDeError(error: unknown): readonly string[] {
  return error instanceof ApiError ? error.mensajes : [MENSAJE_INESPERADO];
}

function esErrorResponse(cuerpo: unknown): cuerpo is ErrorResponse {
  const mensajes = (cuerpo as Partial<ErrorResponse> | null)?.mensajes;
  return Array.isArray(mensajes) && mensajes.every((m) => typeof m === 'string');
}
