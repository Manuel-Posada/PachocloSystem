import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Trabajador, TrabajadorRequest } from './trabajador.models';

const URL = '/api/trabajadores';

/** Acceso a `/api/trabajadores`. Los errores llegan como `ApiError` (errorInterceptor). */
@Injectable({ providedIn: 'root' })
export class TrabajadorService {
  private readonly http = inject(HttpClient);

  /** `texto` filtra por id o nombre en el servidor. */
  listar(texto = ''): Observable<Trabajador[]> {
    const q = texto.trim();
    return this.http.get<Trabajador[]>(URL, { params: q ? new HttpParams().set('q', q) : {} });
  }

  registrar(datos: TrabajadorRequest): Observable<Trabajador> {
    return this.http.post<Trabajador>(URL, datos);
  }

  editar(id: string, datos: TrabajadorRequest): Observable<Trabajador> {
    return this.http.put<Trabajador>(`${URL}/${encodeURIComponent(id)}`, datos);
  }

  /** El backend también desactiva el usuario vinculado al trabajador, si lo tiene. */
  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(`${URL}/${encodeURIComponent(id)}`);
  }
}
