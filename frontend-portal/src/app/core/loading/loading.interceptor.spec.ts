import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RUNTIME_CONFIG } from '../config';
import { LoadingService } from './loading.service';
import { loadingInterceptor } from './loading.interceptor';

describe('loadingInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let loading: boolean;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([loadingInterceptor])),
        provideHttpClientTesting(),
        {
          provide: RUNTIME_CONFIG,
          useValue: { issuerUri: 'http://issuer.test', clientId: 'portal', apiBaseUrl: 'http://api.test', production: false },
        },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    loading = false;
  });

  /** Inside fakeAsync, so tick() moves the bar's one-tick settle. */
  function watchLoading() {
    TestBed.inject(LoadingService).loading$.subscribe((value) => (loading = value));
  }

  afterEach(() => backend.verify());

  it('shows loading until the last API call in flight answers', fakeAsync(() => {
    watchLoading();
    http.get('http://api.test/api/v1/a').subscribe();
    http.get('http://api.test/api/v1/b').subscribe();
    tick();
    expect(loading).toBeTrue();

    backend.expectOne('http://api.test/api/v1/a').flush({});
    tick();
    expect(loading).toBeTrue();

    backend.expectOne('http://api.test/api/v1/b').flush({});
    tick();
    expect(loading).toBeFalse();
  }));

  it('stops when a call fails', fakeAsync(() => {
    watchLoading();
    http.get('http://api.test/api/v1/a').subscribe({ error: () => undefined });
    tick();
    expect(loading).toBeTrue();

    backend.expectOne('http://api.test/api/v1/a').flush({}, { status: 500, statusText: 'Server Error' });
    tick();

    expect(loading).toBeFalse();
  }));

  it("ignores calls that aren't to the API, such as Keycloak's", fakeAsync(() => {
    watchLoading();
    http.get('http://issuer.test/token').subscribe();
    tick();

    expect(loading).toBeFalse();
    backend.expectOne('http://issuer.test/token').flush({});
  }));
});
