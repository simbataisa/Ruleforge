import { cn } from '@/shared/utils/cn';
import type { RuleStatus } from '@/api/types/rule.types';

const statusConfig: Record<string, { label: string; className: string }> = {
  DRAFT: { label: 'Draft', className: 'bg-gray-100 text-gray-700' },
  PUBLISHED: { label: 'Published', className: 'bg-green-100 text-green-700' },
  ARCHIVED: { label: 'Archived', className: 'bg-gray-50 text-gray-500' },
  PENDING_APPROVAL: { label: 'Pending', className: 'bg-amber-100 text-amber-700' },
};

interface StatusBadgeProps {
  status: RuleStatus | string;
  className?: string;
}

export function StatusBadge({ status, className }: StatusBadgeProps) {
  const config = statusConfig[status] ?? { label: status, className: 'bg-gray-100 text-gray-700' };

  return (
    <span
      className={cn(
        'inline-flex items-center rounded px-1.5 py-0.5 text-[11px] font-medium',
        config.className,
        className,
      )}
    >
      {config.label}
    </span>
  );
}
