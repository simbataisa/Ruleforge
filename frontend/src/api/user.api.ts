import { useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from './client';
import type { User } from './types/auth.types';
import type { ApiResponse } from './types/common.types';
import { useAuthStore } from '@/stores/auth.store';

export function useUpdateProfile() {
  const setUser = useAuthStore((s) => s.setUser);
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: { fullName: string }) => {
      const res = await apiClient.put<ApiResponse<User>>('/users/me', data);
      return res.data.data;
    },
    onSuccess: (data) => {
      setUser(data);
      queryClient.invalidateQueries({ queryKey: ['currentUser'] });
    },
  });
}

export function useChangePassword() {
  return useMutation({
    mutationFn: async (data: { currentPassword: string; newPassword: string }) => {
      const res = await apiClient.post<ApiResponse<void>>(
        '/users/me/change-password',
        data,
      );
      return res.data;
    },
  });
}
