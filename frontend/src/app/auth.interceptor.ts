import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthSessionService } from './auth-session.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(AuthSessionService);
  const token = session.accessToken;
  const isAuthRequest = request.url.includes('/auth/login') || request.url.includes('/auth/refresh');

  if (!token || isAuthRequest) {
    return next(request);
  }

  const authorizedRequest = request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(authorizedRequest).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || !session.refreshToken) {
        return throwError(() => error);
      }

      return session.refreshAccessToken().pipe(
        switchMap((nextToken) =>
          next(
            request.clone({
              setHeaders: {
                Authorization: `Bearer ${nextToken}`
              }
            })
          )
        ),
        catchError((refreshError) => {
          session.clear();
          return throwError(() => refreshError);
        })
      );
    })
  );
};
