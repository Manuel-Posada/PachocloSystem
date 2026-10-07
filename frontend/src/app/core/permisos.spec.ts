import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Usuario } from './auth/auth.models';
import { AuthService } from './auth/auth.service';
import { Permiso, PermisosService, tienePermiso } from './permisos';
import { Rol } from './roles';

describe('permisos', () => {
  /** La tabla del README del backend, fila a fila: [permiso, ADMIN, DOCTOR, ENFERMERO]. */
  const TABLA: [Permiso, boolean, boolean, boolean][] = [
    ['pacientes.escribir', true, true, false],
    ['pacientes.habitacion', true, true, true],
    ['pacientes.eliminar', true, false, false],
    ['historial.crear', false, true, true],
    ['historial.diagnosticar', false, true, false],
    ['trabajadores.leer', true, true, false],
    ['trabajadores.escribir', true, false, false],
    ['medicamentos.escribir', true, false, false],
    ['medicamentos.salidas', true, false, true],
    ['usuarios.gestionar', true, false, false],
  ];

  it.each(TABLA)('%s: ADMIN %s, DOCTOR %s, ENFERMERO %s', (permiso, admin, doctor, enfermero) => {
    expect(tienePermiso('ADMIN', permiso)).toBe(admin);
    expect(tienePermiso('DOCTOR', permiso)).toBe(doctor);
    expect(tienePermiso('ENFERMERO', permiso)).toBe(enfermero);
  });

  it('sin rol no hay ningún permiso', () => {
    expect(tienePermiso(null, 'pacientes.habitacion')).toBe(false);
    expect(tienePermiso(undefined, 'pacientes.habitacion')).toBe(false);
  });

  it('PermisosService sigue al usuario autenticado', () => {
    const usuario = signal<Usuario | null>(null);
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: { usuario } }],
    });
    const permisos = TestBed.inject(PermisosService);
    const con = (rol: Rol): Usuario => ({
      idUsuario: 'USR-0001',
      username: 'x',
      rol,
      idTrabajador: null,
      activo: true,
      debeCambiarPassword: false,
    });

    expect(permisos.puede('usuarios.gestionar')).toBe(false);
    usuario.set(con('ADMIN'));
    expect(permisos.puede('usuarios.gestionar')).toBe(true);
    usuario.set(con('ENFERMERO'));
    expect(permisos.puede('usuarios.gestionar')).toBe(false);
  });
});
