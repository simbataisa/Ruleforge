import { useQuery } from '@tanstack/react-query';
import { apiClient } from './client';
import type {
  DecisionLogDto,
  RuleStatsDto,
  DashboardMetricsDto,
  DecisionQueryParams,
} from './types/analytics.types';
import type { PageResponse } from './types/common.types';

async function fetchDashboardMetrics(
  hours: number,
): Promise<DashboardMetricsDto> {
  // Backend derives orgId from JWT TenantContext — no need to send it
  const res = await apiClient.get<DashboardMetricsDto>(
    '/analytics/dashboard',
    { params: { hours } },
  );
  return res.data;
}

async function fetchRuleStats(
  ruleId: string,
  days: number,
): Promise<RuleStatsDto> {
  const res = await apiClient.get<RuleStatsDto>(
    `/analytics/rules/${ruleId}/stats`,
    { params: { days } },
  );
  return res.data;
}

async function fetchDecisionLogs(
  params: DecisionQueryParams,
): Promise<PageResponse<DecisionLogDto>> {
  const res = await apiClient.get<PageResponse<DecisionLogDto>>(
    '/analytics/decisions',
    { params },
  );
  return res.data;
}

export function useDashboardMetrics(hours = 24) {
  return useQuery({
    queryKey: ['analytics', 'dashboard', hours],
    queryFn: () => fetchDashboardMetrics(hours),
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
