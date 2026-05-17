import { useMutation } from '@tanstack/react-query';
import { api } from './client';
import type { AuthResponse, User } from '../types';

export const authApi = {
  login: (email: string, password: string) =>
    api.post<AuthResponse>('/auth/login', { email, password }, { skipAuth: true }),

  me: () => api.get<User>('/auth/me'),

  register: (email: string, password: string, name: string) =>
    api.post<AuthResponse>('/auth/register', { email, password, name }, { skipAuth: true }),
};

export function useLogin() {
  return useMutation({
    mutationFn: ({ email, password }: { email: string; password: string }) =>
      authApi.login(email, password),
  });
}

export function useRegister() {
  return useMutation({
    mutationFn: ({ email, password, name }: { email: string; password: string; name: string }) =>
      authApi.register(email, password, name),
  });
}
