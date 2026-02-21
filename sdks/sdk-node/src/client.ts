import {
  RuleForgeClientOptions,
  SolveResponse,
  MetricsResponse,
  RuleStats,
  DecisionLog,
  Schedule,
  CreateScheduleRequest,
  ExplainRuleResponse,
  GenerateRuleResponse,
  GenerateTestDataResponse,
} from './types';

/**
 * RuleForge API client for Node.js (>=18) using the built-in fetch API.
 *
 * @example
 * ```ts
 * const client = new RuleForgeClient({ baseUrl: 'https://api.ruleforge.io', apiKey: 'your-key' });
 * const result = await client.solve('loan-eligibility', { age: 30, income: 75000 });
 * ```
 */
export class RuleForgeClient {
  private readonly baseUrl: string;
  private readonly apiKey?: string;
  private readonly jwtToken?: string;

  constructor(options: RuleForgeClientOptions) {
    if (!options.baseUrl) {
      throw new Error('baseUrl is required');
    }
    this.baseUrl = options.baseUrl.replace(/\/+$/, '');
    this.apiKey = options.apiKey;
    this.jwtToken = options.jwtToken;
  }

  /**
   * Executes a single rule against the provided input facts.
   *
   * @param ruleIdOrAlias - The rule UUID or its unique alias
   * @param input - Key/value map of input facts
   * @returns The solver response containing output, matched rules, and execution time
   */
  async solve(ruleIdOrAlias: string, input: Record<string, unknown>): Promise<SolveResponse> {
    const url = `${this.baseUrl}/v1/solve/${ruleIdOrAlias}`;
    const response = await this.post(url, { input });
    return response as SolveResponse;
  }

  /**
   * Executes a rule against multiple input records in a single request.
   *
   * @param ruleIdOrAlias - The rule UUID or its unique alias
   * @param inputs - Array of input fact maps
   * @returns Array of solver responses
   */
  async solveBulk(ruleIdOrAlias: string, inputs: Record<string, unknown>[]): Promise<SolveResponse[]> {
    const url = `${this.baseUrl}/v1/solve/bulk/${ruleIdOrAlias}`;
    const response = await this.post(url, { inputs });
    return response as SolveResponse[];
  }

  /**
   * Lists all rules in a given space.
   *
   * @param spaceId - The UUID of the space
   * @returns Paginated list of rules
   */
  async listRules(spaceId: string): Promise<unknown[]> {
    const url = `${this.baseUrl}/v1/spaces/${spaceId}/rules`;
    const data = await this.get(url);
    const page = data as { content?: unknown[] };
    return page.content ?? [];
  }

  /**
   * Retrieves a single rule by its UUID.
   *
   * @param ruleId - The rule UUID
   * @returns Rule details
   */
  async getRule(ruleId: string): Promise<unknown> {
    const url = `${this.baseUrl}/v1/rules/${ruleId}`;
    return this.get(url);
  }

  private buildHeaders(): Record<string, string> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };
    if (this.apiKey) {
      headers['X-API-Key'] = this.apiKey;
    } else if (this.jwtToken) {
      headers['Authorization'] = `Bearer ${this.jwtToken}`;
    }
    return headers;
  }

  private async post(url: string, body: unknown): Promise<unknown> {
    const response = await fetch(url, {
      method: 'POST',
      headers: this.buildHeaders(),
      body: JSON.stringify(body),
    });
    await this.assertOk(response);
    return response.json();
  }

  private async get(url: string): Promise<unknown> {
    const response = await fetch(url, {
      method: 'GET',
      headers: this.buildHeaders(),
    });
    await this.assertOk(response);
    return response.json();
  }

  // --- Analytics ---

  async getMetrics(): Promise<MetricsResponse> {
    const url = `${this.baseUrl}/v1/analytics/metrics`;
    return this.get(url) as Promise<MetricsResponse>;
  }

  async getRuleStats(ruleId: string): Promise<RuleStats> {
    const url = `${this.baseUrl}/v1/analytics/rules/${ruleId}/stats`;
    return this.get(url) as Promise<RuleStats>;
  }

  async getDecisionLogs(ruleId?: string, page = 0, size = 20): Promise<DecisionLog[]> {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (ruleId) params.set('ruleId', ruleId);
    const url = `${this.baseUrl}/v1/analytics/decision-logs?${params}`;
    return this.get(url) as Promise<DecisionLog[]>;
  }

  // --- Schedules ---

  async listSchedules(): Promise<Schedule[]> {
    const url = `${this.baseUrl}/v1/schedules`;
    return this.get(url) as Promise<Schedule[]>;
  }

  async createSchedule(schedule: CreateScheduleRequest): Promise<Schedule> {
    const url = `${this.baseUrl}/v1/schedules`;
    return this.post(url, schedule) as Promise<Schedule>;
  }

  async deleteSchedule(scheduleId: string): Promise<void> {
    const url = `${this.baseUrl}/v1/schedules/${scheduleId}`;
    const response = await fetch(url, {
      method: 'DELETE',
      headers: this.buildHeaders(),
    });
    await this.assertOk(response);
  }

  // --- AI ---

  async explainRule(ruleId: string): Promise<ExplainRuleResponse> {
    const url = `${this.baseUrl}/v1/ai/explain`;
    return this.post(url, { ruleId }) as Promise<ExplainRuleResponse>;
  }

  async generateRule(prompt: string, ruleType: string): Promise<GenerateRuleResponse> {
    const url = `${this.baseUrl}/v1/ai/generate-rule`;
    return this.post(url, { prompt, ruleType }) as Promise<GenerateRuleResponse>;
  }

  async generateTestData(ruleId: string, count: number): Promise<GenerateTestDataResponse> {
    const url = `${this.baseUrl}/v1/ai/generate-test-data`;
    return this.post(url, { ruleId, count }) as Promise<GenerateTestDataResponse>;
  }

  private async delete(url: string): Promise<void> {
    const response = await fetch(url, {
      method: 'DELETE',
      headers: this.buildHeaders(),
    });
    await this.assertOk(response);
  }

  private async assertOk(response: Response): Promise<void> {
    if (!response.ok) {
      const text = await response.text();
      throw new Error(`RuleForge API error ${response.status}: ${text}`);
    }
  }
}
