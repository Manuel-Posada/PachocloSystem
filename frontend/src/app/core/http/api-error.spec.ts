import { HttpErrorResponse } from '@angular/common/http';
import { ApiError, mensajesDeError } from './api-error';

describe('ApiError.desde', () => {
  it('usa los mensajes del ErrorResponse del backend', () => {
    const error = ApiError.desde(
      new HttpErrorResponse({
        status: 400,
        error: { status: 400, error: 'Bad Request', mensajes: ['A.', 'B.'] },
      }),
    );

    expect(error.status).toBe(400);
    expect(error.mensajes).toEqual(['A.', 'B.']);
  });

  it.each([502, 503])('conserva los mensajes del backend también en un %i', (status) => {
    const error = ApiError.desde(
      new HttpErrorResponse({
        status,
        error: {
          status,
          error: 'x',
          mensajes: ['El servicio de medicamentos no está disponible.'],
        },
      }),
    );

    expect(error.status).toBe(status);
    expect(error.mensajes).toEqual(['El servicio de medicamentos no está disponible.']);
  });

  it.each([
    ['estado 0 (sin red)', 0, null],
    ['502 vacío del proxy con el backend apagado', 502, null],
    ['504 con HTML de un intermediario', 504, '<html>Gateway Timeout</html>'],
    ['cuerpo con mensajes vacíos', 500, { status: 500, error: 'x', mensajes: [] }],
  ])('sin el cuerpo uniforme (%s) indica que no se pudo conectar', (_caso, status, cuerpo) => {
    const error = ApiError.desde(new HttpErrorResponse({ status, error: cuerpo }));

    expect(error.status).toBe(status);
    expect(error.mensajes).toEqual([
      'No se pudo conectar con el servidor. Compruebe su conexión y vuelva a intentarlo.',
    ]);
  });
});

describe('mensajesDeError', () => {
  it('devuelve los mensajes de un ApiError', () => {
    expect(mensajesDeError(new ApiError(409, ['Duplicado.']))).toEqual(['Duplicado.']);
  });

  it('devuelve un mensaje genérico para cualquier otro error', () => {
    expect(mensajesDeError(new Error('x'))).toEqual([
      'Se produjo un error inesperado. Vuelva a intentarlo más tarde.',
    ]);
  });
});
