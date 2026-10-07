import { Injector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl } from '@angular/forms';
import { Observable, Subject, of, throwError } from 'rxjs';
import { ApiError } from '../core/http/api-error';
import { ESPERA_BUSQUEDA_MS, crearListaRemota, textoBuscado } from './listas';

describe('crearListaRemota', () => {
  const crear = <C>(opciones: Parameters<typeof crearListaRemota<string, C>>[0]) =>
    runInInjectionContext(TestBed.inject(Injector), () => crearListaRemota(opciones));

  it('carga la consulta inicial al crearse', () => {
    const cargar = vi.fn(() => of(['a', 'b']));

    const lista = crear({ inicial: 'q', cargar });

    expect(cargar).toHaveBeenCalledWith('q');
    expect(lista.elementos()).toEqual(['a', 'b']);
    expect(lista.consulta()).toBe('q');
    expect(lista.cargando()).toBe(false);
  });

  it('indica que está cargando mientras no llega la respuesta', () => {
    const respuesta = new Subject<string[]>();

    const lista = crear({ inicial: '', cargar: () => respuesta });

    expect(lista.cargando()).toBe(true);
    respuesta.next(['a']);
    expect(lista.cargando()).toBe(false);
  });

  it('cada cambio cancela la carga en curso y gana la última consulta', () => {
    const cambios = new Subject<string>();
    const lenta = new Subject<string[]>();
    const cargar = vi.fn<(c: string) => Observable<string[]>>((c) =>
      c === 'lenta' ? lenta : of([c]),
    );
    const lista = crear({ inicial: 'lenta', cambios, cargar });

    cambios.next('rápida');
    lenta.next(['tarde']);

    expect(lista.elementos()).toEqual(['rápida']);
    expect(lista.consulta()).toBe('rápida');
  });

  it('guarda los mensajes de error y recargar repite la última consulta', () => {
    const cambios = new Subject<string>();
    const cargar = vi
      .fn<(c: string) => Observable<string[]>>()
      .mockReturnValueOnce(of(['inicial']))
      .mockReturnValueOnce(throwError(() => new ApiError(503, ['Servicio caído.'])))
      .mockReturnValueOnce(of(['ok']));
    const lista = crear({ inicial: '', cambios, cargar });

    cambios.next('x');
    expect(lista.errores()).toEqual(['Servicio caído.']);
    expect(lista.elementos()).toEqual(['inicial']);
    expect(lista.consulta()).toBe('');

    lista.recargar();
    expect(cargar).toHaveBeenLastCalledWith('x');
    expect(lista.errores()).toEqual([]);
    expect(lista.elementos()).toEqual(['ok']);
    expect(lista.consulta()).toBe('x');
  });
});

describe('textoBuscado', () => {
  it('emite el texto recortado al dejar de escribir y solo si cambió', async () => {
    const control = new FormControl('', { nonNullable: true });
    const emitidos: string[] = [];
    textoBuscado(control).subscribe((t) => emitidos.push(t));

    control.setValue('a');
    control.setValue('ana ');
    await new Promise((r) => setTimeout(r, ESPERA_BUSQUEDA_MS + 50));
    control.setValue(' ana');
    await new Promise((r) => setTimeout(r, ESPERA_BUSQUEDA_MS + 50));

    expect(emitidos).toEqual(['ana']);
  });
});
