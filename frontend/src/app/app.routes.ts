import { Routes } from '@angular/router';
import { roleGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'jobs' },
  { path: 'login', loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent) },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'jobs',
    loadComponent: () => import('./features/job/job-search.component').then((m) => m.JobSearchComponent),
  },
  {
    path: 'jobs/:id',
    loadComponent: () => import('./features/job/job-detail.component').then((m) => m.JobDetailComponent),
  },
  {
    path: 'candidate',
    canActivate: [roleGuard('CANDIDATE')],
    loadComponent: () =>
      import('./features/candidate/candidate-home.component').then((m) => m.CandidateHomeComponent),
  },
  {
    path: 'employer',
    canActivate: [roleGuard('EMPLOYER')],
    loadComponent: () => import('./features/employer/employer-home.component').then((m) => m.EmployerHomeComponent),
  },
  {
    path: 'admin',
    canActivate: [roleGuard('ADMIN')],
    loadComponent: () => import('./features/admin/admin-home.component').then((m) => m.AdminHomeComponent),
  },
];
