import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { AuthService } from '../../core/auth';

/**
 * Handles the OIDC redirect callback after the user authenticates
 * with Zitadel. Displays a loading spinner while the authorization
 * code is exchanged for tokens, then redirects to the dashboard.
 */
@Component({
  selector: 'app-callback',
  standalone: true,
  imports: [ProgressSpinnerModule],
  template: `
    <div class="callback-container">
      <p-progressSpinner
        ariaLabel="Authenticating"
        strokeWidth="3"
        styleClass="callback-spinner"
      />
      <p class="callback-text">Entering the tomb...</p>
    </div>
  `,
  styles: [`
    .callback-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      background: #0a0a0f;
      gap: 1.5rem;
    }

    .callback-text {
      font-family: 'Cinzel', serif;
      color: #8a8078;
      font-size: 1rem;
      letter-spacing: 0.15em;
      animation: pulse-text 2s ease-in-out infinite;
    }

    @keyframes pulse-text {
      0%, 100% { opacity: 0.5; }
      50% { opacity: 1; }
    }
  `],
})
export class CallbackComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  /**
   * Processes the OIDC callback and redirects based on auth result.
   *
   * Explicitly handles the async operation within ngOnInit by catching
   * errors and routing to login on failure, avoiding unhandled rejections.
   */
  ngOnInit(): void {
    this.processCallback();
  }

  private processCallback(): void {
    this.authService.initializeAuth()
      .then(() => {
        const targetRoute = this.authService.isAuthenticated()
          ? '/dashboard'
          : '/login';
        this.router.navigate([targetRoute]);
      })
      .catch(() => {
        this.router.navigate(['/login']);
      });
  }
}
