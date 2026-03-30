import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { AuthResponse, UserInfoResponse } from './agri-mortgage.models';

const STORAGE_KEY = 'agri-mortgage-auth-session';

interface StoredSession {
  accessToken: string;
  refreshToken: string;
  username: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class AuthSessionService {
  private readonly sessionSubject = new BehaviorSubject<StoredSession | null>(this.readSession());
  readonly session$ = this.sessionSubject.asObservable();

  get session(): StoredSession | null {
    return this.sessionSubject.value;
  }

  get accessToken(): string | null {
    return this.session?.accessToken ?? null;
  }

  get isAuthenticated(): boolean {
    return !!this.accessToken;
  }

  setSession(response: AuthResponse): void {
    const session: StoredSession = {
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      username: response.username,
      role: response.role
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    this.sessionSubject.next(session);
  }

  setUserInfo(userInfo: UserInfoResponse): void {
    if (!this.session) {
      return;
    }
    const session: StoredSession = {
      ...this.session,
      username: userInfo.username,
      role: userInfo.role
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    this.sessionSubject.next(session);
  }

  clear(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.sessionSubject.next(null);
  }

  private readSession(): StoredSession | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as StoredSession;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }
}
