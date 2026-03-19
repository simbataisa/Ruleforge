import { useState, useMemo } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { ChevronRight, ArrowLeft, Zap, Send, RefreshCw } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
import { Spinner } from '@/shared/components/Spinner';
import {
  useWebhook,
  useWebhookDeliveries,
  useWebhookOutbox,
  useTestWebhook,
  useResetCircuit,
} from '@/api/webhooks.api';
import { WebhookScopeBadge } from '@/features/webhooks/components/WebhookScopeBadge';
import { WebhookDeliveryLogsTab } from '@/features/webhooks/components/WebhookDeliveryLogsTab';
import { WebhookSecurityTab } from '@/features/webhooks/components/WebhookSecurityTab';
import { WebhookSettingsTab } from '@/features/webhooks/components/WebhookSettingsTab';
import { WebhookMetricsTab } from '@/features/webhooks/components/WebhookMetricsTab';
import { useToastStore } from '@/stores/toast.store';

type Tab = 'overview' | 'deliveries' | 'security' | 'settings' | 'metrics';

const TABS: { id: Tab; label: string }[] = [
  { id: 'overview', label: 'Overview' },
  { id: 'deliveries', label: 'Delivery Logs' },
  { id: 'security', label: 'Security' },
  { id: 'settings', label: 'Settings' },
  { id: 'metrics', label: 'Metrics' },
];

function StatCard({ label, value, className = '' }: { label: string; value: string | number; className?: string }) {
  return (
    <div className={`rounded-lg border border-gray-200 p-4 ${className}`}>
      <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">{label}</p>
      <p className="mt-1 text-2xl font-bold text-gray-900">{value}</p>
    </div>
  );
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  });
}

