import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from './client';
import type { ApiResponse, PageResponse } from './types/common.types';

export type WebhookScopeType = 'ORG' | 'SPACE' | 'FOLDER' | 'RULE';

export type CircuitStatus = 'CLOSED' | 'OPEN';

export type WebhookAuthType =
  | 'NONE'
  | 'SIGNING_SECRET'
  | 'API_KEY'
  | 'BASIC_AUTH'
  | 'BEARER_TOKEN'
  | 'CUSTOM_HEADER'
  | 'OAUTH2_CLIENT_CREDENTIALS';

export interface WebhookAuthConfig {
  headerName?: string;
  apiKey?: string;
  username?: string;
  password?: string;
  token?: string;
  secret?: string;
  headerValue?: string;
  tokenUrl?: string;
  clientId?: string;
  clientSecret?: string;
  scope?: string;
}

export interface WebhookRetryConfig {
  initialDelaySeconds?: number;
  backoffMultiplier?: number;
  maxAttempts?: number;
  retryOnStatusCodes?: number[];
}

export interface Webhook {
  id: string;
  url: string;
  events: string[];
  secret: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  // Enhanced fields
  scopeType: WebhookScopeType;
  scopeId: string;
  name?: string;
  circuitStatus: CircuitStatus;
  consecutiveFailures: number;
  createdBy?: string;
  authType?: WebhookAuthType;
  authConfig?: WebhookAuthConfig;
  retryConfig?: WebhookRetryConfig;
}

export interface WebhookDelivery {
  id: string;
  webhookId: string;
  eventType: string;
  statusCode: number;
  success: boolean;
  deliveredAt: string;
  attemptCount?: number;
  lastError?: string;
  requestHeaders?: Record<string, string>;
  responseBody?: string;
  outboxEntryId?: string;
}

export interface WebhookOutboxEntry {
  id: string;
  webhookId: string;
  eventType: string;
  status: 'PENDING' | 'DELIVERING' | 'DELIVERED' | 'FAILED' | 'DLQ';
  attemptCount: number;
  nextAttemptAt?: string;
  lastAttemptAt?: string;
  lastStatusCode?: number;
  lastError?: string;
  createdAt: string;
}

export interface CreateWebhookRequest {
  url: string;
  events: string[];
  active: boolean;
  scopeType: WebhookScopeType;
  scopeId: string;
  name?: string;
  authType?: WebhookAuthType;
  authConfig?: WebhookAuthConfig;
  retryConfig?: WebhookRetryConfig;
}

export interface CreateWebhookResponse {
  webhook: Webhook;
  secretOnCreation: string;
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

export function useWebhook(id: string) {
  return useQuery({
    queryKey: ['webhooks', id],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<Webhook>>(`/webhooks/${id}`);
      return res.data.data;
    },
    enabled: !!id,
  });
}

export function useCreateWebhook() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreateWebhookRequest) => {
      const res = await apiClient.post<ApiResponse<CreateWebhookResponse>>('/webhooks', data);
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
    mutationFn: async ({ id, ...data }: { id: string; url?: string; events?: string[]; active?: boolean; name?: string; authType?: WebhookAuthType; authConfig?: WebhookAuthConfig; retryConfig?: WebhookRetryConfig }) => {
      const res = await apiClient.put<ApiResponse<Webhook>>(`/webhooks/${id}`, data);
      return res.data.data;
    },
    onSuccess: (_data, vars) => {
      queryClient.invalidateQueries({ queryKey: ['webhooks'] });
      queryClient.invalidateQueries({ queryKey: ['webhooks', vars.id] });
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

export function useWebhookDeliveries(webhookId: string, status?: string) {
  return useQuery({
    queryKey: ['webhook-deliveries', webhookId, status],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<PageResponse<WebhookDelivery>>>(
        `/webhooks/${webhookId}/deliveries`,
        { params: { page: 0, size: 200, ...(status ? { status } : {}) } },
      );
      return res.data.data;
    },
    enabled: !!webhookId,
  });
}

export function useWebhookOutbox(webhookId: string, status?: string) {
  return useQuery({
    queryKey: ['webhook-outbox', webhookId, status],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<WebhookOutboxEntry[]>>(
        `/webhooks/${webhookId}/outbox`,
        { params: status ? { status } : {} },
      );
      return res.data.data;
    },
    enabled: !!webhookId,
  });
}

export function useRotateSecret() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (webhookId: string) => {
      const res = await apiClient.post<ApiResponse<{ secret: string }>>(
        `/webhooks/${webhookId}/rotate-secret`,
      );
      return res.data.data;
    },
    onSuccess: (_data, webhookId) => {
      queryClient.invalidateQueries({ queryKey: ['webhooks', webhookId] });
    },
  });
}

export function useTestWebhook() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ webhookId }: { webhookId: string }) => {
      const res = await apiClient.post<ApiResponse<WebhookDelivery>>(
        `/webhooks/${webhookId}/test`,
      );
      return res.data.data;
    },
    onSuccess: (_data, vars) => {
      queryClient.invalidateQueries({ queryKey: ['webhook-deliveries', vars.webhookId] });
    },
  });
}

export function useResetCircuit() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (webhookId: string) => {
      const res = await apiClient.post<ApiResponse<Webhook>>(
        `/webhooks/${webhookId}/reset-circuit`,
      );
      return res.data.data;
    },
    onSuccess: (_data, webhookId) => {
      queryClient.invalidateQueries({ queryKey: ['webhooks', webhookId] });
      queryClient.invalidateQueries({ queryKey: ['webhooks'] });
    },
  });
}

export function useRetryOutboxEntry() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ webhookId, entryId }: { webhookId: string; entryId: string }) => {
      const res = await apiClient.post<ApiResponse<WebhookOutboxEntry>>(
        `/webhooks/${webhookId}/outbox/${entryId}/retry`,
      );
      return res.data.data;
    },
    onSuccess: (_data, vars) => {
      queryClient.invalidateQueries({ queryKey: ['webhook-outbox', vars.webhookId] });
      queryClient.invalidateQueries({ queryKey: ['webhook-deliveries', vars.webhookId] });
    },
  });
}

export interface DeliveryTimelineEntry {
  date: string;
  success: number;
  failed: number;
}

export interface WebhookMetrics {
  webhookId: string;
  period: string;
  successRate: number;
  totalDeliveries: number;
  successDeliveries: number;
  failedDeliveries: number;
  dlqDepth: number;
  avgLatencyMs: number | null;
  deliveryTimeline: DeliveryTimelineEntry[];
}

export function useWebhookMetrics(webhookId: string) {
  return useQuery({
    queryKey: ['webhooks', webhookId, 'metrics'],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<WebhookMetrics>>(`/webhooks/${webhookId}/metrics`);
      return res.data.data;
    },
    staleTime: 60_000,
    enabled: !!webhookId,
  });
}
