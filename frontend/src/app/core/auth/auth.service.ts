import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable, catchError, of, tap } from 'rxjs';
import { AuthResponse, CurrentUser, UserRole } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  // Access token lives in memory only (never localStorage) — cleared on full page reload,
  // restored via a silent /auth/refresh call using the HttpOnly refresh cookie.
  private accessToken: string | null = null;

  readonly currentUser = signal<CurrentUser | null>(null);

  constructor(private http: HttpClient) {}

  getAccessToken(): string | null {
    return this.accessToken;
  }

  register(email: string, password: string, role: 'CANDIDATE' | 'EMPLOYER'): Observable<void> {
    return this.http.post<void>('/api/v1/auth/register', { email, password, role });
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/v1/auth/login', { email, password }, { withCredentials: true })
      .pipe(tap((res) => this.applyAuthResponse(res)));
  }

  refresh(): Observable<AuthResponse | null> {
    return this.http.post<AuthResponse>('/api/v1/auth/refresh', {}, { withCredentials: true }).pipe(
      tap((res) => this.applyAuthResponse(res)),
      catchError(() => {
        this.clearSession();
        return of(null);
      })
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/v1/auth/logout', {}, { withCredentials: true }).pipe(tap(() => this.clearSession()));
  }

  /** Call once at app startup to silently restore a session from the refresh cookie, if any. */
  restoreSession(): Observable<AuthResponse | null> {
    return this.refresh();
  }

  hasRole(role: UserRole): boolean {
    return this.currentUser()?.role === role;
  }

  private applyAuthResponse(res: AuthResponse): void {
    this.accessToken = res.accessToken;
    this.currentUser.set({ role: res.role });
  }

  private clearSession(): void {
    this.accessToken = null;
    this.currentUser.set(null);
  }
}
