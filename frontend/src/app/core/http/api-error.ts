import { HttpErrorResponse } from '@angular/common/http';

/** Cuerpo de error uniforme de la API (`ErrorResponse` del backend). */
export interface ErrorResponse {
  status: number;
  error: string;
  mensajes: string[];
}

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

  static desde(respuesta: HttpErrorResponse): ApiError {
    if (respuesta.status === 0) {
      return new ApiError(0, [MENSAJE_SIN_CONEXION]);
    }
    if (esErrorResponse(respuesta.error) && respuesta.error.mensajes.length > 0) {
      return new ApiError(respuesta.status, respuesta.error.mensajes);
    }
    return new ApiError(respuesta.status, [mensajeGenerico(respuesta.status)]);
  }
}

/** Mensajes para mostrar de cualquier error recibido en un `subscribe`. */
export function mensajesDeError(error: unknown): readonly string[] {
  return error instanceof ApiError ? error.mensajes : [mensajeGenerico(undefined)];
}

function esErrorResponse(cuerpo: unknown): cuerpo is ErrorResponse {
  const mensajes = (cuerpo as Partial<ErrorResponse> | null)?.mensajes;
  return Array.isArray(mensajes) && mensajes.every((m) => typeof m === 'string');
}

function mensajeGenerico(status: number | undefined): string {
  const codigo = status === undefined ? '' : ` (código ${status})`;
  return `Se produjo un error inesperado${codigo}. Vuelva a intentarlo más tarde.`;
}
