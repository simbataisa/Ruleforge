import { useState, useEffect, useCallback, useMemo } from 'react';
import { Activity, Clock, TrendingUp, AlertCircle, Download, Bell, CalendarDays } from 'lucide-react';
import { useQueryClient } from '@tanstack/react-query';
import { PageHeader } from '@/shared/components/PageHeader';
import { SkeletonCard } from '@/components/Skeleton';
import { Button } from '@/shared/components/Button';
import {
  useDashboardMetrics,
  useDecisionLogs,
  useExecutionTimeline,
  useTypeDistribution,
  useLatencyPercentiles,
  usePassFail,
} from '@/api/analytics.api';
import type { DecisionQueryParams } from '@/api/types/analytics.types';
import { MetricCard } from '../components/MetricCard';
import { TopRulesTable } from '../components/TopRulesTable';
import { DecisionLogTable } from '../components/DecisionLogTable';
import { ExecutionTimelineChart } from '../components/ExecutionTimelineChart';
import { ExecutionsByTypeChart } from '../components/ExecutionsByTypeChart';
import { PassFailChart } from '../components/PassFailChart';
import { LatencyChart } from '../components/LatencyChart';

type TimeRange = { label: string; hours: number };
type TabId = 'dashboard' | 'decision-log' | 'alerts';
type RefreshInterval = 0 | 30 | 60 | 300;

const TIME_RANGES: TimeRange[] = [
  { label: '1h', hours: 1 },
  { label: '24h', hours: 24 },
  { label: '7d', hours: 168 },
  { label: '30d', hours: 720 },
];

const REFRESH_OPTIONS: { label: string; value: RefreshInterval }[] = [
  { label: 'Off', value: 0 },
  { label: '30s', value: 30 },
  { label: '1m', value: 60 },
  { label: '5m', value: 300 },
];

const TABS: { id: TabId; label: string }[] = [
  { id: 'dashboard', label: 'Dashboard' },
  { id: 'decision-log', label: 'Decision Log' },
  { id: 'alerts', label: 'Alerts' },
];

function toDateInputValue(date: Date): string {
  return date.toISOString().split('T')[0];
}

