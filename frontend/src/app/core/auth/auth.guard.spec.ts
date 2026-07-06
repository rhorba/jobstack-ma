import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { roleGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('roleGuard', () => {
  let auth: AuthService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    auth = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
  });

  function runGuard(role: 'CANDIDATE' | 'EMPLOYER' | 'ADMIN') {
    return TestBed.runInInjectionContext(() => roleGuard(role)(null as any, null as any));
  }

  it('redirects to /login when no user is logged in', () => {
    const result = runGuard('CANDIDATE');
    expect(router.serializeUrl(result as any)).toBe('/login');
  });

  it('redirects to / when the logged-in user has the wrong role', () => {
    auth.currentUser.set({ role: 'EMPLOYER' });
    const result = runGuard('CANDIDATE');
    expect(router.serializeUrl(result as any)).toBe('/');
  });

  it('allows access when the logged-in user has the matching role', () => {
    auth.currentUser.set({ role: 'CANDIDATE' });
    const result = runGuard('CANDIDATE');
    expect(result).toBe(true);
  });
});
