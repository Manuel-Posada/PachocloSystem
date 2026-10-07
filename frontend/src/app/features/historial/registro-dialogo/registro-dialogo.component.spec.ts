import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable, Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/http/api-error';
import { PermisosService } from '../../../core/permisos';
import { Medicamento } from '../../medicamentos/medicamento.models';
import { MedicamentoService } from '../../medicamentos/medicamento.service';
import { Registro, RegistroRequest } from '../historial.models';
import { HistorialService } from '../historial.service';
import { DatosRegistroDialogo, RegistroDialogoComponent } from './registro-dialogo.component';

/** Si el rol simulado puede crear diagnósticos (el enfermero no). */
let diagnostica = true;

type Uuid = ReturnType<Crypto['randomUUID']>;

/** `crypto.randomUUID` predecible: clave-1, clave-2... en el orden en que se generan. */
function simularClaves(): void {
  let n = 0;
  vi.spyOn(crypto, 'randomUUID').mockImplementation(() => `clave-${++n}` as Uuid);
}

describe('RegistroDialogoComponent', () => {
  const datos: DatosRegistroDialogo = {
    paciente: { idPaciente: 'PAC-0001', nombre: 'Ana Ruiz', edad: 40, habitacion: 12 },
    autor: 'DOC-0001',
  };
  const creado = { idRegistro: 'r1' } as Registro;
  const servicio = {
    crear: vi.fn<(id: string, r: RegistroRequest, clave: string) => Observable<Registro>>(),
  };
  const dialogo = { close: vi.fn() };

  async function renderizar() {
    TestBed.configureTestingModule({
      imports: [RegistroDialogoComponent],
      providers: [
        { provide: HistorialService, useValue: servicio },
        { provide: MedicamentoService, useValue: { listar: vi.fn() } },
        { provide: MatDialogRef, useValue: dialogo },
        { provide: MAT_DIALOG_DATA, useValue: datos },
        { provide: PermisosService, useValue: { puede: () => diagnostica } },
      ],
    });
    const fixture = TestBed.createComponent(RegistroDialogoComponent);
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;
    const estable = () => fixture.whenStable();
    return {
      elemento,
      elegirTipo: async (etiqueta: string) => {
        Array.from(elemento.querySelectorAll('mat-radio-button'))
          .find((r) => r.textContent?.trim() === etiqueta)!
          .querySelector('input')!
          .click();
        await estable();
      },
      rellenar: async (valores: Record<string, string>) => {
        for (const [nombre, valor] of Object.entries(valores)) {
          const campo = elemento.querySelector<HTMLInputElement | HTMLTextAreaElement>(
            `[formControlName="${nombre}"]`,
          )!;
          campo.value = valor;
          campo.dispatchEvent(new Event('input'));
          campo.dispatchEvent(new Event('blur'));
        }
        await estable();
      },
      enviar: async () => {
        elemento.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
        await estable();
      },
      errores: () =>
        Array.from(elemento.querySelectorAll('mat-error')).map((e) => e.textContent?.trim()),
    };
  }

  const signosValidos = {
    temperatura: '36.5',
    frecCardiaca: '80',
    presionSistolica: '120',
    presionDiastolica: '80',
    frecRespiratoria: '16',
    saturacion: '98',
  };

  beforeEach(() => {
    vi.resetAllMocks();
    simularClaves();
    diagnostica = true;
  });

  it('a quien no puede diagnosticar (enfermero) no se le ofrece el tipo DIAGNOSTICO', async () => {
    diagnostica = false;
    const { elemento } = await renderizar();

    const tipos = Array.from(elemento.querySelectorAll('mat-radio-button')).map((r) =>
      r.textContent?.trim(),
    );
    expect(tipos).toEqual(['Evolución', 'Medicación', 'Signos vitales']);
  });

  it('pide elegir el tipo antes de nada', async () => {
    const { enviar, errores } = await renderizar();

    await enviar();

    expect(servicio.crear).not.toHaveBeenCalled();
    expect(errores()).toEqual(['Debe seleccionar un tipo de registro.']);
  });

  it.each([
    ['', 'El contenido no puede estar vacío.'],
    ['Tos', 'El contenido es demasiado corto (mínimo 5 caracteres).'],
    ['12345', 'El contenido debe incluir texto descriptivo, no solo números.'],
  ])('valida el contenido %j como el backend', async (contenido, mensaje) => {
    const { elegirTipo, rellenar, enviar, errores } = await renderizar();

    await elegirTipo('Diagnóstico');
    await rellenar({ contenido });
    await enviar();

    expect(servicio.crear).not.toHaveBeenCalled();
    expect(errores()).toEqual([mensaje]);
  });

  it('crea un diagnóstico con el autor del usuario y el contenido recortado', async () => {
    servicio.crear.mockReturnValue(of(creado));
    const { elegirTipo, rellenar, enviar } = await renderizar();

    await elegirTipo('Diagnóstico');
    await rellenar({ contenido: '  Hipertensión leve  ' });
    await enviar();

    expect(servicio.crear).toHaveBeenCalledWith(
      'PAC-0001',
      {
        tipo: 'DIAGNOSTICO',
        contenido: 'Hipertensión leve',
        signosVitales: null,
        idMedicamento: null,
        cantidad: null,
      },
      'clave-1',
    );
    expect(dialogo.close).toHaveBeenCalledWith(creado);
  });

  it('signos vitales: campos estructurados con los rangos del backend', async () => {
    const { elegirTipo, rellenar, enviar, errores, elemento } = await renderizar();

    await elegirTipo('Signos vitales');
    expect(elemento.querySelector('[formControlName="contenido"]')).toBeNull();
    await rellenar({
      temperatura: '45.5',
      frecCardiaca: '10',
      presionSistolica: '300',
      presionDiastolica: '20',
      frecRespiratoria: '70',
      saturacion: '101',
      observaciones: '123',
    });
    await enviar();

    expect(servicio.crear).not.toHaveBeenCalled();
    expect(errores()).toEqual([
      'Temperatura: debe ser un número entre 30.0 y 45.0 °C.',
      'Frecuencia Cardíaca: debe ser entre 20 y 250 lpm.',
      'Presión Sistólica: debe ser entre 50 y 250 mmHg.',
      'Presión Diastólica: debe ser entre 30 y 150 mmHg.',
      'Frecuencia Respiratoria: debe ser entre 5 y 60 rpm.',
      'Saturación de Oxígeno: debe ser entre 0 y 100%.',
      'Las observaciones no pueden ser solo números.',
    ]);
  });

  it('signos vitales: la diastólica debe ser menor que la sistólica', async () => {
    const { elegirTipo, rellenar, enviar, errores } = await renderizar();

    await elegirTipo('Signos vitales');
    await rellenar({ ...signosValidos, presionSistolica: '80', presionDiastolica: '80' });
    await enviar();

    expect(servicio.crear).not.toHaveBeenCalled();
    expect(errores()).toEqual(['La presión diastólica debe ser menor que la sistólica.']);
  });

  it('crea signos vitales sin contenido y sin observaciones vacías', async () => {
    servicio.crear.mockReturnValue(of(creado));
    const { elegirTipo, rellenar, enviar } = await renderizar();

    await elegirTipo('Diagnóstico');
    await rellenar({ contenido: 'Esto no debe enviarse' });
    await elegirTipo('Signos vitales');
    await rellenar({ ...signosValidos, observaciones: '   ' });
    await enviar();

    expect(servicio.crear).toHaveBeenCalledWith(
      'PAC-0001',
      {
        tipo: 'SIGNOS_VITALES',
        contenido: null,
        signosVitales: {
          temperatura: 36.5,
          frecCardiaca: 80,
          presionSistolica: 120,
          presionDiastolica: 80,
          frecRespiratoria: 16,
          saturacion: 98,
          observaciones: null,
        },
        idMedicamento: null,
        cantidad: null,
      },
      'clave-1',
    );
  });

  it('muestra en el formulario todos los mensajes de un 400 y sigue abierto', async () => {
    servicio.crear.mockReturnValue(
      throwError(() => new ApiError(400, ['Mensaje uno.', 'Mensaje dos.'])),
    );
    const { elegirTipo, rellenar, enviar, elemento } = await renderizar();

    await elegirTipo('Evolución');
    await rellenar({ contenido: 'Mejora progresiva' });
    await enviar();

    const mensajes = Array.from(elemento.querySelectorAll('[role="alert"] p')).map(
      (p) => p.textContent,
    );
    expect(mensajes).toEqual(['Mensaje uno.', 'Mensaje dos.']);
    expect(dialogo.close).not.toHaveBeenCalled();
  });

  describe('clave de idempotencia', () => {
    const evolucion = {
      tipo: 'EVOLUCION',
      contenido: 'Mejora progresiva',
      signosVitales: null,
      idMedicamento: null,
      cantidad: null,
    };

    async function evolucionLista() {
      const vista = await renderizar();
      await vista.elegirTipo('Evolución');
      await vista.rellenar({ contenido: 'Mejora progresiva' });
      return vista;
    }

    it('cada registro nuevo (cada diálogo) tiene su propia clave', async () => {
      // Un 400 no renueva la clave: así se ve solo la clave con la que nace cada diálogo.
      servicio.crear.mockReturnValue(throwError(() => new ApiError(400, ['Mensaje.'])));

      await (await evolucionLista()).enviar();
      TestBed.resetTestingModule();
      await (await evolucionLista()).enviar();

      expect(servicio.crear.mock.calls.map(([, , clave]) => clave)).toEqual(['clave-1', 'clave-2']);
    });

    it('un doble clic mientras se envía no manda una segunda petición', async () => {
      const respuesta = new Subject<Registro>();
      servicio.crear.mockReturnValue(respuesta);
      const { enviar } = await evolucionLista();

      await enviar();
      await enviar();
      respuesta.next(creado);
      respuesta.complete();

      expect(servicio.crear).toHaveBeenCalledTimes(1);
      expect(dialogo.close).toHaveBeenCalledWith(creado);
    });

    it('tras un alta correcta la clave se renueva y no sale otra petición mientras se cierra', async () => {
      servicio.crear.mockReturnValue(of(creado));
      const { enviar } = await evolucionLista();

      await enviar();
      expect(crypto.randomUUID).toHaveBeenCalledTimes(2);

      // Un clic más antes de que se cierre el diálogo no crea un segundo registro con la clave nueva.
      await enviar();
      expect(servicio.crear).toHaveBeenCalledTimes(1);
      expect(servicio.crear).toHaveBeenCalledWith('PAC-0001', evolucion, 'clave-1');
    });

    it('una repetición del backend (201 replay) se trata como un alta normal', async () => {
      // El servicio entrega el mismo Registro con o sin Idempotency-Replayed.
      servicio.crear.mockReturnValue(of(creado));
      const { enviar, elemento } = await evolucionLista();

      await enviar();

      expect(dialogo.close).toHaveBeenCalledWith(creado);
      expect(elemento.querySelector('[role="alert"]')).toBeNull();
    });

    it('un error que no cambió nada (400) deja corregir y conserva la clave', async () => {
      servicio.crear
        .mockReturnValueOnce(throwError(() => new ApiError(400, ['Mensaje.'])))
        .mockReturnValueOnce(of(creado));
      const { enviar, rellenar } = await evolucionLista();

      await enviar();
      await rellenar({ contenido: 'Mejora progresiva, sin fiebre' });
      await enviar();

      expect(servicio.crear).toHaveBeenNthCalledWith(1, 'PAC-0001', evolucion, 'clave-1');
      expect(servicio.crear).toHaveBeenNthCalledWith(
        2,
        'PAC-0001',
        { ...evolucion, contenido: 'Mejora progresiva, sin fiebre' },
        'clave-1',
      );
    });

    it('un 409 de clave ya usada genera una clave nueva y deja revisar y reenviar', async () => {
      servicio.crear
        .mockReturnValueOnce(
          throwError(
            () => new ApiError(409, ['La clave de idempotencia ya se usó para otra petición.']),
          ),
        )
        .mockReturnValueOnce(of(creado));
      const { enviar, rellenar, elemento } = await evolucionLista();

      await enviar();
      const mensajes = Array.from(elemento.querySelectorAll('[role="alert"] p')).map(
        (p) => p.textContent,
      );
      expect(mensajes).toEqual([
        'La clave de idempotencia ya se usó para otra petición.',
        'Revise los datos y vuelva a guardar el registro.',
      ]);
      expect(
        elemento.querySelector<HTMLTextAreaElement>('[formControlName="contenido"]')!.disabled,
      ).toBe(false);

      await rellenar({ contenido: 'Mejora progresiva revisada' });
      await enviar();

      expect(servicio.crear.mock.calls.map(([, , clave]) => clave)).toEqual(['clave-1', 'clave-2']);
      expect(dialogo.close).toHaveBeenCalledWith(creado);
    });
  });
});

