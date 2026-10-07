import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { etiquetaRol } from '../../../core/roles';
import { formatearFechaHora } from '../../../shared/fechas';
import { ETIQUETAS_TIPO, Registro } from '../historial.models';

/** Lista de registros del historial (solo presentación). */
@Component({
  selector: 'app-registros',
  imports: [RouterLink],
  templateUrl: './registros.component.html',
  styleUrl: './registros.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegistrosComponent {
  readonly registros = input.required<readonly Registro[]>();
  /** En el historial general cada registro muestra su paciente, con enlace a su historial. */
  readonly mostrarPaciente = input(false);

  protected readonly etiquetasTipo = ETIQUETAS_TIPO;
  protected readonly etiquetaRol = etiquetaRol;
  protected readonly formatearFechaHora = formatearFechaHora;
}
