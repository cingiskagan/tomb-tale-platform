import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth';

/**
 * Root application component.
 *
 * Initializes the OIDC authentication flow on startup
 * and renders the active route via the router outlet.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class App implements OnInit {
  private readonly authService = inject(AuthService);

  /**
   * Boot the OIDC discovery document and attempt silent login.
   *
   * Delegates the async work to a private method to satisfy
   * OnInit's void return type contract.
   */
  ngOnInit(): void {
    this.bootstrapAuth();
  }

  /** Initializes auth and logs errors without swallowing them. */
  private bootstrapAuth(): void {
    this.authService.initializeAuth()
      .catch((error: unknown) => {
        console.error(
          '[App] Failed to initialize authentication:',
          error,
        );
      });
  }
}
