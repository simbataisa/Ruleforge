import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
import { Spinner } from '@/shared/components/Spinner';
import { EmptyState } from '@/shared/components/EmptyState';
import {
  useWebhooks,
  type Webhook,
} from '@/api/webhooks.api';
import { WebhookScopeBadge } from '@/features/webhooks/components/WebhookScopeBadge';
import { CreateWebhookModal } from '@/features/webhooks/components/CreateWebhookModal';

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

export function WebhookListPage() {
  const navigate = useNavigate();
  const { data: webhooks, isLoading } = useWebhooks();

  const [showCreateModal, setShowCreateModal] = useState(false);

  const columns = [
    {
      key: 'url',
      header: 'URL / Name',
      render: (w: Webhook) => (
        <div>
          {w.name && (
            <div className="font-medium text-gray-900 text-sm">{w.name}</div>
          )}
          <span className="text-gray-500 text-sm truncate max-w-xs block">{w.url}</span>
        </div>
      ),
    },
    {
      key: 'scope',
      header: 'Scope',
      render: (w: Webhook) => (
        <WebhookScopeBadge scopeType={w.scopeType ?? 'ORG'} />
      ),
    },
    {
      key: 'events',
      header: 'Events',
      render: (w: Webhook) => (
        <div className="flex flex-wrap gap-1">
          {w.events.map((e) => (
            <Badge key={e} className="bg-gray-100 text-gray-700 text-xs">{e}</Badge>
          ))}
        </div>
      ),
    },
    {
      key: 'active',
      header: 'Status',
      render: (w: Webhook) => {
        if (w.circuitStatus === 'OPEN') {
          return (
            <Badge className="bg-orange-100 text-orange-800">
              ⚡ Circuit Open
            </Badge>
          );
        }
        return (
          <Badge className={w.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600'}>
            {w.active ? 'Active' : 'Inactive'}
          </Badge>
        );
      },
    },
    {
      key: 'createdAt',
      header: 'Created',
      render: (w: Webhook) => (
        <span className="text-gray-500">{formatDate(w.createdAt)}</span>
      ),
    },
  ];

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner size="lg" />
      </div>
    );
  }

  const webhookList = webhooks ?? [];

  return (
    <div>
      <PageHeader
        title="Webhooks"
        description="Configure HTTP callbacks for rule lifecycle events."
        actions={
          <Button onClick={() => setShowCreateModal(true)}>
            <Plus className="h-4 w-4" />
            Create Webhook
          </Button>
        }
      />

      {webhookList.length === 0 ? (
        <EmptyState
          title="No webhooks configured"
          description="Create a webhook to receive notifications when events occur."
        />
      ) : (
        <div className="overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                {columns.map((col) => (
                  <th
                    key={col.key}
                    className="text-left px-4 py-3 font-medium text-gray-500"
                  >
                    {col.header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {webhookList.map((webhook) => (
                <tr
                  key={webhook.id}
                  className={`cursor-pointer transition-colors hover:bg-gray-50 ${
                    webhook.circuitStatus === 'OPEN' ? 'bg-orange-50' : ''
                  }`}
                  onClick={() => navigate(`/webhooks/${webhook.id}`)}
                >
                  {columns.map((col) => (
                    <td key={col.key} className="px-4 py-3">
                      {col.render(webhook)}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showCreateModal && (
        <CreateWebhookModal
          onClose={() => setShowCreateModal(false)}
          onSuccess={() => setShowCreateModal(false)}
        />
      )}
    </div>
  );
}
