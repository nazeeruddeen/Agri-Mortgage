import { Routes } from '@angular/router';
import { AgriMortgageWorkspaceComponent } from './workspace.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'overview' },
  { path: 'overview', component: AgriMortgageWorkspaceComponent, data: { tab: 'dashboard' } },
  { path: 'intake', component: AgriMortgageWorkspaceComponent, data: { tab: 'intake' } },
  { path: 'applications', component: AgriMortgageWorkspaceComponent, data: { tab: 'search' } },
  { path: 'operations', component: AgriMortgageWorkspaceComponent, data: { tab: 'operations' } },
  { path: '**', redirectTo: 'overview' }
];
