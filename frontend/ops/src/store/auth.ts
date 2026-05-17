import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Role } from '@ui/types';

interface AuthState {
  token: string | null;
  email: string | null;
  role: Role | null;
  name: string | null;
  isAuthenticated: boolean;
  setAuth: (token: string, email: string, role: Role, name?: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      email: null,
      role: null,
      name: null,
      isAuthenticated: false,

      setAuth: (token, email, role, name) => {
        localStorage.setItem('bakelivery_token', token);
        set({ token, email, role, name: name ?? null, isAuthenticated: true });
      },

      logout: () => {
        localStorage.removeItem('bakelivery_token');
        set({ token: null, email: null, role: null, name: null, isAuthenticated: false });
      },
    }),
    { name: 'bakelivery-ops-auth' },
  ),
);
