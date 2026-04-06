import { Component, inject } from '@angular/core';
import { RouterOutlet, Router } from '@angular/router';
import { MenubarModule } from 'primeng/menubar';
import { MenuItem } from 'primeng/api';
import { AuthService } from '../core/auth';

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
  template: `
    <div class="layout-wrapper">
      <p-menubar [model]="items" styleClass="main-menubar">
        <ng-template #start>
          <div class="logo-container">
            <span class="logo-text">Tomb Tale Online</span>
          </div>
        </ng-template>
        <ng-template #end>
          <span class="user-greeting">
            Welcome, {{ username }}
          </span>
        </ng-template>
      </p-menubar>
      
      <main class="layout-content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [`
    .layout-wrapper {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      background: #0a0a0f;
    }

    ::ng-deep .main-menubar {
      background: rgba(15, 15, 25, 0.95) !important;
      border: 1px solid rgba(212, 162, 78, 0.2);
      border-radius: 0;
      padding: 0.5rem 1.5rem;
    }

    ::ng-deep .main-menubar .p-menubar-item-link {
      color: #e8e0d4 !important;
    }

    ::ng-deep .main-menubar .p-menubar-item-link:hover {
      background: rgba(212, 162, 78, 0.1) !important;
      color: #d4a24e !important;
    }

    ::ng-deep .main-menubar .p-menubar-item-active .p-menubar-item-link {
      color: #d4a24e !important;
      font-weight: bold;
    }

    .logo-container {
      display: flex;
      align-items: center;
      margin-right: 2rem;
    }

    .logo-text {
      font-family: 'Cinzel', serif;
      color: #d4a24e;
      font-size: 1.25rem;
      font-weight: bold;
      letter-spacing: 0.1em;
    }

    .user-greeting {
      color: #8a8078;
      font-size: 0.9rem;
      font-style: italic;
    }

    .layout-content {
      flex-grow: 1;
      padding: 2rem;
    }
  `],
})
export class MainLayoutComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

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
      command: () => this.router.navigate(['/purchases']),
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
