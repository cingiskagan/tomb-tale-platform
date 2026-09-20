import { loadRuntimeConfig } from './runtime-config';

/** A config.json that would boot the portal. */
const VALID = {
  zitadelIssuerUri: 'http://localhost:8080',
  zitadelClientId: '391384737463205891',
  apiBaseUrl: 'http://localhost:8080',
};

describe('loadRuntimeConfig', () => {
  function answerWith(response: Partial<Response>): void {
    spyOn(globalThis, 'fetch').and.resolveTo({
      ok: true,
      status: 200,
      ...response,
    } as Response);
  }

  function answerWithBody(body: unknown): void {
    answerWith({ json: () => Promise.resolve(body) });
  }

  /** Every failure has to name the script that writes the file. */
  async function expectRejection(contains: string): Promise<void> {
    await expectAsync(loadRuntimeConfig()).toBeRejectedWithError(
      new RegExp(`${contains}[\\s\\S]*zitadel-setup\\.sh`),
    );
  }

  it('returns the three settings', async () => {
    answerWithBody(VALID);
    await expectAsync(loadRuntimeConfig()).toBeResolvedTo(VALID);
  });

  it('rejects when the file is not there', async () => {
    answerWith({ ok: false, status: 404 });
    await expectRejection('404');
  });

  it('rejects when an SPA fallback answered with index.html', async () => {
    answerWith({ json: () => Promise.reject(new SyntaxError('Unexpected token <')) });
    await expectRejection('not JSON');
  });

  it('names every setting that is missing or blank', async () => {
    answerWithBody({ ...VALID, zitadelClientId: '', apiBaseUrl: undefined });
    await expectRejection('zitadelClientId, apiBaseUrl');
  });

  it('rejects when the file does not hold an object', async () => {
    answerWithBody('not a config');
    await expectRejection('does not hold an object');
  });
});
