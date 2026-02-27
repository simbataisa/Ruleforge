import { useParams, useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, ExternalLink } from 'lucide-react';
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  Legend,
} from 'recharts';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Badge } from '@/shared/components/Badge';
import { Button } from '@/shared/components/Button';
import { Spinner } from '@/shared/components/Spinner';
import { useToastStore } from '@/stores/toast.store';
import { apiClient } from '@/api/client';
import { useAbTestResults, useAbTestSignificance } from '@/api/analytics.api';
import type { ApiResponse } from '@/api/types/common.types';

// ---------------------------------------------------------------------------
// Shared types -- these match the QE test contract
// ---------------------------------------------------------------------------

export interface VariantResult {
  variant: string;
  executionCount: number;
  avgLatency: number;
  errorRate: number;
}

export interface AbTestResults {
  testId: string;
  testName: string;
  status: string;
  significance: number; // 0-100 %
  significanceThreshold: number; // e.g. 95
  sufficientSamples: boolean;
  winner: string | null;
  variants: VariantResult[];
}

// ---------------------------------------------------------------------------
// Pure presentational component (tested directly by QE)
// ---------------------------------------------------------------------------

interface AbTestDetailPageProps {
  results: AbTestResults;
  onPromoteWinner?: (variant: string) => void;
}

export function AbTestDetailPage({
  results,
  onPromoteWinner,
}: AbTestDetailPageProps) {
  const meetsThreshold =
    results.significance >= results.significanceThreshold;

  return (
    <div>
      <h1>{results.testName}</h1>
      <p data-testid="test-status">{results.status}</p>

      {/* Significance meter */}
      <div data-testid="significance-meter">
        <span data-testid="significance-value">{results.significance}%</span>
        <span data-testid="significance-threshold">
          / {results.significanceThreshold}%
        </span>
        <div
          role="progressbar"
          aria-valuenow={results.significance}
          aria-valuemin={0}
          aria-valuemax={100}
          style={{ width: `${Math.min(results.significance, 100)}%` }}
        />
      </div>

      {/* Metric comparison table */}
      <table data-testid="metric-comparison">
        <thead>
          <tr>
            <th>Metric</th>
            {results.variants.map((v) => (
              <th key={v.variant}>{v.variant}</th>
            ))}
            <th>Winner</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>Execution Count</td>
            {results.variants.map((v) => (
              <td key={v.variant}>{v.executionCount.toLocaleString()}</td>
            ))}
            <td data-testid="winner-executions">
              {results.winner ?? '--'}
            </td>
          </tr>
          <tr>
            <td>Avg Latency</td>
            {results.variants.map((v) => (
              <td key={v.variant}>{v.avgLatency}ms</td>
            ))}
            <td data-testid="winner-latency">
              {results.winner
                ? results.variants[0].avgLatency <
                  results.variants[1].avgLatency
                  ? results.variants[0].variant
                  : results.variants[1].variant
                : '--'}
            </td>
          </tr>
          <tr>
            <td>Error Rate</td>
            {results.variants.map((v) => (
              <td key={v.variant}>
                {(v.errorRate * 100).toFixed(2)}%
              </td>
            ))}
            <td data-testid="winner-error-rate">
              {results.winner
                ? results.variants[0].errorRate <
                  results.variants[1].errorRate
                  ? results.variants[0].variant
                  : results.variants[1].variant
                : '--'}
            </td>
          </tr>
        </tbody>
      </table>

      {/* Promote Winner button */}
      {onPromoteWinner && results.winner && (
        <button
          data-testid="promote-winner-btn"
          disabled={!meetsThreshold}
          onClick={() => onPromoteWinner(results.winner!)}
        >
          Promote Winner ({results.winner})
        </button>
      )}

      {!results.sufficientSamples && (
        <p data-testid="insufficient-samples">
          Insufficient samples for statistical significance
        </p>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Internal types for route-level data fetching
// ---------------------------------------------------------------------------

interface AbTestDetailDto {
  id: string;
  name: string;
  ruleId: string;
  ruleName?: string;
  versionA: number;
  versionB: number;
  trafficSplit: number;
  status: string;
  startDate?: string;
  endDate?: string;
  hypothesis?: string;
  minSampleSize?: number;
  significanceThreshold?: number;
}

interface AbTestResultsDto {
  testId: string;
  versionA: {
    executions: number;
    errors: number;
    errorRate: number;
    avgLatencyMs: number;
    p95LatencyMs: number;
  };
  versionB: {
    executions: number;
    errors: number;
    errorRate: number;
    avgLatencyMs: number;
    p95LatencyMs: number;
  };
  timeline?: { timestamp: string; countA: number; countB: number }[];
}

interface SignificanceDto {
  confidence: number;
  threshold: number;
  winner: 'A' | 'B' | null;
  significant: boolean;
  sufficientSamples?: boolean;
}

type BadgeColor = 'gray' | 'green' | 'blue' | 'yellow' | 'red';

function statusColor(status: string): BadgeColor {
  switch (status.toUpperCase()) {
    case 'ACTIVE':
    case 'RUNNING':
      return 'green';
    case 'COMPLETED':
      return 'blue';
    case 'STOPPED':
    case 'CANCELLED':
      return 'red';
    case 'PENDING':
    case 'SCHEDULED':
      return 'yellow';
    default:
      return 'gray';
  }
}

function useAbTestDetail(testId: string) {
  return useQuery({
    queryKey: ['ab-tests', testId],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<AbTestDetailDto>>(
        `/rules/ab-tests/${testId}`,
      );
      return res.data.data;
    },
    enabled: !!testId,
  });
}

function usePromoteAbTestWinner() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      ruleId,
      testId,
      winner,
    }: {
      ruleId: string;
      testId: string;
      winner: string;
    }) => {
      await apiClient.post(`/rules/${ruleId}/ab-tests/${testId}/promote`, {
        winner,
      });
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['ab-tests', variables.testId],
      });
      queryClient.invalidateQueries({ queryKey: ['ab-tests'] });
    },
  });
}

