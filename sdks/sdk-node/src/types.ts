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
