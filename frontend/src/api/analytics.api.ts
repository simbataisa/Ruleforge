import { useQuery } from '@tanstack/react-query';
import { apiClient } from './client';
import type {
  DecisionLogDto,
  RuleStatsDto,
  DashboardMetricsDto,
  DecisionQueryParams,
  TimelinePointDto,
  TypeDistributionItemDto,
  LatencyPercentilesDto,
  PassFailPointDto,
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

export function useExecutionTimeline(hours = 24, interval = 'hour') {
  return useQuery({
    queryKey: ['analytics', 'timeline', hours, interval],
    queryFn: async () => {
      const res = await apiClient.get<TimelinePointDto[]>(
        '/analytics/timeline',
        { params: { hours, interval } },
      );
      return res.data;
    },
  });
}

export function useTypeDistribution(hours = 24) {
  return useQuery({
    queryKey: ['analytics', 'type-distribution', hours],
    queryFn: async () => {
      const res = await apiClient.get<TypeDistributionItemDto[]>(
        '/analytics/type-distribution',
        { params: { hours } },
      );
      return res.data;
    },
  });
}

export function useLatencyPercentiles(hours = 24) {
  return useQuery({
    queryKey: ['analytics', 'latency-percentiles', hours],
    queryFn: async () => {
      const res = await apiClient.get<LatencyPercentilesDto>(
        '/analytics/latency-percentiles',
        { params: { hours } },
      );
      return res.data;
    },
  });
}

export function usePassFail(hours = 24, interval = 'hour') {
  return useQuery({
    queryKey: ['analytics', 'pass-fail', hours, interval],
    queryFn: async () => {
      const res = await apiClient.get<PassFailPointDto[]>(
        '/analytics/pass-fail',
        { params: { hours, interval } },
      );
      return res.data;
    },
  });
}

// A/B test results
export function useAbTestResults(testId: string) {
  return useQuery({
    queryKey: ['ab-test', testId, 'results'],
    queryFn: async () => {
      const res = await apiClient.get(`/analytics/ab-tests/${testId}/results`);
      return res.data;
    },
    enabled: !!testId,
  });
}

export function useAbTestSignificance(testId: string) {
  return useQuery({
    queryKey: ['ab-test', testId, 'significance'],
    queryFn: async () => {
      const res = await apiClient.get(`/analytics/ab-tests/${testId}/significance`);
      return res.data;
    },
    enabled: !!testId,
  });
}
