import { Badge } from '@/shared/components/Badge';
import { Button } from '@/shared/components/Button';

type BadgeColor = 'gray' | 'green' | 'blue' | 'yellow' | 'red';

interface AbTest {
  id: string;
  name: string;
  versionA: number;
  versionB: number;
  trafficSplit: number;
  status: string;
  startDate?: string;
  endDate?: string;
}

interface AbTestCardProps {
  test: AbTest;
  onStop?: (testId: string) => void;
  onViewResults?: (testId: string) => void;
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

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

export function AbTestCard({ test, onStop, onViewResults }: AbTestCardProps) {
  const splitA = test.trafficSplit;
  const splitB = 100 - test.trafficSplit;
  const isActive =
    test.status.toUpperCase() === 'ACTIVE' ||
    test.status.toUpperCase() === 'RUNNING';

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
      {/* Header */}
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <h3 className="truncate text-sm font-semibold text-gray-900">
            {test.name}
          </h3>
          <p className="mt-0.5 text-xs text-gray-500">
            v{test.versionA} vs v{test.versionB}
          </p>
        </div>
        <Badge color={statusColor(test.status)}>
          {test.status}
        </Badge>
      </div>

      {/* Traffic split bar */}
      <div className="mt-4">
        <div className="mb-1 flex justify-between text-xs text-gray-500">
          <span>v{test.versionA} ({splitA}%)</span>
          <span>v{test.versionB} ({splitB}%)</span>
        </div>
        <div className="flex h-2.5 overflow-hidden rounded-full bg-gray-100">
          <div
            className="bg-indigo-500"
            style={{ width: `${splitA}%` }}
          />
          <div
            className="bg-indigo-200"
            style={{ width: `${splitB}%` }}
          />
        </div>
      </div>

      {/* Date range */}
      {(test.startDate || test.endDate) && (
        <div className="mt-3 flex items-center gap-1 text-xs text-gray-500">
          <span>{test.startDate ? formatDate(test.startDate) : 'Now'}</span>
          <span className="mx-1">→</span>
          <span>{test.endDate ? formatDate(test.endDate) : 'No end date'}</span>
        </div>
      )}

      {/* Actions */}
      <div className="mt-4 flex gap-2">
        {isActive && onStop && (
          <Button
            size="sm"
            variant="danger"
            onClick={() => onStop(test.id)}
          >
            Stop Test
          </Button>
        )}
        {onViewResults && (
          <Button
            size="sm"
            variant="outline"
            onClick={() => onViewResults(test.id)}
          >
            View Results
          </Button>
        )}
      </div>
    </div>
  );
}
