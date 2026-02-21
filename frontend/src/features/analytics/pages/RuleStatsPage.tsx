import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Activity, Clock, AlertCircle, Calendar } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { Spinner } from '@/shared/components/Spinner';
import { useRuleStats } from '@/api/analytics.api';
import { MetricCard } from '../components/MetricCard';

export function RuleStatsPage() {
  const { ruleId } = useParams<{ ruleId: string }>();
  const navigate = useNavigate();

  const {
    data: stats,
    isLoading,
    isError,
  } = useRuleStats(ruleId ?? '');

  return (
    <div className="flex flex-col gap-6">
      {/* Back button + header */}
      <div className="flex items-center gap-4">
        <Button
          variant="ghost"
          size="sm"
          onClick={() => navigate('/analytics')}
          className="flex items-center gap-1 text-gray-600"
        >
          <ArrowLeft className="h-4 w-4" />
          Back to Analytics
        </Button>
      </div>

      <div>
        <h1 className="text-2xl font-bold text-gray-900">
          {isLoading ? 'Loading...' : (stats?.ruleName ?? 'Rule Statistics')}
        </h1>
        <p className="mt-1 text-sm text-gray-500">
          Last 30 days performance metrics
        </p>
      </div>

      {isError && (
        <div className="flex items-center gap-3 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          <AlertCircle className="h-5 w-5 flex-shrink-0" />
          <span>Failed to load rule statistics. Please try again later.</span>
        </div>
      )}

      {isLoading ? (
        <div className="flex items-center justify-center py-24">
          <Spinner size="lg" />
        </div>
      ) : stats ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <MetricCard
            label="Total Executions"
            value={stats.totalExecutions.toLocaleString()}
            description="Over the last 30 days"
            icon={<Activity className="h-5 w-5" />}
          />
          <MetricCard
            label="Avg Execution Time"
            value={`${stats.avgExecutionTimeMs.toFixed(1)} ms`}
            description="Average latency per call"
            icon={<Clock className="h-5 w-5" />}
          />
          <MetricCard
            label="Error Rate"
            value={`${(stats.errorRate * 100).toFixed(2)}%`}
            description="Fraction of failed calls"
            icon={<AlertCircle className="h-5 w-5" />}
          />
          <MetricCard
            label="Last Executed"
            value={
              stats.lastExecuted
                ? new Date(stats.lastExecuted).toLocaleDateString('en-US', {
                    month: 'short',
                    day: 'numeric',
                    year: 'numeric',
                  })
                : 'Never'
            }
            description="Most recent execution"
            icon={<Calendar className="h-5 w-5" />}
          />
        </div>
      ) : null}
    </div>
  );
}
