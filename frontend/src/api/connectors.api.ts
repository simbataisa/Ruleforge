import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from './client';
import type { ApiResponse, PageResponse, PageParams } from './types/common.types';

export type ConnectorType = 'JDBC' | 'HTTP' | 'REDIS';

export interface Connector {
  id: string;
  name: string;
  type: ConnectorType;
  description?: string;
  config: Record<string, unknown>;
  status: 'ACTIVE' | 'INACTIVE' | 'ERROR';
  lastTestedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateConnectorRequest {
  name: string;
  type: ConnectorType;
  description?: string;
  config: Record<string, unknown>;
}

export interface TestConnectionResult {
  success: boolean;
  message: string;
  latencyMs?: number;
}

export function useConnectors(params?: PageParams) {
  return useQuery({
    queryKey: ['connectors', params],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<PageResponse<Connector>>>(
        '/connectors',
        { params },
      );
      return res.data.data;
    },
  });
}

export function useConnector(id: string) {
  return useQuery({
    queryKey: ['connectors', id],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<Connector>>(`/connectors/${id}`);
      return res.data.data;
    },
    enabled: !!id,
  });
}

export function useCreateConnector() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreateConnectorRequest) => {
      const res = await apiClient.post<ApiResponse<Connector>>('/connectors', data);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['connectors'] });
    },
  });
}

export function useUpdateConnector() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }: { id: string; data: Partial<CreateConnectorRequest> }) => {
      const res = await apiClient.put<ApiResponse<Connector>>(`/connectors/${id}`, data);
      return res.data.data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['connectors'] });
      queryClient.invalidateQueries({ queryKey: ['connectors', variables.id] });
    },
  });
}

export function useDeleteConnector() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`/connectors/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['connectors'] });
    },
  });
}

export function useTestConnection() {
  return useMutation({
    mutationFn: async (id: string) => {
      const res = await apiClient.post<ApiResponse<TestConnectionResult>>(
        `/connectors/${id}/test`,
      );
      return res.data.data;
    },
  });
}
