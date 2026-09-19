import { InjectionToken } from '@angular/core';

/**
 * Settings the portal reads at startup rather than compiling in.
 *
 * Zitadel generates the client id when the OIDC app is created, so it cannot
 * live in a source file without a rebuild rewriting that file. See ADR 0019.
 */
export interface RuntimeConfig {
  readonly zitadelIssuerUri: string;
  readonly zitadelClientId: string;
  readonly apiBaseUrl: string;
}

/** The loaded config. Provided in `main.ts`, before the app bootstraps. */
export const RUNTIME_CONFIG = new InjectionToken<RuntimeConfig>('RUNTIME_CONFIG');

const CONFIG_URL = 'config.json';

const REQUIRED_KEYS = ['zitadelIssuerUri', 'zitadelClientId', 'apiBaseUrl'] as const;

/** Points at the one command that writes the file, whatever went wrong. */
function configError(reason: string): Error {
  return new Error(
    `Cannot read ${CONFIG_URL}: ${reason}. ` +
      'Run infrastructure/zitadel-setup.sh to write it.',
  );
}

/**
 * Fetches `config.json` from next to index.html — resolved against the base
 * href, so it still works if the portal is ever served under a sub-path.
 *
 * The dev server answers 404 when the file is missing, but a static host with
 * an SPA fallback answers 200 with index.html. Both mean the same thing, so a
 * parse failure gets the same message as a 404.
 */
export async function loadRuntimeConfig(): Promise<RuntimeConfig> {
  let response: Response;
  try {
    response = await fetch(new URL(CONFIG_URL, document.baseURI), { cache: 'no-store' });
  } catch {
    throw configError('the request failed');
  }

  if (!response.ok) {
    throw configError(`the server answered ${response.status}`);
  }

  let parsed: unknown;
  try {
    parsed = await response.json();
  } catch {
    throw configError('the response is not JSON');
  }

  return validate(parsed);
}

function validate(parsed: unknown): RuntimeConfig {
  if (typeof parsed !== 'object' || parsed === null) {
    throw configError('the file does not hold an object');
  }

  const values = parsed as Record<string, unknown>;
  const missing = REQUIRED_KEYS.filter(
    (key) => typeof values[key] !== 'string' || values[key] === '',
  );

  if (missing.length > 0) {
    throw configError(`${missing.join(', ')} missing or blank`);
  }

  return {
    zitadelIssuerUri: values['zitadelIssuerUri'] as string,
    zitadelClientId: values['zitadelClientId'] as string,
    apiBaseUrl: values['apiBaseUrl'] as string,
  };
}
