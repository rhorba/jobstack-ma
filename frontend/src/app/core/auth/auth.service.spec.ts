import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { AuthResponse } from './auth.models';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const authResponse: AuthResponse = { accessToken: 'token-123', expiresInSeconds: 900, role: 'CANDIDATE' };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('starts with no access token and no current user', () => {
    expect(service.getAccessToken()).toBeNull();
    expect(service.currentUser()).toBeNull();
  });

  it('register posts credentials and role', () => {
    service.register('a@jobstack.ma', 'pw', 'EMPLOYER').subscribe();
    const req = httpMock.expectOne('/api/v1/auth/register');
    expect(req.request.body).toEqual({ email: 'a@jobstack.ma', password: 'pw', role: 'EMPLOYER' });
    req.flush(null);
  });

  it('login stores the access token and current user on success', () => {
    service.login('a@jobstack.ma', 'pw').subscribe();
    const req = httpMock.expectOne('/api/v1/auth/login');
    expect(req.request.withCredentials).toBe(true);
    req.flush(authResponse);

    expect(service.getAccessToken()).toBe('token-123');
    expect(service.currentUser()).toEqual({ role: 'CANDIDATE' });
  });

  it('refresh restores the session on success', () => {
    service.refresh().subscribe();
    httpMock.expectOne('/api/v1/auth/refresh').flush(authResponse);

    expect(service.getAccessToken()).toBe('token-123');
    expect(service.currentUser()).toEqual({ role: 'CANDIDATE' });
  });

  it('refresh clears the session and emits null on failure', () => {
    let result: AuthResponse | null | undefined;
    service.refresh().subscribe((res) => (result = res));
    httpMock.expectOne('/api/v1/auth/refresh').flush('no cookie', { status: 401, statusText: 'Unauthorized' });

    expect(result).toBeNull();
    expect(service.getAccessToken()).toBeNull();
    expect(service.currentUser()).toBeNull();
  });

  it('restoreSession delegates to refresh', () => {
    service.restoreSession().subscribe();
    httpMock.expectOne('/api/v1/auth/refresh').flush(authResponse);

    expect(service.getAccessToken()).toBe('token-123');
  });

  it('logout clears the session', () => {
    service.login('a@jobstack.ma', 'pw').subscribe();
    httpMock.expectOne('/api/v1/auth/login').flush(authResponse);
    expect(service.getAccessToken()).toBe('token-123');

    service.logout().subscribe();
    httpMock.expectOne('/api/v1/auth/logout').flush(null);

    expect(service.getAccessToken()).toBeNull();
    expect(service.currentUser()).toBeNull();
  });

  it('hasRole reflects the current user role', () => {
    expect(service.hasRole('CANDIDATE')).toBe(false);

    service.login('a@jobstack.ma', 'pw').subscribe();
    httpMock.expectOne('/api/v1/auth/login').flush(authResponse);

    expect(service.hasRole('CANDIDATE')).toBe(true);
    expect(service.hasRole('EMPLOYER')).toBe(false);
  });
});
