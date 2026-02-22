import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from './client';
import type {
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  User,
} from './types/auth.types';
import type { ApiResponse } from './types/common.types';
import { useAuthStore } from '@/stores/auth.store';

export function useLogin() {
  const setToken = useAuthStore((s) => s.setToken);
  const setUser = useAuthStore((s) => s.setUser);
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: LoginRequest) => {
      const res = await apiClient.post<ApiResponse<AuthResponse>>(
        '/auth/login',
        data,
      );
      return res.data.data;
    },
    onSuccess: (data) => {
      setToken(data.accessToken);
      setUser(data.user);
      queryClient.invalidateQueries({ queryKey: ['currentUser'] });
    },
  });
}

export function useRegister() {
  const setToken = useAuthStore((s) => s.setToken);
  const setUser = useAuthStore((s) => s.setUser);
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: RegisterRequest) => {
      const res = await apiClient.post<ApiResponse<AuthResponse>>(
        '/auth/register',
        data,
      );
      return res.data.data;
    },
    onSuccess: (data) => {
      setToken(data.accessToken);
      setUser(data.user);
      queryClient.invalidateQueries({ queryKey: ['currentUser'] });
    },
  });
}

export function useCurrentUser() {
  const token = useAuthStore((s) => s.token);
  const setUser = useAuthStore((s) => s.setUser);

  return useQuery({
    queryKey: ['currentUser'],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<User>>('/users/me');
      setUser(res.data.data);
      return res.data.data;
    },
    enabled: !!token,
    retry: false,
  });
}
