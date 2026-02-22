import { Badge } from '@/shared/components/Badge';
import type { VersionStatus } from '@/api/types/rule.types';

const approvalStyles: Partial<Record<VersionStatus, { color: 'yellow' | 'green' | 'red' | 'gray'; label: string }>> = {
  PENDING_APPROVAL: { color: 'yellow', label: 'Pending Approval' },
  PUBLISHED: { color: 'green', label: 'Approved' },
};

interface ApprovalBadgeProps {
  status: VersionStatus;
  rejectionReason?: string;
}

export function ApprovalBadge({ status, rejectionReason }: ApprovalBadgeProps) {
  if (rejectionReason && status === 'DRAFT') {
    return (
      <span title={rejectionReason}>
        <Badge color="red">Rejected</Badge>
      </span>
    );
  }

  const style = approvalStyles[status];
  if (!style) return null;

  return <Badge color={style.color}>{style.label}</Badge>;
}
