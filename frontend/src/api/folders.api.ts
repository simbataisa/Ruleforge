import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from './client';
import type {
  Folder,
  CreateFolderRequest,
  UpdateFolderRequest,
} from './types/folder.types';
import type { ApiResponse } from './types/common.types';

export function useFolders(parentId?: string) {
  return useQuery({
    queryKey: ['folders', { parentId }],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<Folder[]>>('/folders', {
        params: parentId ? { parentId } : undefined,
      });
      return res.data.data;
    },
  });
}

export function useFolder(id: string) {
  return useQuery({
    queryKey: ['folders', id],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<Folder>>(`/folders/${id}`);
      return res.data.data;
    },
    enabled: !!id,
  });
}

export function useCreateFolder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreateFolderRequest) => {
      const res = await apiClient.post<ApiResponse<Folder>>('/folders', data);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['folders'] });
    },
  });
}

export function useUpdateFolder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      id,
      data,
    }: {
      id: string;
      data: UpdateFolderRequest;
    }) => {
      const res = await apiClient.put<ApiResponse<Folder>>(
        `/folders/${id}`,
        data,
      );
      return res.data.data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['folders'] });
      queryClient.invalidateQueries({ queryKey: ['folders', variables.id] });
    },
  });
}

export function useDeleteFolder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`/folders/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['folders'] });
    },
  });
}