/**
 * Transform raw API responses into the AbTestResults shape expected by the
 * presentational component and tests.
 */
function buildResults(
  test: AbTestDetailDto,
  rawResults: AbTestResultsDto | undefined,
  rawSignificance: SignificanceDto | undefined,
): AbTestResults {
  const variants: VariantResult[] = rawResults
    ? [
        {
          variant: `Variant A`,
          executionCount: rawResults.versionA.executions,
          avgLatency: rawResults.versionA.avgLatencyMs,
          errorRate: rawResults.versionA.errorRate,
        },
        {
          variant: `Variant B`,
          executionCount: rawResults.versionB.executions,
          avgLatency: rawResults.versionB.avgLatencyMs,
          errorRate: rawResults.versionB.errorRate,
        },
      ]
    : [
        { variant: 'Variant A', executionCount: 0, avgLatency: 0, errorRate: 0 },
        { variant: 'Variant B', executionCount: 0, avgLatency: 0, errorRate: 0 },
      ];

  const winnerLabel = rawSignificance?.winner
    ? `Variant ${rawSignificance.winner}`
    : null;

  return {
    testId: test.id,
    testName: test.name,
    status: test.status,
    significance: rawSignificance
      ? rawSignificance.confidence * 100
      : 0,
    significanceThreshold: test.significanceThreshold ?? 95,
    sufficientSamples: rawSignificance?.sufficientSamples ?? true,
    winner: winnerLabel,
    variants,
  };
}

// ---------------------------------------------------------------------------
// Route-level page (used in the router, fetches data from APIs)
// ---------------------------------------------------------------------------

