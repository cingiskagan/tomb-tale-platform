import { TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from './auth.service';
import { roleGuard } from './role.guard';
import { PlatformRole } from './auth.models';

describe('roleGuard', () => {
  let auth: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated', 'hasAnyRole']);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
  });

  /** Runs the guard against a route that requires `roles`, or requires nothing. */
  function run(roles?: PlatformRole[]) {
    const route = { data: roles ? { roles } : {} } as unknown as ActivatedRouteSnapshot;
    const state = {} as RouterStateSnapshot;
    return TestBed.runInInjectionContext(() => roleGuard(route, state));
  }

  it('sends a visitor who is not signed in to the login page', () => {
    auth.isAuthenticated.and.returnValue(false);

    expect(String(run([PlatformRole.PLATFORM_ADMIN]))).toBe('/login');
  });

  it('lets a holder of one required role through', () => {
    auth.isAuthenticated.and.returnValue(true);
    auth.hasAnyRole.and.returnValue(true);

    expect(run([PlatformRole.GAME_MASTER])).toBeTrue();
  });

  it('sends a signed-in user without the role to the dashboard', () => {
    auth.isAuthenticated.and.returnValue(true);
    auth.hasAnyRole.and.returnValue(false);

    expect(String(run([PlatformRole.PLATFORM_ADMIN]))).toBe('/dashboard');
  });

  it('allows a route that lists no roles, without asking for one', () => {
    auth.isAuthenticated.and.returnValue(true);

    expect(run()).toBeTrue();
    expect(auth.hasAnyRole).not.toHaveBeenCalled();
  });

  it('allows a route whose roles list is empty', () => {
    auth.isAuthenticated.and.returnValue(true);

    expect(run([])).toBeTrue();
    expect(auth.hasAnyRole).not.toHaveBeenCalled();
  });
});
