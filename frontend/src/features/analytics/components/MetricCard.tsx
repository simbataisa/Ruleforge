import type { ReactNode } from 'react';
import { cn } from '@/shared/utils/cn';

interface MetricCardProps {
  label: string;
  value: string | number;
  description?: string;
  icon?: ReactNode;
  className?: string;
}

export function MetricCard({
  label,
  value,
  description,
  icon,
  className,
}: MetricCardProps) {
  return (
    <div
      className={cn(
        'rounded-lg border border-gray-200 bg-white p-6 shadow-sm',
        className,
      )}
    >
      <div className="flex items-center justify-between">
        <p className="text-sm font-medium text-gray-500">{label}</p>
        {icon && (
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600">
            {icon}
          </div>
        )}
      </div>
      <p className="mt-2 text-3xl font-bold text-gray-900">{value}</p>
      {description && (
        <p className="mt-1 text-sm text-gray-500">{description}</p>
      )}
    </div>
  );
}
