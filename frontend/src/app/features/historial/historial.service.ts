import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { FiltroHistorial, Registro, RegistroRequest } from './historial.models';

/** Acceso al historial clínico. Los errores llegan como `ApiError` (errorInterceptor). */
@Injectable({ providedIn: 'root' })
export class HistorialService {
  private readonly http = inject(HttpClient);

  /** Todos los registros, del más antiguo al más reciente; `texto` busca según `filtro`. */
  listar(filtro: FiltroHistorial, texto = ''): Observable<Registro[]> {
    let params = new HttpParams().set('filtro', filtro);
    if (texto.trim()) {
      params = params.set('q', texto.trim());
    }
    return this.http.get<Registro[]>('/api/historial', { params });
  }

  /** Registros de un paciente; `texto` filtra por autor (id o nombre). */
  listarDePaciente(idPaciente: string, texto = ''): Observable<Registro[]> {
    const q = texto.trim();
    return this.http.get<Registro[]>(this.url(idPaciente), {
      params: q ? new HttpParams().set('q', q) : {},
    });
  }

  /**
   * Crea un registro. Si descuenta stock, el backend hace antes la salida en
   * MedicamentosService y, si falla, no guarda el registro.
   */
  crear(idPaciente: string, registro: RegistroRequest): Observable<Registro> {
    return this.http.post<Registro>(this.url(idPaciente), registro);
  }

  private url(idPaciente: string): string {
    return `/api/pacientes/${encodeURIComponent(idPaciente)}/historial`;
  }
}
