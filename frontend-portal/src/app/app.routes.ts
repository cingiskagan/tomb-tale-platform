import { Routes } from '@angular/router';
import { authGuard, roleGuard, PlatformRole } from './core/auth';
import { LoginComponent } from './features/login/login';
import { CallbackComponent } from './features/callback/callback';
import { MainLayoutComponent } from './layout/main-layout.component';
import { playerProfileResolver } from './core/api/player.resolver';

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
    resolve: { playerProfile: playerProfileResolver },
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard').then(m => m.DashboardComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/profile/my-profile.component').then(m => m.MyProfileComponent)
      },
      {
        path: 'purchases',
        canActivate: [roleGuard],
        data: { roles: [PlatformRole.PLATFORM_ADMIN, PlatformRole.GAME_MASTER] },
        loadComponent: () => import('./features/purchases/purchase-list.component').then(m => m.PurchaseListComponent)
      },
      {
        path: 'players',
        canActivate: [roleGuard],
        data: { roles: [PlatformRole.PLATFORM_ADMIN, PlatformRole.GAME_MASTER] },
        loadComponent: () => import('./features/players/player-list.component').then(m => m.PlayerListComponent)
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: 'login' },
];
