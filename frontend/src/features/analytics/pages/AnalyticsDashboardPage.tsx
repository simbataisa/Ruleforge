import { useState } from 'react';
import { Activity, Clock, TrendingUp, AlertCircle } from 'lucide-react';
import { PageHeader } from '@/shared/components/PageHeader';
import { Spinner } from '@/shared/components/Spinner';
import { useDashboardMetrics, useDecisionLogs } from '@/api/analytics.api';
import { MetricCard } from '../components/MetricCard';
import { TopRulesTable } from '../components/TopRulesTable';
import { DecisionLogTable } from '../components/DecisionLogTable';

type TimeRange = { label: string; hours: number };

const TIME_RANGES: TimeRange[] = [
  { label: '1h', hours: 1 },
  { label: '24h', hours: 24 },
  { label: '7d', hours: 168 },
  { label: '30d', hours: 720 },
];

export function AnalyticsDashboardPage() {
  const [selectedRange, setSelectedRange] = useState<TimeRange>(TIME_RANGES[1]);
  const [logsPage, setLogsPage] = useState(0);

  const {
    data: metrics,
    isLoading: metricsLoading,
    isError: metricsError,
  } = useDashboardMetrics(selectedRange.hours);

  const {
    data: logsData,
    isLoading: logsLoading,
  } = useDecisionLogs({
    page: logsPage,
    size: 20,
  });

  const handleRangeChange = (range: TimeRange) => {
    setSelectedRange(range);
    setLogsPage(0);
  };

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Analytics"
        description="Monitor rule execution performance and trends."
        actions={
          <div className="flex items-center gap-1 rounded-lg border border-gray-200 bg-white p-1">
            {TIME_RANGES.map((range) => (
              <button
                key={range.label}
                onClick={() => handleRangeChange(range)}
                className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                  selectedRange.hours === range.hours
                    ? 'bg-indigo-600 text-white'
                    : 'text-gray-600 hover:bg-gray-100'
                }`}
              >
                {range.label}
              </button>
            ))}
          </div>
        }
      />

      {metricsError && (
        <div className="flex items-center gap-3 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          <AlertCircle className="h-5 w-5 flex-shrink-0" />
          <span>Failed to load analytics data. Please try again later.</span>
        </div>
      )}

      {/* Metric Cards */}
      {metricsLoading ? (
        <div className="flex items-center justify-center py-12">
          <Spinner size="lg" />
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <MetricCard
            label="Total Decisions"
            value={metrics?.totalDecisions.toLocaleString() ?? '—'}
            description={`Last ${selectedRange.label}`}
            icon={<Activity className="h-5 w-5" />}
          />
          <MetricCard
            label="Decisions / Min"
            value={
              metrics
                ? metrics.decisionsPerMinute.toFixed(1)
                : '—'
            }
            description="Current throughput"
            icon={<TrendingUp className="h-5 w-5" />}
          />
          <MetricCard
            label="Avg Latency"
            value={
              metrics
                ? `${metrics.avgExecutionTimeMs.toFixed(1)} ms`
                : '—'
            }
            description="Average execution time"
            icon={<Clock className="h-5 w-5" />}
          />
          <MetricCard
            label="Error Rate"
            value={
              metrics
                ? `${(metrics.errorRate * 100).toFixed(2)}%`
                : '—'
            }
            description="Failed executions"
            icon={<AlertCircle className="h-5 w-5" />}
          />
        </div>
      )}

      {/* Top Rules */}
      <div className="rounded-lg border border-gray-200 bg-white shadow-sm">
        <div className="border-b border-gray-200 px-6 py-4">
          <h2 className="text-base font-semibold text-gray-900">
            Top Rules by Execution Count
          </h2>
        </div>
        <TopRulesTable
          rules={metrics?.topRules ?? []}
          loading={metricsLoading}
        />
      </div>

      {/* Decision Log */}
      <div className="rounded-lg border border-gray-200 bg-white shadow-sm">
        <div className="border-b border-gray-200 px-6 py-4">
          <h2 className="text-base font-semibold text-gray-900">
            Recent Decisions
          </h2>
        </div>
        <DecisionLogTable
          logs={logsData?.content ?? []}
          loading={logsLoading}
          page={logsPage}
          totalPages={logsData?.totalPages}
          onPageChange={setLogsPage}
        />
      </div>
    </div>
  );
}
