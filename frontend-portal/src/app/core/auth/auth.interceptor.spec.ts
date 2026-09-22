import { TestBed } from '@angular/core/testing';
import { HttpRequest, HttpEvent, HttpHandlerFn } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { RUNTIME_CONFIG, RuntimeConfig } from '../config';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

const CONFIG: RuntimeConfig = {
  zitadelIssuerUri: 'http://localhost:8080',
  zitadelClientId: 'test-client',
  apiBaseUrl: 'http://localhost:8081',
};

const API_URL = 'http://localhost:8081/api/v1/players';

describe('authInterceptor', () => {
  let auth: jasmine.SpyObj<AuthService>;
  let next: jasmine.Spy<HttpHandlerFn>;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated', 'getAccessToken']);
    auth.getAccessToken.and.returnValue('test-token');
    next = jasmine
      .createSpy<HttpHandlerFn>('next')
      .and.returnValue(of() as Observable<HttpEvent<unknown>>);
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: RUNTIME_CONFIG, useValue: CONFIG },
      ],
    });
  });

  /** Runs the interceptor and returns the request that reached the next handler. */
  function send(url: string): HttpRequest<unknown> {
    const request = new HttpRequest('GET', url);
    TestBed.runInInjectionContext(() => authInterceptor(request, next));
    return next.calls.mostRecent().args[0];
  }

  it('attaches the token to a platform API request', () => {
    auth.isAuthenticated.and.returnValue(true);

    expect(send(API_URL).headers.get('Authorization')).toBe('Bearer test-token');
  });

  it('leaves a third-party URL alone', () => {
    auth.isAuthenticated.and.returnValue(true);

    expect(send('https://example.com/track').headers.has('Authorization')).toBeFalse();
    expect(auth.getAccessToken).not.toHaveBeenCalled();
  });

  it('forwards untouched when nobody is signed in', () => {
    auth.isAuthenticated.and.returnValue(false);

    expect(send(API_URL).headers.has('Authorization')).toBeFalse();
    expect(auth.getAccessToken).not.toHaveBeenCalled();
  });
});
