import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Role } from '@ui/types';

interface AuthState {
  token: string | null;
  email: string | null;
  role: Role | null;
  isAuthenticated: boolean;
  setAuth: (token: string, email: string, role: Role) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      email: null,
      role: null,
      isAuthenticated: false,

      setAuth: (token, email, role) => {
        localStorage.setItem('bakelivery_token', token);
        localStorage.setItem('bakelivery_role', role);
        set({ token, email, role, isAuthenticated: true });
      },

      logout: () => {
        localStorage.removeItem('bakelivery_token');
        localStorage.removeItem('bakelivery_role');
        set({ token: null, email: null, role: null, isAuthenticated: false });
      },
    }),
    { name: 'bakelivery-shop-auth' },
  ),
);
