import { useState } from 'react';
import { Plus, Trash2, Pencil } from 'lucide-react';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Badge } from '@/shared/components/Badge';
import { DataTable } from '@/shared/components/DataTable';
import { Spinner } from '@/shared/components/Spinner';
import { EmptyState } from '@/shared/components/EmptyState';
import { Modal } from '@/shared/components/Modal';
import {
  useWebhooks,
  useCreateWebhook,
  useUpdateWebhook,
  useDeleteWebhook,
  type Webhook,
} from '@/api/webhooks.api';
import { useToastStore } from '@/stores/toast.store';

const AVAILABLE_EVENTS = [
  'rule.created',
  'rule.updated',
  'rule.deleted',
  'rule.published',
  'rule.executed',
];

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

export function WebhookListPage() {
  const addToast = useToastStore((s) => s.addToast);
  const { data: webhooks, isLoading } = useWebhooks();
  const createWebhook = useCreateWebhook();
  const updateWebhook = useUpdateWebhook();
  const deleteWebhook = useDeleteWebhook();

  const [showModal, setShowModal] = useState(false);
  const [editingWebhook, setEditingWebhook] = useState<Webhook | null>(null);
  const [url, setUrl] = useState('');
  const [selectedEvents, setSelectedEvents] = useState<string[]>([]);
  const [active, setActive] = useState(true);

  const openCreate = () => {
    setEditingWebhook(null);
    setUrl('');
    setSelectedEvents([]);
    setActive(true);
    setShowModal(true);
  };

  const openEdit = (webhook: Webhook) => {
    setEditingWebhook(webhook);
    setUrl(webhook.url);
    setSelectedEvents(webhook.events);
    setActive(webhook.active);
    setShowModal(true);
  };

  const handleSubmit = () => {
    const data = { url, events: selectedEvents, active };
    if (editingWebhook) {
      updateWebhook.mutate(
        { id: editingWebhook.id, ...data },
        {
          onSuccess: () => {
            setShowModal(false);
            addToast({ type: 'success', title: 'Webhook updated' });
          },
          onError: () => addToast({ type: 'error', title: 'Failed to update webhook' }),
        },
      );
    } else {
      createWebhook.mutate(data, {
        onSuccess: () => {
          setShowModal(false);
          addToast({ type: 'success', title: 'Webhook created' });
        },
        onError: () => addToast({ type: 'error', title: 'Failed to create webhook' }),
      });
    }
  };

  const handleDelete = (id: string) => {
    deleteWebhook.mutate(id, {
      onSuccess: () => addToast({ type: 'success', title: 'Webhook deleted' }),
      onError: () => addToast({ type: 'error', title: 'Failed to delete webhook' }),
    });
  };

  const toggleEvent = (event: string) => {
    setSelectedEvents((prev) =>
      prev.includes(event) ? prev.filter((e) => e !== event) : [...prev, event],
    );
  };

  const columns = [
    {
      key: 'url',
      header: 'URL',
      render: (w: Webhook) => (
        <span className="font-medium text-gray-900 text-sm truncate max-w-xs block">{w.url}</span>
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
      render: (w: Webhook) => (
        <Badge className={w.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600'}>
          {w.active ? 'Active' : 'Inactive'}
        </Badge>
      ),
    },
    {
      key: 'createdAt',
      header: 'Created',
      render: (w: Webhook) => (
        <span className="text-gray-500">{formatDate(w.createdAt)}</span>
      ),
    },
    {
      key: 'actions',
      header: '',
      render: (w: Webhook) => (
        <div className="flex items-center gap-1">
          <button
            onClick={(e) => { e.stopPropagation(); openEdit(w); }}
            className="p-1.5 rounded text-gray-400 hover:text-indigo-600 hover:bg-gray-100"
          >
            <Pencil className="h-4 w-4" />
          </button>
          <button
            onClick={(e) => { e.stopPropagation(); handleDelete(w.id); }}
            className="p-1.5 rounded text-gray-400 hover:text-red-600 hover:bg-gray-100"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
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
          <Button onClick={openCreate}>
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
        <DataTable data={webhookList} columns={columns} />
      )}

      <Modal
        open={showModal}
        onClose={() => setShowModal(false)}
        title={editingWebhook ? 'Edit Webhook' : 'Create Webhook'}
        footer={
          <>
            <Button variant="outline" onClick={() => setShowModal(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleSubmit}
              disabled={!url || selectedEvents.length === 0 || createWebhook.isPending || updateWebhook.isPending}
            >
              {editingWebhook ? 'Update' : 'Create'}
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">URL</label>
            <Input
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://example.com/webhook"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Events</label>
            <div className="space-y-2">
              {AVAILABLE_EVENTS.map((event) => (
                <label key={event} className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={selectedEvents.includes(event)}
                    onChange={() => toggleEvent(event)}
                    className="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                  />
                  {event}
                </label>
              ))}
            </div>
          </div>
          <div>
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={active}
                onChange={(e) => setActive(e.target.checked)}
                className="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
              />
              <span className="font-medium text-gray-700">Active</span>
            </label>
          </div>
        </div>
      </Modal>
    </div>
  );
}
