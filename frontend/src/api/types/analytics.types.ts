export interface DecisionLogDto {
  id: string;
  ruleId: string;
  ruleName: string;
  ruleType: string;
  orgId: string;
  executionTimeMs: number;
  status: 'SUCCESS' | 'ERROR';
  timestamp: string;
  correlationId?: string;
}

export interface RuleStatsDto {
  ruleId: string;
  ruleName: string;
  totalExecutions: number;
  avgExecutionTimeMs: number;
  errorRate: number;
  lastExecuted?: string;
}

export interface DashboardMetricsDto {
  totalDecisions: number;
  decisionsPerMinute: number;
  avgExecutionTimeMs: number;
  errorRate: number;
  topRules: RuleStatsDto[];
}

export interface DecisionQueryParams {
  orgId?: string;
  ruleId?: string;
  from?: string;
  to?: string;
  status?: string;
  search?: string;
  minLatency?: number;
  maxLatency?: number;
  page?: number;
  size?: number;
}

export interface TimelinePointDto {
  timestamp: string;
  count: number;
}

export interface TypeDistributionItemDto {
  type: string;
  count: number;
  percentage: number;
}

export interface LatencyPercentilesDto {
  p50: number;
  p75: number;
  p90: number;
  p95: number;
  p99: number;
}

export interface PassFailPointDto {
  timestamp: string;
  passed: number;
  failed: number;
}
