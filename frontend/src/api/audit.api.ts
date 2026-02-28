import { useQuery } from '@tanstack/react-query';
import { apiClient } from './client';
import type { ApiResponse, PageResponse } from './types/common.types';

export interface AuditEvent {
  id: string;
  orgId: string;
  eventType: string;
  resourceType: string;
  resourceId: string;
  resourceName?: string;
  actorId: string;
  actorName?: string;
  actorType?: string;
  spaceId?: string;
  changes?: Record<string, unknown>;
  metadata?: Record<string, unknown>;
  createdAt: string;
}

export interface AuditEventParams {
  eventType?: string;
  resourceType?: string;
  actorId?: string;
  search?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export function useAuditEvents(params: AuditEventParams, enabled = true) {
  return useQuery({
    queryKey: ['audit-events', params],
    queryFn: async () => {
      const queryParams: Record<string, unknown> = {};
      if (params.page != null) queryParams.page = params.page;
      if (params.size != null) queryParams.size = params.size;
      if (params.eventType) queryParams.eventType = params.eventType;
      if (params.resourceType) queryParams.resourceType = params.resourceType;
      if (params.actorId) queryParams.actorId = params.actorId;
      if (params.search) queryParams.search = params.search;
      if (params.from) queryParams.from = params.from;
      if (params.to) queryParams.to = params.to;
      const res = await apiClient.get<ApiResponse<PageResponse<AuditEvent>>>('/audit', { params: queryParams });
      return res.data.data;
    },
    enabled,
  });
}

export function useAuditLogs(page: number, size: number, eventType?: string, search?: string) {
  return useAuditEvents({ page, size, eventType, search });
}

export function useResourceAuditLogs(
  resourceType: string,
  resourceId: string,
  page: number,
  size: number,
) {
  return useQuery({
    queryKey: ['audit-logs', 'resource', resourceType, resourceId, page, size],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<PageResponse<AuditEvent>>>(
        `/audit/resources/${resourceType}/${resourceId}`,
        { params: { page, size } },
      );
      return res.data.data;
    },
    enabled: !!resourceType && !!resourceId,
  });
}

export function exportAuditEventsCsv(params: Omit<AuditEventParams, 'page' | 'size'>): void {
  const baseUrl = apiClient.defaults.baseURL || '/v1';
  const searchParams = new URLSearchParams();
  if (params.eventType) searchParams.set('eventType', params.eventType);
  if (params.resourceType) searchParams.set('resourceType', params.resourceType);
  if (params.actorId) searchParams.set('actorId', params.actorId);
  if (params.search) searchParams.set('search', params.search);
  if (params.from) searchParams.set('from', params.from);
  if (params.to) searchParams.set('to', params.to);
  searchParams.set('format', 'csv');

  const url = `${baseUrl}/audit/export?${searchParams.toString()}`;
  const a = document.createElement('a');
  a.href = url;
  a.download = 'audit-events.csv';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
}