describe('RegistroDialogoComponent · medicación', () => {
  const datos: DatosRegistroDialogo = {
    paciente: { idPaciente: 'PAC-0001', nombre: 'Ana Ruiz', edad: 40, habitacion: 12 },
    autor: 'DOC-0001',
  };
  const creado = { idRegistro: 'r1' } as Registro;
  const servicio = {
    crear: vi.fn<(id: string, r: RegistroRequest, clave: string) => Observable<Registro>>(),
  };
  const dialogo = { close: vi.fn() };
  const inventario = { listar: vi.fn<() => Observable<Medicamento[]>>() };
  const dolex = {
    idMedicamento: 'MED-0001',
    nombre: 'Dolex',
    concentracion: '500 mg',
    lote: 'L-1',
    cantidadStock: 15,
    vencido: false,
  } as Medicamento;
  const caducado = { ...dolex, idMedicamento: 'MED-0002', nombre: 'Amoxil', vencido: true };
  const caido = () =>
    throwError(
      () =>
        new ApiError(503, [
          'El servicio de medicamentos no está disponible. Vuelva a intentarlo más tarde.',
        ]),
    );
  const sinDescuento: RegistroRequest = {
    tipo: 'MEDICACION',
    contenido: 'Paracetamol 500 mg vía oral',
    signosVitales: null,
    idMedicamento: null,
    cantidad: null,
  };

  /** Abre el diálogo con MEDICACION elegida y el contenido relleno. */
  async function preparar() {
    TestBed.configureTestingModule({
      imports: [RegistroDialogoComponent],
      providers: [
        { provide: HistorialService, useValue: servicio },
        { provide: MedicamentoService, useValue: inventario },
        { provide: MatDialogRef, useValue: dialogo },
        { provide: MAT_DIALOG_DATA, useValue: datos },
        { provide: PermisosService, useValue: { puede: () => diagnostica } },
      ],
    });
    const fixture = TestBed.createComponent(RegistroDialogoComponent);
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;
    const estable = () => fixture.whenStable();
    const escribir = (selector: string, valor: string, evento = 'input') => {
      const campo = elemento.querySelector<HTMLInputElement>(selector)!;
      campo.value = valor;
      campo.dispatchEvent(new Event(evento));
    };
    Array.from(elemento.querySelectorAll('mat-radio-button'))
      .find((r) => r.textContent?.trim() === 'Medicación')!
      .querySelector('input')!
      .click();
    await estable();
    escribir('[formControlName="contenido"]', 'Paracetamol 500 mg vía oral');
    await estable();
    return {
      elemento,
      casilla: () => elemento.querySelector<HTMLInputElement>('mat-checkbox input')!,
      marcarDescuento: async () => {
        elemento.querySelector<HTMLInputElement>('mat-checkbox input')!.click();
        await estable();
      },
      elegir: async (id: string, cantidad: string) => {
        escribir('[formControlName="idMedicamento"]', id, 'change');
        escribir('[formControlName="cantidad"]', cantidad);
        await estable();
      },
      enviar: async () => {
        elemento.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
        await estable();
      },
      erroresCampos: () =>
        Array.from(elemento.querySelectorAll('mat-error')).map((e) => e.textContent?.trim()),
      alerta: () =>
        Array.from(elemento.querySelectorAll('app-errores-formulario [role="alert"] p')).map(
          (p) => p.textContent,
        ),
    };
  }

  beforeEach(() => {
    vi.resetAllMocks();
    simularClaves();
    inventario.listar.mockReturnValue(of([dolex, caducado]));
  });

  it('sin descuento no envía medicamento ni cantidad y no carga el inventario', async () => {
    servicio.crear.mockReturnValue(of(creado));
    const { enviar } = await preparar();

    await enviar();

    expect(inventario.listar).not.toHaveBeenCalled();
    expect(servicio.crear).toHaveBeenCalledWith('PAC-0001', sinDescuento, 'clave-1');
  });

  it('con descuento exige medicamento y cantidad, y envía los dos', async () => {
    servicio.crear.mockReturnValue(of(creado));
    const { elemento, marcarDescuento, elegir, enviar, erroresCampos } = await preparar();

    await marcarDescuento();
    expect(inventario.listar).toHaveBeenCalledTimes(1);
    const vencido = elemento.querySelector<HTMLOptionElement>('option[value="MED-0002"]');
    expect(vencido?.disabled).toBe(true);

    await enviar();
    expect(servicio.crear).not.toHaveBeenCalled();
    expect(erroresCampos()).toEqual([
      'Seleccione el medicamento administrado.',
      'La cantidad administrada es obligatoria.',
    ]);

    await elegir('MED-0001', '2');
    expect(elemento.querySelector('mat-hint')?.textContent).toContain('Disponible: 15');
    await enviar();

    expect(servicio.crear).toHaveBeenCalledWith(
      'PAC-0001',
      {
        ...sinDescuento,
        idMedicamento: 'MED-0001',
        cantidad: 2,
      },
      'clave-1',
    );
  });

  it('si el inventario no carga, avisa y deja guardar la medicación sin descuento', async () => {
    inventario.listar.mockReturnValueOnce(caido()).mockReturnValueOnce(of([dolex]));
    servicio.crear.mockReturnValue(of(creado));
    const { elemento, casilla, marcarDescuento, enviar } = await preparar();

    await marcarDescuento();

    const aviso = elemento.querySelector('.aviso-inventario')!;
    expect(aviso.textContent).toContain('El servicio de medicamentos no está disponible.');
    expect(aviso.textContent).toContain('Puede guardar la medicación sin descontar stock.');
    expect(casilla().checked).toBe(false);

    await enviar();
    expect(servicio.crear).toHaveBeenCalledWith('PAC-0001', sinDescuento, 'clave-1');
  });

  it('desde el aviso se puede reintentar la carga del inventario', async () => {
    inventario.listar.mockReturnValueOnce(caido()).mockReturnValueOnce(of([dolex]));
    const { elemento, casilla, marcarDescuento } = await preparar();
    await marcarDescuento();

    elemento.querySelector<HTMLButtonElement>('.aviso-inventario button')!.click();
    await new Promise((r) => setTimeout(r));

    expect(inventario.listar).toHaveBeenCalledTimes(2);
    expect(casilla().checked).toBe(true);
    expect(elemento.querySelector('.aviso-inventario')).toBeNull();
    expect(elemento.querySelector('option[value="MED-0001"]')).not.toBeNull();
  });

  it('si el descuento falla (stock insuficiente) explica que no se guardó nada', async () => {
    servicio.crear.mockReturnValue(
      throwError(() => new ApiError(400, ['Stock insuficiente: disponible 15, solicitado 20.'])),
    );
    const { marcarDescuento, elegir, enviar, alerta } = await preparar();

    await marcarDescuento();
    await elegir('MED-0001', '20');
    await enviar();

    expect(alerta()).toEqual([
      'Stock insuficiente: disponible 15, solicitado 20.',
      'No se guardó el registro ni se descontó stock.',
    ]);
    expect(dialogo.close).not.toHaveBeenCalled();
  });

  it('con 503 al descontar avisa de que el stock pudo descontarse', async () => {
    servicio.crear.mockReturnValue(caido());
    const { marcarDescuento, elegir, enviar, alerta } = await preparar();

    await marcarDescuento();
    await elegir('MED-0001', '2');
    await enviar();

    expect(alerta()[0]).toContain('El servicio de medicamentos no está disponible.');
    expect(alerta()[1]).toContain('la salida de stock pudo registrarse igualmente');
  });

  it('un error sin descuento no añade notas sobre el stock', async () => {
    servicio.crear.mockReturnValue(throwError(() => new ApiError(400, ['Mensaje.'])));
    const { enviar, alerta } = await preparar();

    await enviar();

    expect(alerta()).toEqual(['Mensaje.']);
  });
});
