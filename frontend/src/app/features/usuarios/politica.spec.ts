import { FormControl } from '@angular/forms';
import {
  errorUsername,
  erroresPassword,
  normalizarUsername,
  passwordValida,
  repiteA,
} from './politica';

describe('política de usuarios (copia del backend)', () => {
  describe('username', () => {
    it.each(['ana.torres', '  Ana.Torres  ', 'abc', 'a_b-c.9', 'x'.repeat(30)])(
      'acepta %j (se valida normalizado)',
      (username) => {
        expect(errorUsername(username)).toBeNull();
      },
    );

    it.each(['ab', 'con espacio', 'ñandú', 'x'.repeat(31), '', null])('rechaza %j', (username) => {
      expect(errorUsername(username)).toBe(
        'El username debe tener entre 3 y 30 caracteres y solo puede contener minúsculas, ' +
          'dígitos, punto, guion bajo o guion.',
      );
    });

    it('normaliza como el backend', () => {
      expect(normalizarUsername('  Ana.Torres ')).toBe('ana.torres');
    });
  });

  describe('contraseña', () => {
    it('si es corta solo informa del mínimo', () => {
      expect(erroresPassword('corta-123', 'ana')).toEqual([
        'La contraseña debe tener al menos 10 caracteres.',
      ]);
      expect(erroresPassword('', null)).toEqual([
        'La contraseña debe tener al menos 10 caracteres.',
      ]);
    });

    it('cuenta bytes UTF-8 para el máximo de 72', () => {
      expect(erroresPassword('ñ'.repeat(36), null)).toEqual([]);
      expect(erroresPassword('ñ'.repeat(37), null)).toEqual([
        'La contraseña no puede ocupar más de 72 bytes (las letras con tilde y la ñ ocupan 2).',
      ]);
    });

    it('no puede ser el username, sin distinguir mayúsculas ni espacios', () => {
      expect(erroresPassword('  Ana.Torres.G ', 'ana.torres.g')).toEqual([
        'La contraseña no puede ser igual al username.',
      ]);
    });

    it('el validador compara con el username actual', () => {
      let username = 'otro.usuario';
      const control = new FormControl(
        'ana.torres.g',
        passwordValida(() => username),
      );
      expect(control.errors).toBeNull();

      username = 'ANA.TORRES.G';
      control.updateValueAndValidity();
      expect(control.errors).toEqual({ password: 'La contraseña no puede ser igual al username.' });
    });
  });

  it('la repetición debe coincidir', () => {
    expect(
      new FormControl(
        'a',
        repiteA(() => 'a'),
      ).errors,
    ).toBeNull();
    expect(
      new FormControl(
        'b',
        repiteA(() => 'a'),
      ).errors,
    ).toEqual({
      repeticion: 'Las contraseñas no coinciden.',
    });
  });
});
