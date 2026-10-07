import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { Observable, of, throwError } from 'rxjs';
import { Permiso, PermisosService, tienePermiso } from '../../../core/permisos';
import { Rol } from '../../../core/roles';
import { ApiError } from '../../../core/http/api-error';
import { NotificacionService } from '../../../core/notificacion.service';
import { ConfirmacionDialogoComponent } from '../../../shared/confirmacion-dialogo/confirmacion-dialogo.component';
import { ESPERA_BUSQUEDA_MS } from '../../../shared/listas';
import { TrabajadorDialogoComponent } from '../trabajador-dialogo/trabajador-dialogo.component';
import { Trabajador } from '../trabajador.models';
import { TrabajadorService } from '../trabajador.service';
import { TrabajadoresListaComponent } from './trabajadores-lista.component';

describe('TrabajadoresListaComponent', () => {
  /** Rol de los permisos simulados; ADMIN (todo) salvo en los tests de rol. */
  let rol: Rol = 'ADMIN';
  const doctora: Trabajador = {
    idTrabajador: 'DOC-0001',
    nombreCompleto: 'Ana Ruiz',
    rol: 'Doctor',
    especialidad: 'Cardiología',
    nivelExperiencia: null,
  };
  const enfermero: Trabajador = {
    idTrabajador: 'ENF-0001',
    nombreCompleto: 'Luis Gil',
    rol: 'Enfermero',
    especialidad: null,
    nivelExperiencia: 'PRINCIPIANTE',
  };

  const servicio = {
    listar: vi.fn<(q?: string) => Observable<Trabajador[]>>(),
    eliminar: vi.fn<(id: string) => Observable<void>>(),
  };
  const dialogo = { open: vi.fn() };
  const notificaciones = { exito: vi.fn(), error: vi.fn() };
  const alCerrarDialogo = (resultado: unknown) =>
    dialogo.open.mockReturnValueOnce({ afterClosed: () => of(resultado) });

  async function renderizar() {
    TestBed.configureTestingModule({
      imports: [TrabajadoresListaComponent],
      providers: [
        { provide: TrabajadorService, useValue: servicio },
        { provide: MatDialog, useValue: dialogo },
        { provide: NotificacionService, useValue: notificaciones },
        { provide: PermisosService, useValue: { puede: (p: Permiso) => tienePermiso(rol, p) } },
      ],
    });
    const fixture = TestBed.createComponent(TrabajadoresListaComponent);
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;
    return {
      fixture,
      elemento,
      filas: () =>
        Array.from(elemento.querySelectorAll('tr[mat-row]')).map((f) =>
          Array.from(f.querySelectorAll('td'))
            .slice(0, 4)
            .map((c) => c.textContent?.trim()),
        ),
      boton: (etiqueta: string) =>
        elemento.querySelector<HTMLButtonElement>(`button[aria-label="${etiqueta}"]`)!,
      textoEstado: () => elemento.querySelector('.lista-estado')?.textContent?.trim(),
    };
  }

  beforeEach(() => {
    vi.resetAllMocks();
    rol = 'ADMIN';
    servicio.listar.mockReturnValue(of([doctora, enfermero]));
  });

  it('muestra rol y especialidad o nivel según el trabajador', async () => {
    const { filas } = await renderizar();

    expect(filas()).toEqual([
      ['DOC-0001', 'Ana Ruiz', 'Doctor', 'Cardiología'],
      ['ENF-0001', 'Luis Gil', 'Enfermero', 'Nivel principiante'],
    ]);
  });

  it('el doctor solo consulta: sin alta ni columna de acciones', async () => {
    rol = 'DOCTOR';
    const { elemento, filas } = await renderizar();

    expect(filas()).toHaveLength(2);
    expect(elemento.querySelector('.lista-encabezado button')).toBeNull();
    expect(elemento.querySelector('.celda-acciones')).toBeNull();
  });

  it('indica que no hay trabajadores', async () => {
    servicio.listar.mockReturnValue(of([]));

    const { textoEstado } = await renderizar();

    expect(textoEstado()).toBe('Todavía no hay trabajadores registrados.');
  });

  it('busca en el servidor tras dejar de escribir', async () => {
    const { fixture, elemento, textoEstado } = await renderizar();
    servicio.listar.mockReturnValue(of([]));

    const input = elemento.querySelector<HTMLInputElement>('input[type="search"]')!;
    input.value = 'zz';
    input.dispatchEvent(new Event('input'));
    await new Promise((r) => setTimeout(r, ESPERA_BUSQUEDA_MS + 50));
    await fixture.whenStable();

    expect(servicio.listar).toHaveBeenLastCalledWith('zz');
    expect(textoEstado()).toBe('Ningún trabajador coincide con «zz».');
  });

  it('muestra el error de carga con opción de reintentar', async () => {
    servicio.listar.mockReturnValueOnce(
      throwError(() => new ApiError(0, ['No se pudo conectar con el servidor.'])),
    );
    const { fixture, elemento, filas } = await renderizar();

    elemento.querySelector<HTMLButtonElement>('[role="alert"] button')!.click();
    await fixture.whenStable();

    expect(servicio.listar).toHaveBeenCalledTimes(2);
    expect(filas()).toHaveLength(2);
  });

  it('al eliminar avisa de que se desactiva el usuario asociado', async () => {
    servicio.eliminar.mockReturnValue(of(undefined));
    alCerrarDialogo(true);
    const { fixture, boton } = await renderizar();

    boton('Eliminar a Ana Ruiz').click();
    await fixture.whenStable();

    const [componente, config] = dialogo.open.mock.calls[0];
    expect(componente).toBe(ConfirmacionDialogoComponent);
    expect(config.data.mensaje).toContain('Ana Ruiz (DOC-0001)');
    expect(config.data.mensaje).toContain('usuario de acceso, también quedará desactivado');
    expect(servicio.eliminar).toHaveBeenCalledWith('DOC-0001');
    expect(notificaciones.exito).toHaveBeenCalledWith('Trabajador DOC-0001 eliminado.');
    expect(servicio.listar).toHaveBeenCalledTimes(2);
  });

  it('no elimina si se cancela la confirmación', async () => {
    alCerrarDialogo(false);
    const { fixture, boton } = await renderizar();

    boton('Eliminar a Ana Ruiz').click();
    await fixture.whenStable();

    expect(servicio.eliminar).not.toHaveBeenCalled();
  });

  it('edita en el diálogo, avisa y recarga', async () => {
    alCerrarDialogo(enfermero);
    const { fixture, boton } = await renderizar();

    boton('Editar a Luis Gil').click();
    await fixture.whenStable();

    expect(dialogo.open).toHaveBeenCalledWith(
      TrabajadorDialogoComponent,
      expect.objectContaining({ data: { trabajador: enfermero } }),
    );
    expect(notificaciones.exito).toHaveBeenCalledWith('Trabajador ENF-0001 actualizado.');
    expect(servicio.listar).toHaveBeenCalledTimes(2);
  });
});
