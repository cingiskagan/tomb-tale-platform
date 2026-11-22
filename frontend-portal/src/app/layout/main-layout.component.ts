import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService, PlatformRole } from '../core/auth';
import { ChangeBlockerComponent, LoadingService } from '../core/loading';

/** A link in the top bar, shown only to the roles that may open its page. */
interface NavItem {
  label: string;
  icon: string;
  route: string;
  visible: boolean;
}

/**
 * Main layout component for authenticated views.
 *
 * Provides a top navigation bar and an outlet for the nested routes.
 * Maintains the dark and gold (Cinzel) theme used by the platform.
 */
@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    AsyncPipe,
    ChangeBlockerComponent,
  ],
  templateUrl: './main-layout.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './main-layout.component.css',
})
export class MainLayoutComponent {
  private readonly authService = inject(AuthService);

  /** A thin bar on top of everything while any API call is in flight (ADR 0037). */
  readonly loading$ = inject(LoadingService).loading$;

  /** Roles that grant access to admin-only menu items. */
  private static readonly ADMIN_ROLES: PlatformRole[] = [
    PlatformRole.PLATFORM_ADMIN,
    PlatformRole.GAME_MASTER,
  ];

  readonly username: string;

  readonly items: NavItem[] = [
    { label: 'Dashboard', icon: 'pi-home', route: '/dashboard', visible: true },
    {
      label: 'Purchases',
      icon: 'pi-shopping-cart',
      route: '/purchases',
      visible: this.authService.hasAnyRole(MainLayoutComponent.ADMIN_ROLES),
    },
    {
      label: 'Players',
      icon: 'pi-user',
      route: '/players',
      visible: this.authService.hasAnyRole(MainLayoutComponent.ADMIN_ROLES),
    },
    {
      label: 'Items',
      icon: 'pi-box',
      route: '/items',
      visible: this.authService.hasAnyRole([PlatformRole.PLATFORM_ADMIN]),
    },
    {
      label: 'Dealers',
      icon: 'pi-shop',
      route: '/dealers',
      visible: this.authService.hasAnyRole([PlatformRole.PLATFORM_ADMIN]),
    },
    { label: 'My Profile', icon: 'pi-id-card', route: '/profile', visible: true },
  ].filter((item) => item.visible);

  constructor() {
    const profile = this.authService.getUserProfile();
    this.username = profile?.preferred_username || profile?.name || 'Adventurer';
  }

  logout() {
    this.authService.logout();
  }
}
