import { Component, inject } from '@angular/core';
import { RouterOutlet, Router } from '@angular/router';
import { MenubarModule } from 'primeng/menubar';
import { MenuItem } from 'primeng/api';
import { AuthService, PlatformRole } from '../core/auth';

/**
 * Main layout component for authenticated views.
 *
 * Provides a top navigation bar (Menubar) and an outlet for the nested routes.
 * Maintains the dark and gold (Cinzel) theme used by the platform.
 */
@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, MenubarModule],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.css'
})
export class MainLayoutComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  /** Roles that grant access to admin-only menu items. */
  private static readonly ADMIN_ROLES: PlatformRole[] = [
    PlatformRole.PLATFORM_ADMIN,
    PlatformRole.GAME_MASTER,
  ];

  readonly username: string;

  items: MenuItem[] = [
    {
      label: 'Dashboard',
      icon: 'pi pi-home',
      command: () => this.router.navigate(['/dashboard']),
    },
    {
      label: 'Purchases',
      icon: 'pi pi-shopping-cart',
      visible: this.authService.hasAnyRole(MainLayoutComponent.ADMIN_ROLES),
      command: () => this.router.navigate(['/purchases']),
    },
    {
      label: 'Players',
      icon: 'pi pi-user',
      visible: this.authService.hasAnyRole(MainLayoutComponent.ADMIN_ROLES),
      command: () => this.router.navigate(['/players']),
    },
    {
      label: 'My Profile',
      icon: 'pi pi-id-card',
      command: () => this.router.navigate(['/profile']),
    },
    {
      label: 'Logout',
      icon: 'pi pi-sign-out',
      command: () => this.authService.logout(),
    },
  ];

  constructor() {
    const profile = this.authService.getUserProfile();
    this.username = profile?.preferred_username || profile?.name || 'Adventurer';
  }
}
