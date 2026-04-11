import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'beneficios',
    pathMatch: 'full'
  },
  {
    path: 'beneficios',
    loadComponent: () => import('./features/beneficio/pages/beneficio-list/beneficio-list.component')
      .then(m => m.BeneficioListComponent)
  },
  {
    path: 'beneficios/novo',
    loadComponent: () => import('./features/beneficio/pages/beneficio-form/beneficio-form.component')
      .then(m => m.BeneficioFormComponent)
  },
  {
    path: 'beneficios/:id/editar',
    loadComponent: () => import('./features/beneficio/pages/beneficio-form/beneficio-form.component')
      .then(m => m.BeneficioFormComponent)
  },
  {
    path: 'transferencia',
    loadComponent: () => import('./features/beneficio/pages/transferencia/transferencia.component')
      .then(m => m.TransferenciaComponent)
  },
  {
    path: '**',
    redirectTo: 'beneficios'
  }
];
