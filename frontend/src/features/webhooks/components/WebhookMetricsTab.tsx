import { RefreshCw } from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  Bar,
} from 'recharts';
import { Button } from '@/shared/components/Button';
import { useWebhookMetrics } from '@/api/webhooks.api';

interface WebhookMetricsTabProps {
  webhookId: string;
}

function SkeletonCard() {
  return (
    <div className="rounded-lg border border-gray-200 p-4 animate-pulse">
      <div className="h-3 bg-gray-200 rounded w-20 mb-3" />
      <div className="h-8 bg-gray-200 rounded w-16" />
    </div>
  );
}

export function WebhookMetricsTab({ webhookId }: WebhookMetricsTabProps) {
  const { data: metrics, isLoading, isError, refetch, isFetching } = useWebhookMetrics(webhookId);

  if (isLoading) {
    return (
      <div className="space-y-6 max-w-3xl">
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
          {Array.from({ length: 5 }).map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
        <div className="rounded-lg border border-gray-200 p-4 animate-pulse">
          <div className="h-48 bg-gray-100 rounded" />
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex items-center gap-4 rounded-lg border border-red-200 bg-red-50 p-6 max-w-xl">
        <p className="text-sm text-red-700 font-medium flex-1">Unable to load metrics.</p>
        <Button variant="outline" size="sm" onClick={() => refetch()}>
          Retry
        </Button>
      </div>
    );
  }

  if (!metrics || metrics.totalDeliveries === 0) {
    return (
      <div className="flex items-center justify-center h-48">
        <p className="text-sm text-gray-500">No delivery data yet.</p>
      </div>
    );
  }

  const successRateBorder =
    metrics.successRate >= 95
      ? ''
      : metrics.successRate >= 80
        ? 'border-yellow-400'
        : 'border-red-500';

  const statCards = [
    {
      label: 'Success Rate',
      value: `${metrics.successRate.toFixed(1)}%`,
      borderClass: successRateBorder,
    },
    {
      label: 'Deliveries',
      value: metrics.totalDeliveries,
      borderClass: '',
    },
    {
      label: 'Failures',
      value: metrics.failedDeliveries,
      borderClass: '',
    },
    {
      label: 'DLQ',
      value: metrics.dlqDepth > 0 ? `⚠ ${metrics.dlqDepth}` : metrics.dlqDepth,
      borderClass: '',
    },
    {
      label: 'Avg Latency',
      value: metrics.avgLatencyMs != null ? `${metrics.avgLatencyMs}ms` : '—',
      borderClass: '',
    },
  ];

  return (
    <div className="space-y-6 max-w-3xl">
      {/* Header row with Refresh */}
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-gray-900">Delivery Metrics</h3>
        <Button
          variant="outline"
          size="sm"
          onClick={() => refetch()}
          loading={isFetching}
        >
          <RefreshCw className="h-3.5 w-3.5" />
          Refresh
        </Button>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        {statCards.map((card) => (
          <div
            key={card.label}
            className={`rounded-lg border border-gray-200 p-4 ${card.borderClass}`}
          >
            <p className="text-xs font-medium text-gray-500">{card.label}</p>
            <p className="text-2xl font-bold text-gray-900">{card.value}</p>
          </div>
        ))}
      </div>

      {/* Bar chart */}
      {metrics.deliveryTimeline.length > 0 && (
        <div className="rounded-lg border border-gray-200 p-4">
          <p className="text-xs font-medium text-gray-500 mb-4">Delivery Timeline</p>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={metrics.deliveryTimeline}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis
                dataKey="date"
                tickFormatter={(d) =>
                  new Date(d).toLocaleDateString('en-US', { weekday: 'short' })
                }
              />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="success" fill="#22c55e" name="Success" stackId="a" />
              <Bar dataKey="failed" fill="#ef4444" name="Failed" stackId="a" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
}
