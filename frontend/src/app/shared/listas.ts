import { Signal, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl } from '@angular/forms';
import {
  EMPTY,
  Observable,
  Subject,
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  merge,
  startWith,
  switchMap,
  tap,
} from 'rxjs';
import { mensajesDeError } from '../core/http/api-error';

/** Espera tras la última tecla antes de buscar en el servidor (pantallas de lista). */
export const ESPERA_BUSQUEDA_MS = 300;

/** Estado de una lista que se carga del servidor. */
export interface ListaRemota<T, C> {
  readonly elementos: Signal<readonly T[]>;
  readonly cargando: Signal<boolean>;
  /** Mensajes del último fallo de carga; vacío si la última carga fue bien. */
  readonly errores: Signal<readonly string[]>;
  /** Consulta de la última carga correcta (p. ej. el filtro, para el mensaje de lista vacía). */
  readonly consulta: Signal<C>;
  /** Repite la última consulta (tras un cambio o para reintentar). */
  recargar(): void;
}

export interface OpcionesListaRemota<T, C> {
  /** Consulta de la primera carga, que se hace en cuanto se crea la lista. */
  inicial: C;
  /** Consultas siguientes (búsqueda, pestaña...); cada una cancela la carga en curso. */
  cambios?: Observable<C>;
  cargar: (consulta: C) => Observable<readonly T[]>;
}

/**
 * Carga una lista del servidor y expone su estado en signals. Llamar en un
 * contexto de inyección (inicializador de campo o constructor): se da de baja
 * al destruirse el componente.
 */
export function crearListaRemota<T, C>(opciones: OpcionesListaRemota<T, C>): ListaRemota<T, C> {
  const elementos = signal<readonly T[]>([]);
  const cargando = signal(true);
  const errores = signal<readonly string[]>([]);
  const consulta = signal(opciones.inicial);
  const recargas = new Subject<void>();
  let ultima = opciones.inicial;

  merge(opciones.cambios ?? EMPTY, recargas.pipe(map(() => ultima)))
    .pipe(
      startWith(opciones.inicial),
      tap((pedida) => {
        ultima = pedida;
        cargando.set(true);
      }),
      switchMap((pedida) =>
        opciones.cargar(pedida).pipe(
          tap({
            next: (recibidos) => {
              elementos.set(recibidos);
              consulta.set(pedida);
              errores.set([]);
              cargando.set(false);
            },
            error: (error: unknown) => {
              errores.set(mensajesDeError(error));
              cargando.set(false);
            },
          }),
          catchError(() => EMPTY),
        ),
      ),
      takeUntilDestroyed(),
    )
    .subscribe();

  return {
    elementos: elementos.asReadonly(),
    cargando: cargando.asReadonly(),
    errores: errores.asReadonly(),
    consulta: consulta.asReadonly(),
    recargar: () => recargas.next(),
  };
}

/** Texto de un buscador, recortado, cuando se deja de escribir y solo si cambió. */
export function textoBuscado(control: FormControl<string>): Observable<string> {
  return control.valueChanges.pipe(
    debounceTime(ESPERA_BUSQUEDA_MS),
    map((texto) => texto.trim()),
    distinctUntilChanged(),
  );
}
