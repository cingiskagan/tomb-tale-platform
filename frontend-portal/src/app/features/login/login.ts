import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../../core/auth';

/**
 * Login page component for Tomb Tale Online RPG.
 *
 * This component does NOT render a username/password form.
 * Instead, it delegates authentication to Zitadel's hosted login page
 * via the OIDC Authorization Code + PKCE flow.
 *
 * If the user is already authenticated, they are redirected to the dashboard.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ButtonModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class LoginComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.authService.initializeAuth().then(() => {
      if (this.authService.isAuthenticated()) {
        this.router.navigate(['/dashboard']);
      }
    }).catch((err) => {
      console.error('[Login] Failed to verify auth state', err);
    });
  }

  /** Initiates the OIDC PKCE login flow, redirecting to Zitadel. */
  onEnterTomb(): void {
    this.authService.login();
  }
}
