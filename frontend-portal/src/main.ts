import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { RUNTIME_CONFIG, loadRuntimeConfig } from './app/core/config';

/**
 * The config has to be in hand before the app bootstraps: AuthService
 * configures the OIDC client in its constructor, and it needs the client id.
 */
loadRuntimeConfig()
  .then((runtimeConfig) =>
    bootstrapApplication(App, {
      ...appConfig,
      providers: [...appConfig.providers, { provide: RUNTIME_CONFIG, useValue: runtimeConfig }],
    }),
  )
  .catch((error: unknown) => {
    console.error(error);
    showStartupError(error);
  });

/** Without this a missing config.json is a blank page and nothing else. */
function showStartupError(error: unknown): void {
  const root = document.querySelector('app-root');
  if (root) {
    root.textContent =
      error instanceof Error ? error.message : 'The portal failed to start.';
  }
}
