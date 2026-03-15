import { useState, useEffect } from 'react';
import { AlertTriangle } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { ConfirmDialog } from '@/shared/components/ConfirmDialog';
import { useUpdateWebhook, useDeleteWebhook, type Webhook } from '@/api/webhooks.api';
import { useToastStore } from '@/stores/toast.store';

const AVAILABLE_EVENTS = [
  'rule.created',
  'rule.updated',
  'rule.deleted',
  'rule.published',
  'rule.executed',
];

interface Props {
  webhook: Webhook;
  onUpdated: () => void;
  onDeleted: () => void;
}

export function WebhookSettingsTab({ webhook, onUpdated, onDeleted }: Props) {
  const addToast = useToastStore((s) => s.addToast);
  const updateWebhook = useUpdateWebhook();
  const deleteWebhook = useDeleteWebhook();

  const [name, setName] = useState(webhook.name ?? '');
  const [url, setUrl] = useState(webhook.url);
  const [selectedEvents, setSelectedEvents] = useState<string[]>(webhook.events);
  const [active, setActive] = useState(webhook.active);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  // Sync when webhook changes
  useEffect(() => {
    setName(webhook.name ?? '');
    setUrl(webhook.url);
    setSelectedEvents(webhook.events);
    setActive(webhook.active);
  }, [webhook.id, webhook.updatedAt]);

  const toggleEvent = (event: string) => {
    setSelectedEvents((prev) =>
      prev.includes(event) ? prev.filter((e) => e !== event) : [...prev, event],
    );
  };

  const isDirty =
    name !== (webhook.name ?? '') ||
    url !== webhook.url ||
    active !== webhook.active ||
    JSON.stringify([...selectedEvents].sort()) !== JSON.stringify([...webhook.events].sort());

  const isValid = !!url && url.startsWith('https://') && selectedEvents.length > 0;

  const handleSave = () => {
    updateWebhook.mutate(
      { id: webhook.id, url, events: selectedEvents, active, ...(name ? { name } : {}) },
      {
        onSuccess: () => {
          addToast({ type: 'success', title: 'Webhook updated' });
          onUpdated();
        },
        onError: () => addToast({ type: 'error', title: 'Failed to update webhook' }),
      },
    );
  };

  const handleDelete = () => {
    deleteWebhook.mutate(webhook.id, {
      onSuccess: () => {
        addToast({ type: 'success', title: 'Webhook deleted' });
        onDeleted();
      },
      onError: () => addToast({ type: 'error', title: 'Failed to delete webhook' }),
    });
  };

  return (
    <div className="space-y-8 max-w-2xl">
      {/* Edit form */}
      <div className="space-y-4">
        <h3 className="text-sm font-semibold text-gray-900">Webhook Configuration</h3>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Name <span className="text-gray-400 font-normal">(optional)</span>
          </label>
          <Input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g., Production alerts"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">URL</label>
          <Input
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            placeholder="https://example.com/webhook"
          />
          {url && !url.startsWith('https://') && (
            <p className="mt-1 text-xs text-red-600">URL must start with https://</p>
          )}
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

        <div className="flex justify-end">
          <Button
            onClick={handleSave}
            disabled={!isDirty || !isValid || updateWebhook.isPending}
            loading={updateWebhook.isPending}
          >
            Save Changes
          </Button>
        </div>
      </div>

      {/* Danger Zone */}
      <div className="rounded-lg border-2 border-red-200 p-5">
        <div className="flex items-center gap-2 mb-3">
          <AlertTriangle className="h-5 w-5 text-red-500" />
          <h3 className="text-sm font-semibold text-red-800">Danger Zone</h3>
        </div>
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-900">Delete this webhook</p>
            <p className="text-xs text-gray-500 mt-0.5">
              Permanently remove this webhook and all its delivery history. This action cannot be undone.
            </p>
          </div>
          <Button
            variant="danger"
            size="sm"
            onClick={() => setShowDeleteConfirm(true)}
          >
            Delete Webhook
          </Button>
        </div>
      </div>

      <ConfirmDialog
        open={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={handleDelete}
        title="Delete Webhook"
        message={`Are you sure you want to delete this webhook${webhook.name ? ` "${webhook.name}"` : ''}? All delivery history will be permanently removed. This action cannot be undone.`}
        confirmLabel="Delete Webhook"
        confirmVariant="danger"
        isLoading={deleteWebhook.isPending}
      />
    </div>
  );
}