export function WebhookDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const addToast = useToastStore((s) => s.addToast);

  const [activeTab, setActiveTab] = useState<Tab>('overview');

  const { data: webhook, isLoading, isError, refetch } = useWebhook(id ?? '');
  const { data: deliveriesPage } = useWebhookDeliveries(id ?? '');
  const { data: outboxEntries } = useWebhookOutbox(id ?? '');
  const testWebhook = useTestWebhook();
  const resetCircuit = useResetCircuit();

  // Stats — compute from delivery data
  const stats = useMemo(() => {
    const deliveries = deliveriesPage?.content ?? [];
    const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;
    const recent = deliveries.filter((d) => new Date(d.deliveredAt).getTime() > sevenDaysAgo);

    const delivered7d = recent.filter((d) => d.success).length;
    const failed7d = recent.filter((d) => !d.success).length;
    const successRate = recent.length > 0
      ? Math.round((delivered7d / recent.length) * 100)
      : 100;

    const pendingCount = (outboxEntries ?? []).filter(
      (e) => e.status === 'PENDING' || e.status === 'DELIVERING',
    ).length;

    return { delivered7d, failed7d, successRate, pendingCount };
  }, [deliveriesPage, outboxEntries]);

  const handleTest = () => {
    if (!id) return;
    testWebhook.mutate({ webhookId: id }, {
      onSuccess: () => addToast({ type: 'success', title: 'Test delivery sent' }),
      onError: () => addToast({ type: 'error', title: 'Failed to send test delivery' }),
    });
  };

  const handleResetCircuit = () => {
    if (!id) return;
    resetCircuit.mutate(id, {
      onSuccess: () => {
        addToast({ type: 'success', title: 'Circuit reset — webhook re-enabled' });
        refetch();
      },
      onError: () => addToast({ type: 'error', title: 'Failed to reset circuit' }),
    });
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner size="lg" />
      </div>
    );
  }

  if (isError || !webhook) {
    return (
      <div className="flex flex-col items-center justify-center h-64 gap-4">
        <p className="text-gray-500">Webhook not found or you don&apos;t have access.</p>
        <Button variant="outline" onClick={() => navigate('/webhooks')}>
          Back to Webhooks
        </Button>
      </div>
    );
  }

  const isCircuitOpen = webhook.circuitStatus === 'OPEN';
  const title = webhook.name || webhook.url;

  return (
    <div className="flex flex-col h-full">
      {/* Breadcrumb */}
      <div className="flex items-center gap-1.5 px-6 pt-4 text-sm text-gray-500">
        <Link to="/webhooks" className="hover:text-gray-700 transition-colors">
          Webhooks
        </Link>
        <ChevronRight className="h-3.5 w-3.5" />
        <span className="text-gray-900 font-medium truncate max-w-xs">{title}</span>
      </div>

      {/* Header */}
      <div className="flex items-start justify-between px-6 py-4 border-b border-gray-200">
        <div className="flex items-start gap-3">
          <button
            onClick={() => navigate('/webhooks')}
            className="mt-0.5 p-1.5 rounded-md text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
            aria-label="Back to webhooks"
          >
            <ArrowLeft className="h-4 w-4" />
          </button>
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <h1 className="text-xl font-semibold text-gray-900 truncate max-w-lg">{title}</h1>
              {isCircuitOpen ? (
                <span className="inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium bg-orange-100 text-orange-800">
                  <Zap className="h-3 w-3" />
                  Circuit Open
                </span>
              ) : (
                <Badge color="green">Active</Badge>
              )}
              <WebhookScopeBadge scopeType={webhook.scopeType ?? 'ORG'} />
            </div>
            <p className="text-sm text-gray-400 font-mono mt-0.5 truncate max-w-lg">{webhook.url}</p>
          </div>
        </div>

        <div className="flex items-center gap-2 flex-shrink-0">
          {isCircuitOpen && (
            <Button
              variant="outline"
              size="sm"
              onClick={handleResetCircuit}
              loading={resetCircuit.isPending}
            >
              <RefreshCw className="h-3.5 w-3.5" />
              Reset Circuit
            </Button>
          )}
          <Button
            variant="outline"
            size="sm"
            onClick={handleTest}
            loading={testWebhook.isPending}
          >
            <Send className="h-3.5 w-3.5" />
            Send Test
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setActiveTab('settings')}
          >
            Edit
          </Button>
        </div>
      </div>

      {/* Stats Row */}
      <div className="grid grid-cols-4 gap-4 px-6 py-4 border-b border-gray-200">
        <StatCard label="Delivered (7d)" value={stats.delivered7d} />
        <StatCard
          label="Failed (7d)"
          value={stats.failed7d}
          className={stats.failed7d > 0 ? 'border-red-200 bg-red-50' : ''}
        />
        <StatCard label="Pending" value={stats.pendingCount} />
        <StatCard label="Success Rate (7d)" value={`${stats.successRate}%`} />
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200 px-6">
        <nav className="flex gap-0 -mb-px">
          {TABS.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 py-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === tab.id
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab content */}
      <div className="flex-1 overflow-auto p-6">
        {activeTab === 'overview' && (
          <div className="max-w-2xl space-y-6">
            <div className="rounded-lg border border-gray-200 p-5">
              <h3 className="text-sm font-semibold text-gray-900 mb-4">Configuration</h3>
              <dl className="grid grid-cols-[auto,1fr] gap-x-6 gap-y-3 text-sm">
                {webhook.name && (
                  <>
                    <dt className="font-medium text-gray-500">Name</dt>
                    <dd className="text-gray-900">{webhook.name}</dd>
                  </>
                )}
                <dt className="font-medium text-gray-500">URL</dt>
                <dd className="text-gray-900 font-mono text-xs break-all">{webhook.url}</dd>

                <dt className="font-medium text-gray-500">Scope</dt>
                <dd><WebhookScopeBadge scopeType={webhook.scopeType ?? 'ORG'} /></dd>

                <dt className="font-medium text-gray-500">Events</dt>
                <dd>
                  <div className="flex flex-wrap gap-1">
                    {webhook.events.map((e) => (
                      <span
                        key={e}
                        className="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium bg-gray-100 text-gray-700"
                      >
                        {e}
                      </span>
                    ))}
                  </div>
                </dd>

                <dt className="font-medium text-gray-500">Status</dt>
                <dd>
                  {isCircuitOpen ? (
                    <span className="text-orange-700 font-medium">Circuit Open ({webhook.consecutiveFailures} consecutive failures)</span>
                  ) : webhook.active ? (
                    <span className="text-green-700 font-medium">Active</span>
                  ) : (
                    <span className="text-gray-500">Inactive</span>
                  )}
                </dd>

                {webhook.createdBy && (
                  <>
                    <dt className="font-medium text-gray-500">Created by</dt>
                    <dd className="text-gray-900">{webhook.createdBy}</dd>
                  </>
                )}

                <dt className="font-medium text-gray-500">Created</dt>
                <dd className="text-gray-900">{formatDate(webhook.createdAt)}</dd>

                <dt className="font-medium text-gray-500">Last updated</dt>
                <dd className="text-gray-900">{formatDate(webhook.updatedAt)}</dd>
              </dl>
            </div>
          </div>
        )}

        {activeTab === 'deliveries' && (
          <WebhookDeliveryLogsTab webhookId={webhook.id} />
        )}

        {activeTab === 'security' && (
          <WebhookSecurityTab webhookId={webhook.id} webhookUrl={webhook.url} />
        )}

        {activeTab === 'settings' && (
          <WebhookSettingsTab
            webhook={webhook}
            onUpdated={() => {
              refetch();
              addToast({ type: 'success', title: 'Settings saved' });
            }}
            onDeleted={() => navigate('/webhooks')}
          />
        )}

        {activeTab === 'metrics' && (
          <WebhookMetricsTab webhookId={webhook.id} />
        )}
      </div>
    </div>
  );
}
