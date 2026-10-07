import { formatearFecha } from './fechas';

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
