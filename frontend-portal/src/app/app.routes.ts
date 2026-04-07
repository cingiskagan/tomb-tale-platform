import { Routes } from '@angular/router';
import { authGuard } from './core/auth';
import { LoginComponent } from './features/login/login';
import { CallbackComponent } from './features/callback/callback';
import { DashboardComponent } from './features/dashboard/dashboard';
import { MainLayoutComponent } from './layout/main-layout.component';
import { PurchaseListComponent } from './features/purchases/purchase-list.component';

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
      { path: 'dashboard', component: DashboardComponent },
      { path: 'purchases', component: PurchaseListComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: 'login' },
];
