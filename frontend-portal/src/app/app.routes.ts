import { Routes } from '@angular/router';
import { authGuard } from './core/auth';
import { LoginComponent } from './features/login/login';
import { CallbackComponent } from './features/callback/callback';
import { DashboardComponent } from './features/dashboard/dashboard';

/**
 * Top-level application routes.
 *
 * - /login     → Public login page (delegates auth to Zitadel)
 * - /callback  → OIDC redirect handler (processes auth code)
 * - /dashboard → Protected placeholder dashboard
 * - /          → Redirects to login
 * - **         → Catch-all redirects to login
 */
export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'callback', component: CallbackComponent },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard],
  },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' },
];
