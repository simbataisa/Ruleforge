import { create } from 'zustand';
import type { User } from '@/api/types/auth.types';
import { useToastStore } from '@/stores/toast.store';

const SESSION_WARNING_BEFORE_MS = 5 * 60 * 1000; // 5 minutes

function parseJwtExpiry(token: string): number | null {
  try {
    const payload = token.split('.')[1];
    if (!payload) return null;
    const decoded = JSON.parse(atob(payload));
    return typeof decoded.exp === 'number' ? decoded.exp * 1000 : null;
  } catch {
    return null;
  }
}

let sessionExpiryTimer: ReturnType<typeof setTimeout> | null = null;
let sessionWarningTimer: ReturnType<typeof setTimeout> | null = null;

function clearSessionTimers() {
  if (sessionExpiryTimer) {
    clearTimeout(sessionExpiryTimer);
    sessionExpiryTimer = null;
  }
  if (sessionWarningTimer) {
    clearTimeout(sessionWarningTimer);
    sessionWarningTimer = null;
  }
}

function startSessionTimer(token: string) {
  clearSessionTimers();

  const expiresAt = parseJwtExpiry(token);
  if (!expiresAt) return;

  const now = Date.now();
  const timeUntilExpiry = expiresAt - now;

  if (timeUntilExpiry <= 0) {
    // Token already expired
    handleSessionExpired();
    return;
  }

  // Warning timer (5 min before expiry)
  const timeUntilWarning = timeUntilExpiry - SESSION_WARNING_BEFORE_MS;
  if (timeUntilWarning > 0) {
    sessionWarningTimer = setTimeout(() => {
      useToastStore.getState().addToast({
        type: 'warning',
        title: 'Session expiring soon. Save your work.',
        duration: 30000,
      });
    }, timeUntilWarning);
  }

  // Expiry timer
  sessionExpiryTimer = setTimeout(() => {
    handleSessionExpired();
  }, timeUntilExpiry);
}

function handleSessionExpired() {
  clearSessionTimers();
  localStorage.removeItem('ruleforge_token');
  useAuthStore.setState({ token: null, user: null });
  useToastStore.getState().addToast({
    type: 'error',
    title: 'Session expired. Please log in again.',
  });
  window.location.href = '/login';
}

interface AuthState {
  token: string | null;
  user: User | null;
  setToken: (token: string) => void;
  setUser: (user: User | null) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem('ruleforge_token'),
  user: null,
  setToken: (token) => {
    localStorage.setItem('ruleforge_token', token);
    set({ token });
    startSessionTimer(token);
  },
  setUser: (user) => set({ user }),
  logout: () => {
    clearSessionTimers();
    localStorage.removeItem('ruleforge_token');
    set({ token: null, user: null });
  },
}));

// Initialize session timer if a token already exists on load
const existingToken = localStorage.getItem('ruleforge_token');
if (existingToken) {
  startSessionTimer(existingToken);
}
