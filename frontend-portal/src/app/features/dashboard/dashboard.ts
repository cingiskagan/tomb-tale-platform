import { Component, OnInit, inject } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { AuthService, UserProfile } from '../../core/auth';

/**
 * Placeholder dashboard component shown after successful authentication.
 *
 * Displays a welcome message with the user's name and a logout button.
 * Full dashboard implementation will be built in a future task.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [ButtonModule, CardModule],
  template: `
    <div class="dashboard-container">
      <p-card styleClass="dashboard-card">
        <ng-template #header>
          <div class="dashboard-header">
            <h1 class="dashboard-title">Welcome, Adventurer</h1>
            @if (userProfile) {
              <p class="dashboard-username">
                {{ userProfile.preferred_username || userProfile.name }}
              </p>
            }
          </div>
        </ng-template>

        <p class="dashboard-message">
          You have entered the tomb. The full dashboard is being forged...
        </p>

        <ng-template #footer>
          <p-button
            id="dashboard-logout-button"
            label="Leave the Tomb"
            icon="pi pi-sign-out"
            severity="secondary"
            [outlined]="true"
            (onClick)="onLogout()"
          />
        </ng-template>
      </p-card>
    </div>
  `,
  styles: [`
    .dashboard-container {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      background: #0a0a0f;
      padding: 2rem;
    }

    ::ng-deep .dashboard-card {
      max-width: 500px;
      width: 100%;
      background: rgba(15, 15, 25, 0.9) !important;
      border: 1px solid rgba(212, 162, 78, 0.2);
    }

    .dashboard-header {
      padding: 1.5rem 1.5rem 0;
    }

    .dashboard-title {
      font-family: 'Cinzel', serif;
      color: #d4a24e;
      font-size: 1.5rem;
      margin: 0 0 0.25rem;
      letter-spacing: 0.1em;
    }

    .dashboard-username {
      color: #8a8078;
      font-size: 0.9rem;
      margin: 0;
    }

    .dashboard-message {
      color: #e8e0d4;
      font-size: 0.95rem;
      font-style: italic;
      line-height: 1.6;
    }
  `],
})
export class DashboardComponent implements OnInit {
  userProfile: UserProfile | null = null;

  private readonly authService = inject(AuthService);

  ngOnInit(): void {
    this.userProfile = this.authService.getUserProfile();
  }

  /** Triggers OIDC RP-initiated logout. */
  onLogout(): void {
    this.authService.logout();
  }
}
