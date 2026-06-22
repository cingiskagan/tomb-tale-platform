import { Routes } from '@angular/router';
import { authGuard } from './core/auth';
import { LoginComponent } from './features/login/login';
import { CallbackComponent } from './features/callback/callback';
import { MainLayoutComponent } from './layout/main-layout.component';

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
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard').then(m => m.DashboardComponent)
      },
      {
        path: 'purchases',
        loadComponent: () => import('./features/purchases/purchase-list.component').then(m => m.PurchaseListComponent)
      },
      {
        path: 'players',
        loadComponent: () => import('./features/players/player-list.component').then(m => m.PlayerListComponent)
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: 'login' },
];
