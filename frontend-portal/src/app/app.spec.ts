import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { provideRouter } from '@angular/router';
import { AuthService } from './core/auth';

/**
 * Basic test suite for the root App component.
 */
describe('App', () => {
  let mockAuthService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    mockAuthService = jasmine.createSpyObj('AuthService', ['initializeAuth']);
    mockAuthService.initializeAuth.and.returnValue(Promise.resolve());

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should initialize auth on init', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    expect(mockAuthService.initializeAuth).toHaveBeenCalled();
  });
});
