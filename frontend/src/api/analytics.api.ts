import { useQuery } from '@tanstack/react-query';
import { apiClient } from './client';
import type {
  DecisionLogDto,
  RuleStatsDto,
  DashboardMetricsDto,
  DecisionQueryParams,
} from './types/analytics.types';
import type { ApiResponse, PageResponse } from './types/common.types';

async function fetchDashboardMetrics(
  orgId: string,
  hours: number,
): Promise<DashboardMetricsDto> {
  const res = await apiClient.get<ApiResponse<DashboardMetricsDto>>(
    '/analytics/dashboard',
    { params: { orgId, hours } },
  );
  return res.data.data;
}

async function fetchRuleStats(
  ruleId: string,
  days: number,
): Promise<RuleStatsDto> {
  const res = await apiClient.get<ApiResponse<RuleStatsDto>>(
    `/analytics/rules/${ruleId}/stats`,
    { params: { days } },
  );
  return res.data.data;
}

async function fetchDecisionLogs(
  params: DecisionQueryParams,
): Promise<PageResponse<DecisionLogDto>> {
  const res = await apiClient.get<ApiResponse<PageResponse<DecisionLogDto>>>(
    '/analytics/decisions',
    { params },
  );
  return res.data.data;
}

export function useDashboardMetrics(orgId: string, hours = 24) {
  return useQuery({
    queryKey: ['analytics', 'dashboard', orgId, hours],
    queryFn: () => fetchDashboardMetrics(orgId, hours),
    enabled: !!orgId,
  });
}

export function useRuleStats(ruleId: string, days = 30) {
  return useQuery({
    queryKey: ['analytics', 'rules', ruleId, 'stats', days],
    queryFn: () => fetchRuleStats(ruleId, days),
    enabled: !!ruleId,
  });
}

export function useDecisionLogs(params: DecisionQueryParams) {
  return useQuery({
    queryKey: ['analytics', 'decisions', params],
    queryFn: () => fetchDecisionLogs(params),
  });
}
