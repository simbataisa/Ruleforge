import { Link } from 'react-router-dom';
import { Spinner } from '@/shared/components/Spinner';
import type { RuleStatsDto } from '@/api/types/analytics.types';

interface TopRulesTableProps {
  rules: RuleStatsDto[];
  loading?: boolean;
}

export function TopRulesTable({ rules, loading }: TopRulesTableProps) {
  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Spinner size="lg" />
      </div>
    );
  }

  if (rules.length === 0) {
    return (
      <div className="py-12 text-center text-sm text-gray-500">
        No rule executions recorded yet.
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
              Rule Name
            </th>
            <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500">
              Total Executions
            </th>
            <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500">
              Avg Time (ms)
            </th>
            <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500">
              Error Rate
            </th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-200 bg-white">
          {rules.map((rule) => (
            <tr key={rule.ruleId} className="hover:bg-gray-50">
              <td className="whitespace-nowrap px-4 py-3">
                <Link
                  to={`/rules/${rule.ruleId}/edit`}
                  className="font-medium text-indigo-600 hover:text-indigo-800 hover:underline"
                >
                  {rule.ruleName}
                </Link>
              </td>
              <td className="whitespace-nowrap px-4 py-3 text-right text-sm text-gray-700">
                {rule.totalExecutions.toLocaleString()}
              </td>
              <td className="whitespace-nowrap px-4 py-3 text-right text-sm text-gray-700">
                {rule.avgExecutionTimeMs.toFixed(1)}
              </td>
              <td className="whitespace-nowrap px-4 py-3 text-right text-sm">
                <span
                  className={
                    rule.errorRate > 0.05
                      ? 'font-medium text-red-600'
                      : 'text-gray-700'
                  }
                >
                  {(rule.errorRate * 100).toFixed(2)}%
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