export function AnalyticsDashboardPage() {
  const queryClient = useQueryClient();
  const [selectedRange, setSelectedRange] = useState<TimeRange>(TIME_RANGES[1]);
  const [activeTab, setActiveTab] = useState<TabId>('dashboard');
  const [refreshInterval, setRefreshInterval] = useState<RefreshInterval>(0);

  // Custom date range
  const [customFrom, setCustomFrom] = useState('');
  const [customTo, setCustomTo] = useState('');
  const [useCustomRange, setUseCustomRange] = useState(false);

  // Compute effective hours from custom range or preset
  const effectiveHours = useMemo(() => {
    if (useCustomRange && customFrom && customTo) {
      const from = new Date(customFrom).getTime();
      const to = new Date(customTo).getTime() + 24 * 60 * 60 * 1000; // end of day
      return Math.max(1, Math.round((to - from) / (1000 * 60 * 60)));
    }
    return selectedRange.hours;
  }, [useCustomRange, customFrom, customTo, selectedRange.hours]);

  // Decision log state
  const [logsPage, setLogsPage] = useState(0);
  const [logSearch, setLogSearch] = useState('');
  const [logStatus, setLogStatus] = useState<string>('');
  const [logMinLatency, setLogMinLatency] = useState<string>('');
  const [logMaxLatency, setLogMaxLatency] = useState<string>('');

  const logParams: DecisionQueryParams = {
    page: logsPage,
    size: 20,
    ...(logSearch ? { search: logSearch } : {}),
    ...(logStatus ? { status: logStatus } : {}),
    ...(logMinLatency ? { minLatency: Number(logMinLatency) } : {}),
    ...(logMaxLatency ? { maxLatency: Number(logMaxLatency) } : {}),
  };

  // Dashboard data
  const {
    data: metrics,
    isLoading: metricsLoading,
    isError: metricsError,
  } = useDashboardMetrics(effectiveHours);

  const { data: logsData, isLoading: logsLoading } = useDecisionLogs(logParams);

  // New API hooks for real chart data
  const { data: timelineData } = useExecutionTimeline(
    effectiveHours,
    effectiveHours <= 24 ? 'hour' : 'day',
  );
  const { data: typeDistribution } = useTypeDistribution(effectiveHours);
  const { data: latencyPercentiles } = useLatencyPercentiles(effectiveHours);
  const { data: passFailData } = usePassFail(
    effectiveHours,
    effectiveHours <= 24 ? 'hour' : 'day',
  );

  // Auto-refresh
  useEffect(() => {
    if (refreshInterval === 0) return;
    const interval = setInterval(() => {
      queryClient.invalidateQueries({ queryKey: ['analytics'] });
    }, refreshInterval * 1000);
    return () => clearInterval(interval);
  }, [refreshInterval, queryClient]);

  const handleRangeChange = (range: TimeRange) => {
    setSelectedRange(range);
    setUseCustomRange(false);
    setLogsPage(0);
  };

  // Click metric card to jump to filtered decision log
  const handleMetricClick = useCallback(
    (metricType: 'errors' | 'latency') => {
      setActiveTab('decision-log');
      setLogsPage(0);
      if (metricType === 'errors') {
        setLogStatus('ERROR');
      } else if (metricType === 'latency') {
        setLogMinLatency('100');
        setLogMaxLatency('');
      }
    },
    [],
  );

  const handleExportCsv = () => {
    const params = new URLSearchParams();
    if (logSearch) params.set('search', logSearch);
    if (logStatus) params.set('status', logStatus);
    if (logMinLatency) params.set('minLatency', logMinLatency);
    if (logMaxLatency) params.set('maxLatency', logMaxLatency);
    window.open(`/v1/analytics/decisions/export.csv?${params.toString()}`, '_blank');
  };

  // Derive chart data -- use real API data when available, fall back to metrics
  const chartTimeline = (timelineData ?? []).map((p) => ({
    time: new Date(p.timestamp).toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
    }),
    count: p.count,
  }));

  const chartTypeData = (typeDistribution ?? []).map((t) => ({
    type: t.type,
    count: t.count,
  }));

  const chartPassFail = passFailData
    ? {
        passed: 0,
        failed: 0,
        timelineData: passFailData.map((p) => ({
          time: new Date(p.timestamp).toLocaleTimeString('en-US', {
            hour: '2-digit',
            minute: '2-digit',
          }),
          passed: p.passed,
          failed: p.failed,
        })),
      }
    : metrics
      ? {
          passed: metrics.totalDecisions - Math.round(metrics.totalDecisions * metrics.errorRate),
          failed: Math.round(metrics.totalDecisions * metrics.errorRate),
          timelineData: undefined,
        }
      : { passed: 0, failed: 0, timelineData: undefined };

  const chartLatency = latencyPercentiles
    ? [
        { percentile: 'p50', ms: Math.round(latencyPercentiles.p50) },
        { percentile: 'p75', ms: Math.round(latencyPercentiles.p75) },
        { percentile: 'p90', ms: Math.round(latencyPercentiles.p90) },
        { percentile: 'p95', ms: Math.round(latencyPercentiles.p95) },
        { percentile: 'p99', ms: Math.round(latencyPercentiles.p99) },
      ]
    : metrics
      ? [
          { percentile: 'p50', ms: Math.round(metrics.avgExecutionTimeMs * 0.8) },
          { percentile: 'p75', ms: Math.round(metrics.avgExecutionTimeMs * 1.1) },
          { percentile: 'p90', ms: Math.round(metrics.avgExecutionTimeMs * 1.5) },
          { percentile: 'p95', ms: Math.round(metrics.avgExecutionTimeMs * 2.2) },
          { percentile: 'p99', ms: Math.round(metrics.avgExecutionTimeMs * 4.0) },
        ]
      : [];

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Analytics"
        description="Monitor rule execution performance and trends."
        actions={
          <div className="flex items-center gap-3">
            {/* Auto-refresh toggle */}
            <div className="flex items-center gap-2">
              {refreshInterval > 0 && (
                <span className="relative flex h-2 w-2">
                  <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-green-400 opacity-75" />
                  <span className="relative inline-flex h-2 w-2 rounded-full bg-green-500" />
                </span>
              )}
              <select
                value={refreshInterval}
                onChange={(e) =>
                  setRefreshInterval(Number(e.target.value) as RefreshInterval)
                }
                className="rounded-md border border-gray-200 bg-white px-2 py-1.5 text-sm text-gray-600"
              >
                {REFRESH_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.value === 0 ? 'Auto-refresh: Off' : `Refresh: ${opt.label}`}
                  </option>
                ))}
              </select>
            </div>

            {/* Time range presets */}
            <div className="flex items-center gap-1 rounded-lg border border-gray-200 bg-white p-1">
              {TIME_RANGES.map((range) => (
                <button
                  key={range.label}
                  onClick={() => handleRangeChange(range)}
                  className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                    selectedRange.hours === range.hours && !useCustomRange
                      ? 'bg-indigo-600 text-white'
                      : 'text-gray-600 hover:bg-gray-100'
                  }`}
                >
                  {range.label}
                </button>
              ))}
            </div>

            {/* Custom date range */}
            <div className="flex items-center gap-1.5">
              <CalendarDays className="h-4 w-4 text-gray-400" />
              <input
                type="date"
                value={customFrom}
                onChange={(e) => {
                  setCustomFrom(e.target.value);
                  if (e.target.value && customTo) setUseCustomRange(true);
                }}
                max={customTo || toDateInputValue(new Date())}
                className={`rounded-md border px-2 py-1.5 text-sm ${
                  useCustomRange
                    ? 'border-indigo-400 ring-1 ring-indigo-200'
                    : 'border-gray-200'
                }`}
              />
              <span className="text-xs text-gray-400">to</span>
              <input
                type="date"
                value={customTo}
                onChange={(e) => {
                  setCustomTo(e.target.value);
                  if (customFrom && e.target.value) setUseCustomRange(true);
                }}
                min={customFrom || undefined}
                max={toDateInputValue(new Date())}
                className={`rounded-md border px-2 py-1.5 text-sm ${
                  useCustomRange
                    ? 'border-indigo-400 ring-1 ring-indigo-200'
                    : 'border-gray-200'
                }`}
              />
              {useCustomRange && (
                <button
                  onClick={() => {
                    setUseCustomRange(false);
                    setCustomFrom('');
                    setCustomTo('');
                  }}
                  className="text-xs text-gray-400 hover:text-gray-600"
                >
                  Clear
                </button>
              )}
            </div>
          </div>
        }
      />

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="flex gap-6">
          {TABS.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`border-b-2 pb-3 text-sm font-medium transition-colors ${
                activeTab === tab.id
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* Dashboard Tab */}
      {activeTab === 'dashboard' && (
        <>
          {/* Metric Cards */}
          {metricsLoading ? (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {Array.from({ length: 4 }).map((_, i) => (
                <SkeletonCard key={i} />
              ))}
            </div>
          ) : metricsError ? (
            <div className="flex items-center gap-3 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-700">
              <AlertCircle className="h-5 w-5 flex-shrink-0" />
              <span>Analytics data is temporarily unavailable. Execution history may not be indexed yet.</span>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <MetricCard
                label="Total Decisions"
                value={metrics?.totalDecisions.toLocaleString() ?? '--'}
                description={`Last ${selectedRange.label}`}
                icon={<Activity className="h-5 w-5" />}
              />
              <MetricCard
                label="Decisions / Min"
                value={metrics ? metrics.decisionsPerMinute.toFixed(1) : '--'}
                description="Current throughput"
                icon={<TrendingUp className="h-5 w-5" />}
              />
              <MetricCard
                label="Avg Latency"
                value={
                  metrics ? `${metrics.avgExecutionTimeMs.toFixed(1)} ms` : '--'
                }
                description="Average execution time"
                icon={<Clock className="h-5 w-5" />}
                className="cursor-pointer hover:ring-2 hover:ring-indigo-200"
                onClick={() => handleMetricClick('latency')}
              />
              <MetricCard
                label="Error Rate"
                value={
                  metrics
                    ? `${(metrics.errorRate * 100).toFixed(2)}%`
                    : '--'
                }
                description="Failed executions"
                icon={<AlertCircle className="h-5 w-5" />}
                className="cursor-pointer hover:ring-2 hover:ring-indigo-200"
                onClick={() => handleMetricClick('errors')}
              />
            </div>
          )}

          {/* Charts */}
          {!metricsError && (metrics || chartTimeline.length > 0) && (
            <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
              <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
                <h3 className="mb-3 text-sm font-semibold text-gray-900">
                  Executions Over Time
                </h3>
                <ExecutionTimelineChart data={chartTimeline} />
              </div>
              <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
                <h3 className="mb-3 text-sm font-semibold text-gray-900">
                  Executions by Rule Type
                </h3>
                <ExecutionsByTypeChart data={chartTypeData} />
              </div>
              <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
                <h3 className="mb-3 text-sm font-semibold text-gray-900">
                  Pass / Fail Ratio
                </h3>
                <PassFailChart
                  passed={chartPassFail.passed}
                  failed={chartPassFail.failed}
                  timelineData={chartPassFail.timelineData}
                />
              </div>
              <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
                <h3 className="mb-3 text-sm font-semibold text-gray-900">
                  Latency Distribution
                </h3>
                <LatencyChart data={chartLatency} />
              </div>
            </div>
          )}

          {/* Top Rules */}
          {!metricsError && (
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
          )}
        </>
      )}

      {/* Decision Log Tab */}
      {activeTab === 'decision-log' && (
        <div className="rounded-lg border border-gray-200 bg-white shadow-sm">
          <div className="border-b border-gray-200 px-6 py-4">
            <div className="flex items-center justify-between">
              <h2 className="text-base font-semibold text-gray-900">
                Decision Log
              </h2>
              <Button size="sm" variant="outline" onClick={handleExportCsv}>
                <Download className="h-4 w-4" />
                Export CSV
              </Button>
            </div>

            {/* Filters */}
            <div className="mt-3 flex flex-wrap items-center gap-3">
              <input
                type="text"
                value={logSearch}
                onChange={(e) => {
                  setLogSearch(e.target.value);
                  setLogsPage(0);
                }}
                placeholder="Search rule name..."
                className="rounded-md border border-gray-300 px-3 py-1.5 text-sm shadow-sm placeholder:text-gray-400 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 w-56"
              />
              <select
                value={logStatus}
                onChange={(e) => {
                  setLogStatus(e.target.value);
                  setLogsPage(0);
                }}
                className="rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm shadow-sm"
              >
                <option value="">All statuses</option>
                <option value="SUCCESS">Success</option>
                <option value="ERROR">Error</option>
              </select>
              <div className="flex items-center gap-1 text-sm text-gray-500">
                <span>Latency:</span>
                <input
                  type="number"
                  value={logMinLatency}
                  onChange={(e) => {
                    setLogMinLatency(e.target.value);
                    setLogsPage(0);
                  }}
                  placeholder="Min ms"
                  className="w-20 rounded-md border border-gray-300 px-2 py-1.5 text-sm"
                />
                <span>-</span>
                <input
                  type="number"
                  value={logMaxLatency}
                  onChange={(e) => {
                    setLogMaxLatency(e.target.value);
                    setLogsPage(0);
                  }}
                  placeholder="Max ms"
                  className="w-20 rounded-md border border-gray-300 px-2 py-1.5 text-sm"
                />
              </div>
              {(logSearch || logStatus || logMinLatency || logMaxLatency) && (
                <button
                  onClick={() => {
                    setLogSearch('');
                    setLogStatus('');
                    setLogMinLatency('');
                    setLogMaxLatency('');
                    setLogsPage(0);
                  }}
                  className="text-xs text-indigo-600 hover:underline"
                >
                  Clear filters
                </button>
              )}
            </div>
          </div>

          <DecisionLogTable
            logs={logsData?.content ?? []}
            loading={logsLoading}
            page={logsPage}
            totalPages={logsData?.totalPages}
            onPageChange={setLogsPage}
          />
        </div>
      )}

      {/* Alerts Tab */}
      {activeTab === 'alerts' && (
        <div className="flex flex-col items-center justify-center rounded-lg border border-gray-200 bg-white p-16">
          <Bell className="h-12 w-12 text-gray-300" />
          <h3 className="mt-4 text-lg font-semibold text-gray-900">
            Alerts Coming Soon
          </h3>
          <p className="mt-2 max-w-sm text-center text-sm text-gray-500">
            Set up threshold-based alerts for error rates, latency spikes, and
            execution anomalies. Get notified via email, Slack, or webhooks.
          </p>
        </div>
      )}
    </div>
  );
}
