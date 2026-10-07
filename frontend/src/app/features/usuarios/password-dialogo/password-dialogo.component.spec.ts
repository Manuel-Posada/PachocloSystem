import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable, of, throwError } from 'rxjs';
import { Usuario } from '../../../core/auth/auth.models';
import { AuthService } from '../../../core/auth/auth.service';
import { ApiError } from '../../../core/http/api-error';
import { UsuarioService } from '../usuario.service';
import { PasswordDialogoComponent } from './password-dialogo.component';

describe('PasswordDialogoComponent', () => {
  const admin: Usuario = {
    idUsuario: 'USR-0001',
    username: 'admin.principal',
    rol: 'ADMIN',
    idTrabajador: null,
    activo: true,
  };
  const eva: Usuario = {
    idUsuario: 'USR-0002',
    username: 'eva.mora',
    rol: 'DOCTOR',
    idTrabajador: 'DOC-0001',
    activo: true,
  };
  const usuarios = { restablecerPassword: vi.fn<() => Observable<void>>() };
  const auth = { usuario: signal<Usuario | null>(admin), cerrarSesion: vi.fn() };
  const dialogo = { close: vi.fn() };

  async function renderizar(usuario: Usuario) {
    TestBed.configureTestingModule({
      imports: [PasswordDialogoComponent],
      providers: [
        { provide: UsuarioService, useValue: usuarios },
        { provide: AuthService, useValue: auth },
        { provide: MatDialogRef, useValue: dialogo },
        { provide: MAT_DIALOG_DATA, useValue: { usuario } },
      ],
    });
    const fixture = TestBed.createComponent(PasswordDialogoComponent);
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;
    return {
      elemento,
      restablecer: async (password: string, repeticion = password) => {
        for (const [nombre, valor] of [
          ['password', password],
          ['repeticion', repeticion],
        ]) {
          const input = elemento.querySelector<HTMLInputElement>(`[formControlName="${nombre}"]`)!;
          input.value = valor;
          input.dispatchEvent(new Event('input'));
        }
        elemento.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
        await fixture.whenStable();
      },
      errores: () =>
        Array.from(elemento.querySelectorAll('mat-error')).map((e) => e.textContent?.trim()),
    };
  }

  beforeEach(() => vi.resetAllMocks());

  it('restablece la de otro usuario sin cerrar la sesión del admin', async () => {
    usuarios.restablecerPassword.mockReturnValue(of(undefined));
    const { elemento, restablecer } = await renderizar(eva);

    expect(elemento.querySelector('.aviso')).toBeNull();
    await restablecer('nueva-clave-eva-1');

    expect(usuarios.restablecerPassword).toHaveBeenCalledWith('USR-0002', 'nueva-clave-eva-1');
    expect(dialogo.close).toHaveBeenCalledWith(true);
    expect(auth.cerrarSesion).not.toHaveBeenCalled();
  });

  it('si es la propia, avisa antes y después cierra la sesión explicando el motivo', async () => {
    usuarios.restablecerPassword.mockReturnValue(of(undefined));
    const { elemento, restablecer } = await renderizar(admin);

    expect(elemento.querySelector('.aviso')?.textContent).toContain(
      'su sesión actual dejará de valer',
    );
    await restablecer('nueva-clave-admin-1');

    expect(dialogo.close).toHaveBeenCalledWith(true);
    expect(auth.cerrarSesion).toHaveBeenCalledWith({ motivo: 'password-cambiada' });
  });

  it('aplica la política en local: repetida igual y hasta 72 bytes', async () => {
    const { restablecer, errores } = await renderizar(eva);

    await restablecer('clave-valida-1', 'otra-distinta');
    expect(errores()).toEqual(['Las contraseñas no coinciden.']);

    await restablecer('ñ'.repeat(37));
    expect(errores()).toEqual([
      'La contraseña no puede ocupar más de 72 bytes (las letras con tilde y la ñ ocupan 2).',
    ]);
    expect(usuarios.restablecerPassword).not.toHaveBeenCalled();
  });

  it('muestra dentro el error del servidor y no cierra la sesión', async () => {
    usuarios.restablecerPassword.mockReturnValue(
      throwError(() => new ApiError(404, ['No se encontró el usuario USR-0001.'])),
    );
    const { elemento, restablecer } = await renderizar(admin);

    await restablecer('nueva-clave-admin-1');

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'No se encontró el usuario USR-0001.',
    );
    expect(dialogo.close).not.toHaveBeenCalled();
    expect(auth.cerrarSesion).not.toHaveBeenCalled();
  });
});
