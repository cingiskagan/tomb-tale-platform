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

  /** The cover goes up with a change and stays through the reload the change starts as soon as it answers. */
  it('blocks from a change until the reload after it answers', fakeAsync(() => {
    let blocking = false;
    TestBed.inject(LoadingService).blocking$.subscribe((value) => (blocking = value));
    http.post('http://api.test/api/v1/dealers/d-1/retire', null).subscribe(() => {
      http.get('http://api.test/api/v1/dealers').subscribe();
    });
    expect(blocking).toBeTrue();

    backend.expectOne('http://api.test/api/v1/dealers/d-1/retire').flush({});
    tick();
    expect(blocking).toBeTrue();

    backend.expectOne('http://api.test/api/v1/dealers').flush({});
    tick();
    expect(blocking).toBeFalse();
  }));

  /** Paging, sorting and filtering stay free: a read alone never covers the portal. */
  it('never blocks for a read', fakeAsync(() => {
    let blocking = false;
    TestBed.inject(LoadingService).blocking$.subscribe((value) => (blocking = value));
    http.get('http://api.test/api/v1/dealers').subscribe();
    tick();

    expect(blocking).toBeFalse();
    backend.expectOne('http://api.test/api/v1/dealers').flush({});
  }));

  /** A change that fails lifts the cover too, so the admin can try again. */
  it('lifts the block when a change fails', fakeAsync(() => {
    let blocking = false;
    TestBed.inject(LoadingService).blocking$.subscribe((value) => (blocking = value));
    http.delete('http://api.test/api/v1/purchases/p-1').subscribe({ error: () => undefined });

    backend.expectOne('http://api.test/api/v1/purchases/p-1').flush({}, { status: 409, statusText: 'Conflict' });
    tick();

    expect(blocking).toBeFalse();
  }));

  it("ignores calls that aren't to the API, such as Keycloak's", fakeAsync(() => {
    watchLoading();
    http.get('http://issuer.test/token').subscribe();
    tick();

    expect(loading).toBeFalse();
    backend.expectOne('http://issuer.test/token').flush({});
  }));
});
