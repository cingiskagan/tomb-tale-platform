import { Component, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
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
  imports: [MatButtonModule, MatCardModule, MatIconModule],
  template: `
    <div class="dashboard-container">
      <mat-card class="dashboard-card" appearance="outlined">
        <div class="dashboard-header">
          <h1 class="dashboard-title">Welcome, Adventurer</h1>
          @if (userProfile) {
            <p class="dashboard-username">
              {{ userProfile.preferred_username || userProfile.name }}
            </p>
          }
        </div>

        <mat-card-content>
          <p class="dashboard-message">
            You have entered the tomb. The full dashboard is being forged...
          </p>
        </mat-card-content>

        <mat-card-actions>
          <button id="dashboard-logout-button" mat-stroked-button type="button" (click)="onLogout()">
            <mat-icon fontIcon="pi-sign-out" />
            Leave the Tomb
          </button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.Eager,
  styles: [`
    .dashboard-container {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      background: #0a0a0f;
      padding: 2rem;
    }

    .dashboard-card {
      --mat-card-outlined-container-color: rgba(15, 15, 25, 0.9);
      --mat-card-outlined-outline-color: rgba(212, 162, 78, 0.2);
      max-width: 500px;
      width: 100%;
    }

    .dashboard-header {
      padding: 1.5rem 1rem 0.5rem;
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
