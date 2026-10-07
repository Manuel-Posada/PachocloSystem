import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';

interface Seccion {
  readonly ruta: string;
  readonly etiqueta: string;
  readonly icono: string;
}

/** Marco de la aplicación autenticada: barra superior, menú lateral y contenido. */
@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShellComponent {
  protected readonly esMovil = toSignal(
    inject(BreakpointObserver)
      .observe(Breakpoints.Handset)
      .pipe(map((estado) => estado.matches)),
    { initialValue: false },
  );

  protected readonly secciones: readonly Seccion[] = [
    { ruta: '/pacientes', etiqueta: 'Pacientes', icono: 'personal_injury' },
    { ruta: '/trabajadores', etiqueta: 'Trabajadores', icono: 'badge' },
    { ruta: '/medicamentos', etiqueta: 'Medicamentos', icono: 'medication' },
    { ruta: '/historial', etiqueta: 'Historial clínico', icono: 'history_edu' },
  ];
}
