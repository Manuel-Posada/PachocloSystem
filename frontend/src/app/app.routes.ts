import { Routes } from '@angular/router';
import { authGuard, invitadoGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Iniciar sesión · PachocloSystem',
    canActivate: [invitadoGuard],
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'pacientes' },
      {
        path: 'pacientes',
        title: 'Pacientes · PachocloSystem',
        loadComponent: () =>
          import('./features/pacientes/pacientes-lista/pacientes-lista.component').then(
            (m) => m.PacientesListaComponent,
          ),
      },
      {
        path: 'pacientes/:id/historial',
        title: 'Historial del paciente · PachocloSystem',
        loadComponent: () =>
          import('./features/historial/historial-paciente/historial-paciente.component').then(
            (m) => m.HistorialPacienteComponent,
          ),
      },
      {
        path: 'trabajadores',
        title: 'Trabajadores · PachocloSystem',
        loadComponent: () =>
          import('./features/trabajadores/trabajadores-lista/trabajadores-lista.component').then(
            (m) => m.TrabajadoresListaComponent,
          ),
      },
      {
        path: 'medicamentos',
        title: 'Medicamentos · PachocloSystem',
        loadComponent: () =>
          import('./features/medicamentos/medicamentos-lista/medicamentos-lista.component').then(
            (m) => m.MedicamentosListaComponent,
          ),
      },
      {
        path: 'historial',
        title: 'Historial clínico · PachocloSystem',
        loadComponent: () =>
          import('./features/historial/historial-general/historial-general.component').then(
            (m) => m.HistorialGeneralComponent,
          ),
      },
    ],
  },
  {
    path: '**',
    title: 'Página no encontrada · PachocloSystem',
    loadComponent: () =>
      import('./features/no-encontrado/no-encontrado.component').then(
        (m) => m.NoEncontradoComponent,
      ),
  },
];
