import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from './client';
import type { ApiResponse, PageResponse } from './types/common.types';

export interface Webhook {
  id: string;
  spaceId: string;
  url: string;
  events: string[];
  secret: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface WebhookDelivery {
  id: string;
  webhookId: string;
  eventType: string;
  statusCode: number;
  success: boolean;
  createdAt: string;
}

export function useWebhooks() {
  return useQuery({
    queryKey: ['webhooks'],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<Webhook[]>>('/webhooks');
      return res.data.data;
    },
  });
}

export function useCreateWebhook() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: { url: string; events: string[]; active: boolean }) => {
      const res = await apiClient.post<ApiResponse<Webhook>>('/webhooks', data);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['webhooks'] });
    },
  });
}

export function useUpdateWebhook() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...data }: { id: string; url: string; events: string[]; active: boolean }) => {
      const res = await apiClient.put<ApiResponse<Webhook>>(`/webhooks/${id}`, data);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['webhooks'] });
    },
  });
}

export function useDeleteWebhook() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`/webhooks/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['webhooks'] });
    },
  });
}

export function useWebhookDeliveries(webhookId: string, page: number, size: number) {
  return useQuery({
    queryKey: ['webhook-deliveries', webhookId, page, size],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<PageResponse<WebhookDelivery>>>(
        `/webhooks/${webhookId}/deliveries`,
        { params: { page, size } },
      );
      return res.data.data;
    },
    enabled: !!webhookId,
  });
}
