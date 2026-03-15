import { useState } from 'react';
import { ChevronDown, ChevronRight, RefreshCw } from 'lucide-react';
import { Badge } from '@/shared/components/Badge';
import { Button } from '@/shared/components/Button';
import { Spinner } from '@/shared/components/Spinner';
import { useWebhookDeliveries, useWebhookOutbox, useRetryOutboxEntry, type WebhookDelivery } from '@/api/webhooks.api';
import { useToastStore } from '@/stores/toast.store';

type StatusFilter = 'ALL' | 'DELIVERED' | 'FAILED' | 'PENDING' | 'DLQ';

function statusBadgeProps(delivery: WebhookDelivery): { color: 'green' | 'red' | 'yellow' | 'gray'; label: string } {
  if (delivery.success) return { color: 'green', label: 'Delivered' };
  if (delivery.statusCode === 0) return { color: 'yellow', label: 'Pending' };
  return { color: 'red', label: 'Failed' };
}

function formatDateTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

interface ExpandedRowProps {
  delivery: WebhookDelivery;
}

function ExpandedRow({ delivery }: ExpandedRowProps) {
  return (
    <tr>
      <td colSpan={6} className="px-4 py-3 bg-gray-50 border-b border-gray-100">
        <div className="space-y-3 text-xs">
          {delivery.requestHeaders && Object.keys(delivery.requestHeaders).length > 0 && (
            <div>
              <p className="font-semibold text-gray-600 mb-1">Request Headers</p>
              <div className="bg-white rounded border border-gray-200 p-2 font-mono space-y-0.5">
                {Object.entries(delivery.requestHeaders).map(([k, v]) => (
                  <div key={k}>
                    <span className="text-gray-500">{k}:</span>{' '}
                    <span className="text-gray-900">{v}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
          {delivery.responseBody && (
            <div>
              <p className="font-semibold text-gray-600 mb-1">Response Body</p>
              <pre className="bg-white rounded border border-gray-200 p-2 font-mono text-gray-900 overflow-x-auto whitespace-pre-wrap">
                {delivery.responseBody}
              </pre>
            </div>
          )}
          {delivery.lastError && (
            <div>
              <p className="font-semibold text-red-600 mb-1">Error</p>
              <pre className="bg-red-50 rounded border border-red-200 p-2 font-mono text-red-900 overflow-x-auto whitespace-pre-wrap">
                {delivery.lastError}
              </pre>
            </div>
          )}
          {!delivery.requestHeaders && !delivery.responseBody && !delivery.lastError && (
            <p className="text-gray-400 italic">No additional details available.</p>
          )}
        </div>
      </td>
    </tr>
  );
}

interface Props {
  webhookId: string;
}

const STATUS_FILTERS: { value: StatusFilter; label: string }[] = [
  { value: 'ALL', label: 'All' },
  { value: 'DELIVERED', label: 'Delivered' },
  { value: 'FAILED', label: 'Failed' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'DLQ', label: 'DLQ' },
];

export function WebhookDeliveryLogsTab({ webhookId }: Props) {
  const addToast = useToastStore((s) => s.addToast);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());

  const { data: deliveriesPage, isLoading, refetch } = useWebhookDeliveries(webhookId);
  const { data: dlqEntries } = useWebhookOutbox(webhookId, 'DLQ');
  const retryEntry = useRetryOutboxEntry();

  const deliveries = deliveriesPage?.content ?? [];

  const filtered = deliveries.filter((d) => {
    if (statusFilter === 'ALL') return true;
    if (statusFilter === 'DELIVERED') return d.success;
    if (statusFilter === 'FAILED') return !d.success && d.statusCode !== 0;
    if (statusFilter === 'PENDING') return d.statusCode === 0;
    return false; // DLQ shown separately via outbox
  });

  const toggleExpand = (id: string) => {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleRetryDlq = (entryId: string) => {
    retryEntry.mutate(
      { webhookId, entryId },
      {
        onSuccess: () => addToast({ type: 'success', title: 'Retry queued' }),
        onError: () => addToast({ type: 'error', title: 'Failed to retry' }),
      },
    );
  };

  const dlqList = dlqEntries ?? [];

  return (
    <div className="space-y-4">
      {/* Filter bar */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1">
          {STATUS_FILTERS.map((f) => (
            <button
              key={f.value}
              onClick={() => setStatusFilter(f.value)}
              className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                statusFilter === f.value
                  ? 'bg-indigo-100 text-indigo-700'
                  : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              {f.label}
              {f.value === 'DLQ' && dlqList.length > 0 && (
                <span className="ml-1.5 inline-flex items-center justify-center h-4 min-w-4 px-1 rounded-full bg-red-500 text-white text-xs">
                  {dlqList.length}
                </span>
              )}
            </button>
          ))}
        </div>
        <Button variant="outline" size="sm" onClick={() => refetch()} disabled={isLoading}>
          <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          Refresh
        </Button>
      </div>

      {/* DLQ section */}
      {statusFilter === 'DLQ' && (
        <div>
          {dlqList.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12 text-gray-400">
              <p className="text-sm">No DLQ entries</p>
            </div>
          ) : (
            <div className="rounded-lg border border-red-200 overflow-hidden">
              <div className="bg-red-50 border-b border-red-200 px-4 py-2 flex items-center justify-between">
                <span className="text-sm font-medium text-red-800">
                  {dlqList.length} dead letter {dlqList.length === 1 ? 'entry' : 'entries'}
                </span>
              </div>
              <table className="w-full text-sm">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    <th className="text-left px-4 py-2 font-medium text-gray-500">Event Type</th>
                    <th className="text-left px-4 py-2 font-medium text-gray-500">Attempts</th>
                    <th className="text-left px-4 py-2 font-medium text-gray-500">Last Status</th>
                    <th className="text-left px-4 py-2 font-medium text-gray-500">Created</th>
                    <th className="text-right px-4 py-2 font-medium text-gray-500">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {dlqList.map((entry) => (
                    <tr key={entry.id} className="hover:bg-gray-50">
                      <td className="px-4 py-2 font-mono text-xs">{entry.eventType}</td>
                      <td className="px-4 py-2 text-gray-600">{entry.attemptCount}</td>
                      <td className="px-4 py-2">
                        {entry.lastStatusCode ? (
                          <span className="text-red-600">{entry.lastStatusCode}</span>
                        ) : (
                          <span className="text-gray-400">—</span>
                        )}
                      </td>
                      <td className="px-4 py-2 text-gray-400 text-xs">{formatDateTime(entry.createdAt)}</td>
                      <td className="px-4 py-2 text-right">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => handleRetryDlq(entry.id)}
                          loading={retryEntry.isPending}
                        >
                          Retry
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Delivery logs table */}
      {statusFilter !== 'DLQ' && (
        <>
          {isLoading ? (
            <div className="flex items-center justify-center py-12">
              <Spinner size="lg" />
            </div>
          ) : filtered.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12 text-gray-400">
              <p className="text-sm">No delivery logs found</p>
            </div>
          ) : (
            <div className="rounded-lg border border-gray-200 overflow-hidden">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    <th className="w-8" />
                    <th className="text-left px-4 py-3 font-medium text-gray-500">Timestamp</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-500">Event Type</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-500">Status</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-500">Status Code</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-500">Attempts</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((delivery) => {
                    const isExpanded = expandedIds.has(delivery.id);
                    const { color, label } = statusBadgeProps(delivery);
                    return [
                      <tr
                        key={delivery.id}
                        className="hover:bg-gray-50 cursor-pointer border-b border-gray-100 transition-colors"
                        onClick={() => toggleExpand(delivery.id)}
                      >
                        <td className="pl-3">
                          {isExpanded
                            ? <ChevronDown className="h-3.5 w-3.5 text-gray-400" />
                            : <ChevronRight className="h-3.5 w-3.5 text-gray-400" />
                          }
                        </td>
                        <td className="px-4 py-3 text-gray-500 text-xs">{formatDateTime(delivery.createdAt)}</td>
                        <td className="px-4 py-3 font-mono text-xs text-gray-900">{delivery.eventType}</td>
                        <td className="px-4 py-3">
                          <Badge color={color}>{label}</Badge>
                        </td>
                        <td className="px-4 py-3 text-gray-600">
                          {delivery.statusCode || <span className="text-gray-400">—</span>}
                        </td>
                        <td className="px-4 py-3 text-gray-600">
                          {delivery.attemptCount ?? 1}
                        </td>
                      </tr>,
                      isExpanded && <ExpandedRow key={`${delivery.id}-expanded`} delivery={delivery} />,
                    ];
                  })}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  );
}
