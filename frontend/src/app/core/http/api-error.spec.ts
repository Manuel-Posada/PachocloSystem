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

  it('traduce el estado 0 a un mensaje de falta de conexión', () => {
    const error = ApiError.desde(new HttpErrorResponse({ status: 0 }));

    expect(error.status).toBe(0);
    expect(error.mensajes[0]).toContain('No se pudo conectar con el servidor');
  });

  it('usa un mensaje genérico si el cuerpo no es un ErrorResponse', () => {
    const error = ApiError.desde(new HttpErrorResponse({ status: 504, error: '<html>' }));

    expect(error.mensajes).toEqual([
      'Se produjo un error inesperado (código 504). Vuelva a intentarlo más tarde.',
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
