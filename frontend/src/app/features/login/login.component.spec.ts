import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/http/api-error';
import { LoginComponent, destinoSeguro } from './login.component';

describe('LoginComponent', () => {
  const auth = { iniciarSesion: vi.fn<() => Observable<unknown>>() };

  async function renderizar(parametros: Record<string, string> = {}) {
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(parametros) } },
        },
      ],
    });
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(LoginComponent);
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;

    const escribir = (nombre: string, valor: string) => {
      const input = elemento.querySelector<HTMLInputElement>(`input[formControlName="${nombre}"]`)!;
      input.value = valor;
      input.dispatchEvent(new Event('input'));
    };
    const enviar = async () => {
      elemento.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
      await fixture.whenStable();
    };
    return { elemento, router, escribir, enviar };
  }

  beforeEach(() => auth.iniciarSesion.mockReset());

  it('valida los campos obligatorios sin llamar al servidor', async () => {
    const { elemento, escribir, enviar } = await renderizar();

    escribir('username', '   ');
    await enviar();

    expect(auth.iniciarSesion).not.toHaveBeenCalled();
    const errores = Array.from(elemento.querySelectorAll('mat-error')).map((e) =>
      e.textContent?.trim(),
    );
    expect(errores).toEqual(['El usuario es obligatorio.', 'La contraseña es obligatoria.']);
  });

  it('inicia sesión y navega a la ruta pedida', async () => {
    auth.iniciarSesion.mockReturnValue(of({}));
    const { router, escribir, enviar } = await renderizar({ returnUrl: '/trabajadores' });

    escribir('username', 'admin');
    escribir('password', 'secreto');
    await enviar();

    expect(auth.iniciarSesion).toHaveBeenCalledWith({ username: 'admin', password: 'secreto' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/trabajadores');
  });

  it('muestra los mensajes del servidor si falla', async () => {
    auth.iniciarSesion.mockReturnValue(
      throwError(() => new ApiError(401, ['Credenciales inválidas.'])),
    );
    const { elemento, router, escribir, enviar } = await renderizar();

    escribir('username', 'admin');
    escribir('password', 'mala');
    await enviar();

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'Credenciales inválidas.',
    );
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(elemento.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(
      false,
    );
  });

  it('avisa si se llegó por sesión expirada', async () => {
    const { elemento } = await renderizar({ motivo: 'expirada' });

    expect(elemento.querySelector('[role="status"]')?.textContent).toContain(
      'Su sesión expiró o ya no es válida.',
    );
  });

  it('explica el cierre si el usuario cambió su propia contraseña', async () => {
    const { elemento } = await renderizar({ motivo: 'password-cambiada' });

    expect(elemento.querySelector('[role="status"]')?.textContent).toContain(
      'Su contraseña ha cambiado y la sesión anterior ya no es válida.',
    );
  });

  it('ignora un motivo desconocido', async () => {
    const { elemento } = await renderizar({ motivo: 'inventado' });

    expect(elemento.querySelector('[role="status"]')).toBeNull();
  });
});

describe('destinoSeguro', () => {
  it.each([
    ['/pacientes?q=ana', '/pacientes?q=ana'],
    [null, '/'],
    ['', '/'],
    ['https://malo.example', '/'],
    ['//malo.example', '/'],
    ['/login?returnUrl=/x', '/'],
  ])('%j → %j', (entrada, esperado) => {
    expect(destinoSeguro(entrada)).toBe(esperado);
  });
});
