import { TestBed } from '@angular/core/testing';
import { OAuthService, OAuthEvent } from 'angular-oauth2-oidc';
import { Subject } from 'rxjs';
import { RUNTIME_CONFIG, RuntimeConfig } from '../config';
import { AuthService } from './auth.service';
import { PlatformRole } from './auth.models';

const CONFIG: RuntimeConfig = {
  issuerUri: 'http://localhost:8080/realms/tombtale',
  clientId: 'test-client',
  apiBaseUrl: 'http://localhost:8081',
};

describe('AuthService', () => {
  let oauth: jasmine.SpyObj<OAuthService>;
  let service: AuthService;

  beforeEach(() => {
    oauth = jasmine.createSpyObj<OAuthService>(
      'OAuthService',
      ['configure', 'setupAutomaticSilentRefresh', 'getIdentityClaims', 'hasValidAccessToken'],
      { events: new Subject<OAuthEvent>().asObservable() },
    );
    TestBed.configureTestingModule({
      providers: [
        { provide: OAuthService, useValue: oauth },
        { provide: RUNTIME_CONFIG, useValue: CONFIG },
      ],
    });
    service = TestBed.inject(AuthService);
  });

  /** Sets what the ID token holds. Takes `unknown`, so `null` is legal here. */
  function idTokenHolds(claims: unknown): void {
    oauth.getIdentityClaims.and.returnValue(claims as Record<string, unknown>);
  }

  describe('getUserProfile', () => {
    it('reads the role names out of the roles list', () => {
      idTokenHolds({ sub: 'u-1', roles: ['player', 'game_master'] });

      expect(service.getUserProfile()?.roles).toEqual([
        PlatformRole.PLAYER,
        PlatformRole.GAME_MASTER,
      ]);
    });

    it("drops Keycloak's own default roles", () => {
      idTokenHolds({
        sub: 'u-1',
        roles: ['offline_access', 'uma_authorization', 'default-roles-tombtale', 'player'],
      });

      expect(service.getUserProfile()?.roles).toEqual([PlatformRole.PLAYER]);
    });

    it('reports no roles when the claim is a map, the shape Zitadel used', () => {
      idTokenHolds({ sub: 'u-1', roles: { platform_admin: { orgId: 'tombtale' } } });

      expect(service.getUserProfile()?.roles).toEqual([]);
    });

    it('drops a role name the platform does not know', () => {
      idTokenHolds({ sub: 'u-1', roles: ['player', 'enemy'] });

      expect(service.getUserProfile()?.roles).toEqual([PlatformRole.PLAYER]);
    });

    it('reports no roles when the claim is a string', () => {
      idTokenHolds({ sub: 'u-1', roles: 'player, game_master' });

      expect(service.getUserProfile()?.roles).toEqual([]);
    });

    it('reports no roles when the token carries none', () => {
      idTokenHolds({ sub: 'u-1' });

      expect(service.getUserProfile()?.roles).toEqual([]);
    });

    it('returns no profile when there is no token', () => {
      idTokenHolds(null);

      expect(service.getUserProfile()).toBeNull();
    });

    it('returns no profile when the subject is missing', () => {
      idTokenHolds({ roles: ['player'] });

      expect(service.getUserProfile()).toBeNull();
    });

    it('returns no profile when the subject is blank', () => {
      idTokenHolds({ sub: '', roles: ['platform_admin'] });

      expect(service.getUserProfile()).toBeNull();
    });

    it('keeps a name out of the profile when it is not a string', () => {
      idTokenHolds({ sub: 'u-1', name: 42 });

      const profile = service.getUserProfile();

      expect(profile).not.toBeNull();
      expect(profile?.name).toBeUndefined();
    });
  });

  describe('hasAnyRole', () => {
    it('is true when the user holds one of the required roles', () => {
      idTokenHolds({ sub: 'u-1', roles: ['player'] });

      expect(service.hasAnyRole([PlatformRole.PLAYER, PlatformRole.PLATFORM_ADMIN])).toBeTrue();
    });

    it('is false when the user holds none of them', () => {
      idTokenHolds({ sub: 'u-1', roles: ['player'] });

      expect(service.hasAnyRole([PlatformRole.PLATFORM_ADMIN])).toBeFalse();
    });

    it('is false when nobody is signed in', () => {
      idTokenHolds(null);

      expect(service.hasAnyRole([PlatformRole.PLAYER])).toBeFalse();
    });
  });
});
