import { Routes } from '@angular/router';
import { authGuard, invitadoGuard } from './core/auth/auth.guard';

const enConstruccion = () =>
  import('./features/en-construccion/en-construccion.component').then(
    (m) => m.EnConstruccionComponent,
  );

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
        path: 'trabajadores',
        title: 'Trabajadores · PachocloSystem',
        loadComponent: enConstruccion,
        data: { titulo: 'Trabajadores' },
      },
      {
        path: 'medicamentos',
        title: 'Medicamentos · PachocloSystem',
        loadComponent: enConstruccion,
        data: { titulo: 'Medicamentos' },
      },
      {
        path: 'historial',
        title: 'Historial clínico · PachocloSystem',
        loadComponent: enConstruccion,
        data: { titulo: 'Historial clínico' },
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
