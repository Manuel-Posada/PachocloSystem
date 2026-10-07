import { FormControl, Validators } from '@angular/forms';
import {
  contieneLetra,
  edad,
  enteroEntre,
  especialidad,
  habitacion,
  longitudMaxima,
  longitudMinima,
  numeroEntre,
  nombrePersona,
  obligatorio,
  primerError,
  textoDescriptivo,
  unoDe,
} from './validadores';

const validar = (validador: (c: FormControl) => unknown, valor: unknown) =>
  validador(new FormControl(valor));

describe('obligatorio', () => {
  const validador = obligatorio('Falta.');

  it.each([null, undefined, '', '   '])('rechaza %j', (valor) => {
    expect(validar(validador, valor)).toEqual({ obligatorio: 'Falta.' });
  });

  it.each(['admin', ' a ', 0])('acepta %j', (valor) => {
    expect(validar(validador, valor)).toBeNull();
  });
});

describe('nombrePersona', () => {
  const validador = nombrePersona();

  it.each(['Ana', 'José Núñez', 'María de los Ángeles', '  Ana Ruiz  '])('acepta %j', (valor) => {
    expect(validar(validador, valor)).toBeNull();
  });

  it.each(['Al', 'Ana3', 'Ana-Ruiz', 'A'.repeat(61), ' 1Ana'])('rechaza %j', (valor) => {
    expect(validar(validador, valor)).toEqual({
      patron: 'El nombre debe tener letras y espacios (3 a 60 caracteres).',
    });
  });

  it('deja el vacío a obligatorio', () => {
    expect(validar(validador, '')).toBeNull();
  });

  it('admite un mensaje propio', () => {
    expect(validar(nombrePersona('Otro.'), '1')).toEqual({ patron: 'Otro.' });
  });
});

describe('enteroEntre', () => {
  const validador = enteroEntre(1, 10, 'Fuera.');

  it.each([1, 10, 5, '7'])('acepta %j', (valor) => {
    expect(validar(validador, valor)).toBeNull();
  });

  it.each([0, 11, 2.5, 'abc'])('rechaza %j', (valor) => {
    expect(validar(validador, valor)).toEqual({ enteroEntre: 'Fuera.' });
  });

  it.each([null, ''])('deja %j a obligatorio', (valor) => {
    expect(validar(validador, valor)).toBeNull();
  });
});

describe('edad y habitacion', () => {
  it('edad: 0 a 120', () => {
    expect(validar(edad, 0)).toBeNull();
    expect(validar(edad, 120)).toBeNull();
    expect(validar(edad, 121)).toEqual({
      enteroEntre: 'La edad debe ser un número entero entre 0 y 120.',
    });
  });

  it('habitacion: 1 a 999', () => {
    expect(validar(habitacion, 1)).toBeNull();
    expect(validar(habitacion, 999)).toBeNull();
    expect(validar(habitacion, 0)).toEqual({
      enteroEntre: 'El número de habitación debe ser un entero entre 1 y 999.',
    });
  });
});

describe('primerError', () => {
  it('devuelve el mensaje del primer validador que falla', () => {
    const control = new FormControl('', [obligatorio('Primero.'), nombrePersona()]);

    expect(primerError(control)).toBe('Primero.');
  });

  it('devuelve null si es válido', () => {
    expect(primerError(new FormControl('Ana', obligatorio('x')))).toBeNull();
  });

  it('usa un mensaje genérico para validadores sin mensaje', () => {
    expect(primerError(new FormControl('', Validators.required))).toBe('El valor no es válido.');
  });
});

describe('textoDescriptivo', () => {
  const validador = textoDescriptivo(3, 'Corto.');

  it.each(['abc', '  Cardiología ', 'A12', 'Nivel 2'])('acepta %j', (valor) => {
    expect(validar(validador, valor)).toBeNull();
  });

  it.each(['ab', '  ab  ', '123', '---'])('rechaza %j', (valor) => {
    expect(validar(validador, valor)).toEqual({ textoDescriptivo: 'Corto.' });
  });

  it('deja el vacío a obligatorio', () => {
    expect(validar(validador, '   ')).toBeNull();
  });

  it('especialidad usa el mensaje del backend', () => {
    expect(validar(especialidad, '12')).toEqual({
      textoDescriptivo: 'La especialidad debe ser un texto descriptivo (mínimo 3 caracteres).',
    });
  });
});

describe('longitudMaxima', () => {
  const validador = longitudMaxima(5, 'Largo.');

  it.each(['', 'abcde', '  abcde  ', null])('acepta %j', (valor) => {
    expect(validar(validador, valor)).toBeNull();
  });

  it('rechaza más caracteres de la cuenta', () => {
    expect(validar(validador, 'abcdef')).toEqual({ longitudMaxima: 'Largo.' });
  });
});

describe('unoDe', () => {
  const validador = unoDe(['TABLETA', 'JARABE'], 'No válido.');

  it.each(['TABLETA', 'JARABE', null, ''])('acepta %j', (valor) => {
    expect(validar(validador, valor)).toBeNull();
  });

  it.each(['tableta', 'PASTILLA'])('rechaza %j', (valor) => {
    expect(validar(validador, valor)).toEqual({ unoDe: 'No válido.' });
  });
});

describe('longitudMinima', () => {
  const validador = longitudMinima(5, 'Corto.');

  it.each(['abcde', '  abcde  ', '', null])('acepta %j', (valor) => {
    expect(validar(validador, valor)).toBeNull();
  });

  it.each(['abcd', '  ab  '])('rechaza %j', (valor) => {
    expect(validar(validador, valor)).toEqual({ longitudMinima: 'Corto.' });
  });
});

describe('contieneLetra', () => {
  const validador = contieneLetra('Sin letras.');

  it.each(['abc', '120 mg', 'Ñ', '', null])('acepta %j', (valor) => {
    expect(validar(validador, valor)).toBeNull();
  });

  it.each(['12345', '--- 1'])('rechaza %j', (valor) => {
    expect(validar(validador, valor)).toEqual({ contieneLetra: 'Sin letras.' });
  });
});

describe('numeroEntre', () => {
  const validador = numeroEntre(30, 45, 'Fuera.');

  it.each([30, 36.6, 45, '37.2', null, ''])('acepta %j', (valor) => {
    expect(validar(validador, valor)).toBeNull();
  });

  it.each([29.9, 45.1, 'abc'])('rechaza %j', (valor) => {
    expect(validar(validador, valor)).toEqual({ numeroEntre: 'Fuera.' });
  });
});
