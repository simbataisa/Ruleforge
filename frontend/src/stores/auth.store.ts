import { create } from 'zustand';
import type { User } from '@/api/types/auth.types';

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
  },
  setUser: (user) => set({ user }),
  logout: () => {
    localStorage.removeItem('ruleforge_token');
    set({ token: null, user: null });
  },
}));
