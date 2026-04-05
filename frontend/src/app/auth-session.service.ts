import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, finalize, map, shareReplay, tap } from 'rxjs';
import { environment } from '../environments/environment';
import { AuthResponse, UserInfoResponse } from './agri-mortgage.models';

interface StoredSession {
  username: string;
  role: string;
  expiresIn: number;
}

@Injectable({ providedIn: 'root' })
export class AuthSessionService {
  private readonly sessionSubject = new BehaviorSubject<StoredSession | null>(null);
  private readonly serverBaseUrl = environment.apiBaseUrl.replace(/\/$/, '').replace(/\/api\/v1$/, '');
  private refreshRequest$: Observable<string> | null = null;
  readonly session$ = this.sessionSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  get session(): StoredSession | null {
    return this.sessionSubject.value;
  }

  get isAuthenticated(): boolean {
    return !!this.session;
  }

  setSession(response: AuthResponse): void {
    const session: StoredSession = {
      username: response.username,
      role: response.role,
      expiresIn: response.expiresIn
    };
    this.sessionSubject.next(session);
  }

  setUserInfo(userInfo: UserInfoResponse): void {
    const session: StoredSession = {
      ...(this.session ?? { expiresIn: 0 }),
      username: userInfo.username,
      role: userInfo.role
    };
    this.sessionSubject.next(session);
  }

  clear(): void {
    this.sessionSubject.next(null);
  }

  refreshAccessToken(): Observable<string> {
    if (this.refreshRequest$) {
      return this.refreshRequest$;
    }

    this.refreshRequest$ = this.http
      .post<AuthResponse>(`${this.serverBaseUrl}/auth/refresh`, {}, { withCredentials: true })
      .pipe(
        tap((response) => this.setSession(response)),
        map(() => 'refreshed'),
        shareReplay(1),
        finalize(() => {
          this.refreshRequest$ = null;
        })
      );

    return this.refreshRequest$;
  }
}
