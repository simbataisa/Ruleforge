export interface SolveRequest {
  input: Record<string, unknown>;
}

export interface SolveResponse {
  output: Record<string, unknown>;
  matchedRules: unknown[];
  executionTimeMs: number;
}

export interface RuleForgeClientOptions {
  baseUrl: string;
  apiKey?: string;
  jwtToken?: string;
}

export interface MetricsResponse {
  totalExecutions: number;
  avgLatencyMs: number;
  [key: string]: unknown;
}

export interface RuleStats {
  hitCount: number;
  avgLatencyMs: number;
  errorRate: number;
  [key: string]: unknown;
}

export interface DecisionLog {
  id: string;
  ruleId: string;
  timestamp: string;
  [key: string]: unknown;
}

export interface Schedule {
  id: string;
  ruleId: string;
  cronExpression: string;
  enabled: boolean;
  [key: string]: unknown;
}

export interface CreateScheduleRequest {
  ruleId: string;
  cronExpression: string;
  input?: Record<string, unknown>;
  enabled?: boolean;
}

export interface ExplainRuleResponse {
  summary: string;
  conditions: string[];
  actions: string[];
  [key: string]: unknown;
}

export interface GenerateRuleResponse {
  id: string;
  name: string;
  type: string;
  [key: string]: unknown;
}

export interface GenerateTestDataResponse {
  testData: Record<string, unknown>[];
}
