import { etiquetaRol, rolDeTrabajador, rolDeUsuario } from './roles';

describe('roles', () => {
  it('convierte el rol de trabajador al de usuario', () => {
    expect(rolDeUsuario('Doctor')).toBe('DOCTOR');
    expect(rolDeUsuario('Enfermero')).toBe('ENFERMERO');
  });

  it('convierte el rol de usuario al de trabajador', () => {
    expect(rolDeTrabajador('DOCTOR')).toBe('Doctor');
    expect(rolDeTrabajador('ENFERMERO')).toBe('Enfermero');
    expect(rolDeTrabajador('ADMIN')).toBeNull();
  });

  it.each([
    ['ADMIN', 'Administrador'],
    ['DOCTOR', 'Doctor'],
    ['Doctor', 'Doctor'],
    ['ENFERMERO', 'Enfermero'],
    ['Enfermero', 'Enfermero'],
  ] as const)('etiqueta %s como %s', (rol, etiqueta) => {
    expect(etiquetaRol(rol)).toBe(etiqueta);
  });
});
