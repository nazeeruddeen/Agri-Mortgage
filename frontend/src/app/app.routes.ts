import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'overview' },
  { path: 'overview', loadComponent: () => import('./workspace.component').then((m) => m.AgriMortgageWorkspaceComponent), data: { tab: 'dashboard' } },
  { path: 'intake', loadComponent: () => import('./workspace.component').then((m) => m.AgriMortgageWorkspaceComponent), data: { tab: 'intake' } },
  { path: 'applications', loadComponent: () => import('./workspace.component').then((m) => m.AgriMortgageWorkspaceComponent), data: { tab: 'search' } },
  { path: 'operations', loadComponent: () => import('./workspace.component').then((m) => m.AgriMortgageWorkspaceComponent), data: { tab: 'operations' } },
  { path: '**', redirectTo: 'overview' }
];
