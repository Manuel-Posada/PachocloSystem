import { formatearFecha, formatearFechaHora } from './fechas';

describe('formatearFecha', () => {
  it('da la vuelta a la fecha sin pasar por zonas horarias', () => {
    expect(formatearFecha('2027-01-31')).toBe('31/01/2027');
    // Con Date y UTC-5, el 1 de enero se mostraría como 31/12 del año anterior.
    expect(formatearFecha('2027-01-01')).toBe('01/01/2027');
  });

  it('devuelve tal cual un texto que no es una fecha ISO', () => {
    expect(formatearFecha('31/01/2027')).toBe('31/01/2027');
  });
});

describe('formatearFechaHora', () => {
  it.each([
    ['2026-10-06T20:21:00.915452', '06/10/2026 20:21'],
    ['2026-10-06T00:05:00', '06/10/2026 00:05'],
    ['2026-12-31T23:59', '31/12/2026 23:59'],
  ])('%s → %s, sin convertir de zona', (entrada, salida) => {
    expect(formatearFechaHora(entrada)).toBe(salida);
  });

  it('devuelve tal cual un texto que no es una fecha y hora ISO', () => {
    expect(formatearFechaHora('ayer')).toBe('ayer');
  });
});