export function AbTestDetailPageRoute() {
  const { id: testId } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const addToast = useToastStore((s) => s.addToast);
  const promoteWinner = usePromoteAbTestWinner();

  const { data: test, isLoading } = useAbTestDetail(testId ?? '');
  const { data: rawResults } = useAbTestResults(testId ?? '');
  const { data: rawSignificance } = useAbTestSignificance(testId ?? '');

  const apiResults = rawResults as AbTestResultsDto | undefined;
  const apiSignificance = rawSignificance as SignificanceDto | undefined;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner size="lg" />
      </div>
    );
  }

  if (!test) {
    return (
      <div className="text-center py-20">
        <p className="text-gray-500">A/B test not found.</p>
        <Button
          variant="outline"
          className="mt-4"
          onClick={() => navigate('/ab-tests')}
        >
          Back to A/B Tests
        </Button>
      </div>
    );
  }

  const results = buildResults(test, apiResults, apiSignificance);
  const totalA = apiResults?.versionA.executions ?? 0;
  const totalB = apiResults?.versionB.executions ?? 0;

  const handlePromote = (variant: string) => {
    const winnerKey = variant.includes('A') ? 'A' : 'B';
    promoteWinner.mutate(
      { ruleId: test.ruleId, testId: test.id, winner: winnerKey },
      {
        onSuccess: () => {
          addToast({
            type: 'success',
            title: `${variant} promoted`,
          });
        },
        onError: () => {
          addToast({ type: 'error', title: 'Failed to promote winner' });
        },
      },
    );
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Back and header */}
      <div>
        <button
          onClick={() => navigate('/ab-tests')}
          className="mb-3 flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
        >
          <ArrowLeft className="h-4 w-4" />
          Back to A/B Tests
        </button>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold text-gray-900">{test.name}</h1>
            <Badge color={statusColor(test.status)}>{test.status}</Badge>
          </div>
          <div className="flex items-center gap-2 text-sm text-gray-500">
            {test.startDate && (
              <span>
                {new Date(test.startDate).toLocaleDateString()} --{' '}
                {test.endDate
                  ? new Date(test.endDate).toLocaleDateString()
                  : 'Ongoing'}
              </span>
            )}
            <Link
              to={`/rules/${test.ruleId}/edit`}
              className="flex items-center gap-1 text-indigo-600 hover:underline"
            >
              {test.ruleName || 'View Rule'}
              <ExternalLink className="h-3 w-3" />
            </Link>
          </div>
        </div>
      </div>

      {/* Hypothesis */}
      {test.hypothesis && (
        <div className="rounded-lg border border-gray-200 bg-gray-50 p-4">
          <p className="text-xs font-semibold uppercase text-gray-500 mb-1">
            Hypothesis
          </p>
          <p className="text-sm text-gray-700">{test.hypothesis}</p>
        </div>
      )}

      {/* Traffic split visualization */}
      <div className="rounded-lg border border-gray-200 bg-white p-4">
        <h3 className="text-sm font-semibold text-gray-900 mb-3">
          Traffic Distribution
        </h3>
        <div className="flex h-8 overflow-hidden rounded-full">
          <div
            className="flex items-center justify-center bg-indigo-500 text-xs font-medium text-white transition-all"
            style={{ width: `${test.trafficSplit}%` }}
          >
            A: {test.trafficSplit}%
            {totalA > 0 && ` (${totalA.toLocaleString()})`}
          </div>
          <div
            className="flex items-center justify-center bg-indigo-200 text-xs font-medium text-indigo-800 transition-all"
            style={{ width: `${100 - test.trafficSplit}%` }}
          >
            B: {100 - test.trafficSplit}%
            {totalB > 0 && ` (${totalB.toLocaleString()})`}
          </div>
        </div>
      </div>

      {/* Testable core component */}
      <div className="rounded-lg border border-gray-200 bg-white p-4">
        <AbTestDetailPage
          results={results}
          onPromoteWinner={handlePromote}
        />
      </div>

      {/* Performance timeline chart */}
      {apiResults?.timeline && apiResults.timeline.length > 0 && (
        <div className="rounded-lg border border-gray-200 bg-white p-4">
          <h3 className="text-sm font-semibold text-gray-900 mb-3">
            Execution Count Over Time
          </h3>
          <ResponsiveContainer width="100%" height={280}>
            <LineChart
              data={apiResults.timeline.map((p) => ({
                time: new Date(p.timestamp).toLocaleTimeString('en-US', {
                  hour: '2-digit',
                  minute: '2-digit',
                }),
                A: p.countA,
                B: p.countB,
              }))}
              margin={{ top: 8, right: 16, bottom: 0, left: 0 }}
            >
              <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
              <XAxis
                dataKey="time"
                tick={{ fontSize: 12, fill: '#6b7280' }}
                tickLine={false}
                axisLine={{ stroke: '#e5e7eb' }}
              />
              <YAxis
                tick={{ fontSize: 12, fill: '#6b7280' }}
                tickLine={false}
                axisLine={false}
                allowDecimals={false}
              />
              <Tooltip
                contentStyle={{
                  fontSize: 13,
                  borderRadius: 8,
                  border: '1px solid #e5e7eb',
                }}
              />
              <Legend
                verticalAlign="bottom"
                iconType="circle"
                formatter={(value: string) => (
                  <span className="text-sm text-gray-600">{value}</span>
                )}
              />
              <Line
                type="monotone"
                dataKey="A"
                stroke="#6366f1"
                strokeWidth={2}
                dot={false}
                name={`Version A (v${test.versionA})`}
              />
              <Line
                type="monotone"
                dataKey="B"
                stroke="#a78bfa"
                strokeWidth={2}
                dot={false}
                name={`Version B (v${test.versionB})`}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
}
