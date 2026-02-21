import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { RuleForgeClient } from '../client';

const BASE_URL = 'https://api.ruleforge.io';

const server = setupServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

function createClient() {
  return new RuleForgeClient({ baseUrl: BASE_URL, apiKey: 'test-api-key' });
}

// --- Solver ---

describe('solve', () => {
  it('returns solver result', async () => {
    server.use(
      http.post(`${BASE_URL}/v1/solve/my-rule`, () =>
        HttpResponse.json({ output: { approved: true }, matchedRules: [], executionTimeMs: 5 }),
      ),
    );
    const client = createClient();
    const result = await client.solve('my-rule', { age: 30 });
    expect(result.output).toEqual({ approved: true });
    expect(result.executionTimeMs).toBe(5);
  });
});

describe('solveBulk', () => {
  it('returns multiple results', async () => {
    server.use(
      http.post(`${BASE_URL}/v1/solve/bulk/my-rule`, () =>
        HttpResponse.json([
          { output: { approved: true }, matchedRules: [], executionTimeMs: 3 },
          { output: { approved: false }, matchedRules: [], executionTimeMs: 4 },
        ]),
      ),
    );
    const client = createClient();
    const results = await client.solveBulk('my-rule', [{ age: 30 }, { age: 17 }]);
    expect(results).toHaveLength(2);
  });
});

// --- Rules ---

describe('listRules', () => {
  it('returns rules array', async () => {
    server.use(
      http.get(`${BASE_URL}/v1/spaces/space-1/rules`, () =>
        HttpResponse.json({ content: [{ id: 'r1', name: 'Test Rule' }], totalElements: 1 }),
      ),
    );
    const client = createClient();
    const rules = await client.listRules('space-1');
    expect(rules).toHaveLength(1);
  });
});

describe('getRule', () => {
  it('returns rule details', async () => {
    server.use(
      http.get(`${BASE_URL}/v1/rules/r1`, () =>
        HttpResponse.json({ id: 'r1', name: 'Test Rule' }),
      ),
    );
    const client = createClient();
    const rule = await client.getRule('r1');
    expect(rule).toEqual({ id: 'r1', name: 'Test Rule' });
  });
});

// --- Analytics ---

describe('getMetrics', () => {
  it('returns metrics', async () => {
    server.use(
      http.get(`${BASE_URL}/v1/analytics/metrics`, () =>
        HttpResponse.json({ totalExecutions: 1000, avgLatencyMs: 12.5 }),
      ),
    );
    const client = createClient();
    const metrics = await client.getMetrics();
    expect(metrics.totalExecutions).toBe(1000);
  });
});

describe('getRuleStats', () => {
  it('returns rule statistics', async () => {
    server.use(
      http.get(`${BASE_URL}/v1/analytics/rules/r1/stats`, () =>
        HttpResponse.json({ hitCount: 500, avgLatencyMs: 8.2, errorRate: 0.01 }),
      ),
    );
    const client = createClient();
    const stats = await client.getRuleStats('r1');
    expect(stats.hitCount).toBe(500);
    expect(stats.errorRate).toBe(0.01);
  });
});

describe('getDecisionLogs', () => {
  it('returns decision logs', async () => {
    server.use(
      http.get(`${BASE_URL}/v1/analytics/decision-logs`, () =>
        HttpResponse.json([{ id: 'log1', ruleId: 'r1', timestamp: '2026-01-15T10:00:00Z' }]),
      ),
    );
    const client = createClient();
    const logs = await client.getDecisionLogs('r1', 0, 10);
    expect(logs).toHaveLength(1);
    expect(logs[0].id).toBe('log1');
  });

  it('works without rule filter', async () => {
    server.use(
      http.get(`${BASE_URL}/v1/analytics/decision-logs`, () => HttpResponse.json([])),
    );
    const client = createClient();
    const logs = await client.getDecisionLogs();
    expect(logs).toEqual([]);
  });
});

// --- Schedules ---

describe('listSchedules', () => {
  it('returns schedules', async () => {
    server.use(
      http.get(`${BASE_URL}/v1/schedules`, () =>
        HttpResponse.json([{ id: 's1', cronExpression: '0 0 * * *', ruleId: 'r1', enabled: true }]),
      ),
    );
    const client = createClient();
    const schedules = await client.listSchedules();
    expect(schedules).toHaveLength(1);
    expect(schedules[0].id).toBe('s1');
  });
});

describe('createSchedule', () => {
  it('creates and returns schedule', async () => {
    server.use(
      http.post(`${BASE_URL}/v1/schedules`, () =>
        HttpResponse.json({ id: 's2', cronExpression: '0 */5 * * *', ruleId: 'r1', enabled: true }),
      ),
    );
    const client = createClient();
    const result = await client.createSchedule({
      ruleId: 'r1',
      cronExpression: '0 */5 * * *',
    });
    expect(result.id).toBe('s2');
  });
});

describe('deleteSchedule', () => {
  it('deletes without error', async () => {
    server.use(
      http.delete(`${BASE_URL}/v1/schedules/s1`, () => new HttpResponse(null, { status: 204 })),
    );
    const client = createClient();
    await expect(client.deleteSchedule('s1')).resolves.toBeUndefined();
  });
});

// --- AI ---

describe('explainRule', () => {
  it('returns explanation', async () => {
    server.use(
      http.post(`${BASE_URL}/v1/ai/explain`, () =>
        HttpResponse.json({ summary: 'Checks loan eligibility', conditions: ['age >= 18'] }),
      ),
    );
    const client = createClient();
    const result = await client.explainRule('r1');
    expect(result.summary).toBe('Checks loan eligibility');
  });
});

describe('generateRule', () => {
  it('returns generated rule', async () => {
    server.use(
      http.post(`${BASE_URL}/v1/ai/generate-rule`, () =>
        HttpResponse.json({ id: 'gen-1', name: 'Auto Loan Rule', type: 'DECISION_TABLE' }),
      ),
    );
    const client = createClient();
    const result = await client.generateRule('Approve loans for age > 18', 'DECISION_TABLE');
    expect(result.type).toBe('DECISION_TABLE');
  });
});

describe('generateTestData', () => {
  it('returns test data', async () => {
    server.use(
      http.post(`${BASE_URL}/v1/ai/generate-test-data`, () =>
        HttpResponse.json({ testData: [{ age: 25, income: 60000 }, { age: 17, income: 30000 }] }),
      ),
    );
    const client = createClient();
    const result = await client.generateTestData('r1', 2);
    expect(result.testData).toHaveLength(2);
  });
});

// --- Error handling ---

describe('error handling', () => {
  it('throws on server error', async () => {
    server.use(
      http.post(`${BASE_URL}/v1/solve/bad-rule`, () =>
        HttpResponse.json({ error: 'Internal Server Error' }, { status: 500 }),
      ),
    );
    const client = createClient();
    await expect(client.solve('bad-rule', {})).rejects.toThrow('RuleForge API error 500');
  });
});
