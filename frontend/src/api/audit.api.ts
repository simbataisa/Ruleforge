import { useQuery } from '@tanstack/react-query';
import { apiClient } from './client';
import type { ApiResponse, PageResponse } from './types/common.types';

export interface AuditEvent {
  id: string;
  orgId: string;
  eventType: string;
  actorId: string;
  resourceType: string;
  resourceId: string;
  changes: Record<string, unknown>;
  metadata: Record<string, unknown>;
  createdAt: string;
}

export function useAuditLogs(page: number, size: number, eventType?: string, search?: string) {
  return useQuery({
    queryKey: ['audit-logs', page, size, eventType, search],
    queryFn: async () => {
      const params: Record<string, unknown> = { page, size };
      if (eventType) params.eventType = eventType;
      if (search) params.search = search;
      const res = await apiClient.get<ApiResponse<PageResponse<AuditEvent>>>('/audit', { params });
      return res.data.data;
    },
  });
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
