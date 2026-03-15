import type { WebhookScopeType } from '@/api/webhooks.api';

interface WebhookScopeBadgeProps {
  scopeType: WebhookScopeType;
}

const SCOPE_CONFIG: Record<WebhookScopeType, { icon: string; label: string; className: string }> = {
  ORG: { icon: '🏢', label: 'Org', className: 'bg-purple-100 text-purple-700' },
  SPACE: { icon: '📦', label: 'Space', className: 'bg-blue-100 text-blue-700' },
  FOLDER: { icon: '📁', label: 'Folder', className: 'bg-yellow-100 text-yellow-700' },
  RULE: { icon: '📋', label: 'Rule', className: 'bg-green-100 text-green-700' },
};

export function WebhookScopeBadge({ scopeType }: WebhookScopeBadgeProps) {
  const config = SCOPE_CONFIG[scopeType] ?? SCOPE_CONFIG.ORG;
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium ${config.className}`}
    >
      <span aria-hidden="true">{config.icon}</span>
      {config.label}
    </span>
  );
}
