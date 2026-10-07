import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Paciente, PacienteRequest } from './paciente.models';

const URL = '/api/pacientes';

/** Acceso a `/api/pacientes`. Los errores llegan como `ApiError` (errorInterceptor). */
@Injectable({ providedIn: 'root' })
export class PacienteService {
  private readonly http = inject(HttpClient);

  /** `texto` filtra por id o nombre en el servidor. */
  listar(texto = ''): Observable<Paciente[]> {
    const q = texto.trim();
    return this.http.get<Paciente[]>(URL, { params: q ? new HttpParams().set('q', q) : {} });
  }

  obtener(id: string): Observable<Paciente> {
    return this.http.get<Paciente>(`${URL}/${encodeURIComponent(id)}`);
  }

  registrar(datos: PacienteRequest): Observable<Paciente> {
    return this.http.post<Paciente>(URL, datos);
  }

  editar(id: string, datos: PacienteRequest): Observable<Paciente> {
    return this.http.put<Paciente>(`${URL}/${encodeURIComponent(id)}`, datos);
  }

  cambiarHabitacion(id: string, habitacion: number): Observable<Paciente> {
    return this.http.patch<Paciente>(`${URL}/${encodeURIComponent(id)}/habitacion`, {
      habitacion,
    });
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(`${URL}/${encodeURIComponent(id)}`);
  }
}
