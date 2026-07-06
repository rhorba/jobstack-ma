export type UserRole = 'CANDIDATE' | 'EMPLOYER' | 'ADMIN';

export interface AuthResponse {
  accessToken: string;
  expiresInSeconds: number;
  role: UserRole;
}

export interface CurrentUser {
  role: UserRole;
}
