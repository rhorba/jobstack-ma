import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { UserRole } from './auth.models';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.currentUser()) {
    return true;
  }
  return router.parseUrl('/login');
};

export function roleGuard(role: UserRole): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (!auth.currentUser()) {
      return router.parseUrl('/login');
    }
    if (!auth.hasRole(role)) {
      return router.parseUrl('/');
    }
    return true;
  };
}
