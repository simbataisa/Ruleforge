import { useState } from 'react';
import { Cable, Plus, Trash2, Zap } from 'lucide-react';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
import { DataTable, type Column } from '@/shared/components/DataTable';
import { EmptyState } from '@/shared/components/EmptyState';
import { Spinner } from '@/shared/components/Spinner';
import { useToastStore } from '@/stores/toast.store';
import {
  useConnectors,
  useDeleteConnector,
  useTestConnection,
  type Connector,
} from '@/api/connectors.api';
import { ConnectorFormModal } from '../components/ConnectorFormModal';

type BadgeColor = 'gray' | 'green' | 'blue' | 'yellow' | 'red';

function statusColor(status: string): BadgeColor {
  switch (status) {
    case 'ACTIVE':
      return 'green';
    case 'INACTIVE':
      return 'gray';
    case 'ERROR':
      return 'red';
    default:
      return 'gray';
  }
}

function typeColor(type: string): BadgeColor {
  switch (type) {
    case 'JDBC':
      return 'blue';
    case 'HTTP':
      return 'green';
    case 'REDIS':
      return 'red';
    default:
      return 'gray';
  }
}

export function ConnectorsPage() {
  const addToast = useToastStore((s) => s.addToast);
  const [showCreate, setShowCreate] = useState(false);
  const { data, isLoading } = useConnectors();
  const deleteConnector = useDeleteConnector();
  const testConnection = useTestConnection();

  const connectors = data?.content ?? [];

  const handleDelete = async (id: string) => {
    try {
      await deleteConnector.mutateAsync(id);
      addToast({ type: 'success', title: 'Connector deleted' });
    } catch {
      addToast({ type: 'error', title: 'Failed to delete connector' });
    }
  };

  const handleTest = async (id: string) => {
    try {
      const result = await testConnection.mutateAsync(id);
      if (result.success) {
        addToast({
          type: 'success',
          title: `Connection successful${result.latencyMs ? ` (${result.latencyMs}ms)` : ''}`,
        });
      } else {
        addToast({ type: 'error', title: result.message });
      }
    } catch {
      addToast({ type: 'error', title: 'Connection test failed' });
    }
  };

  const columns: Column<Connector>[] = [
    {
      key: 'name',
      header: 'Name',
      render: (c) => (
        <div>
          <p className="font-medium text-gray-900">{c.name}</p>
          {c.description && (
            <p className="text-xs text-gray-500 truncate max-w-xs">{c.description}</p>
          )}
        </div>
      ),
    },
    {
      key: 'type',
      header: 'Type',
      render: (c) => <Badge color={typeColor(c.type)}>{c.type}</Badge>,
    },
    {
      key: 'status',
      header: 'Status',
      render: (c) => <Badge color={statusColor(c.status)}>{c.status}</Badge>,
    },
    {
      key: 'lastTested',
      header: 'Last Tested',
      render: (c) => (
        <span className="text-sm text-gray-600">
          {c.lastTestedAt
            ? new Date(c.lastTestedAt).toLocaleString('en-US', {
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              })
            : 'Never'}
        </span>
      ),
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (c) => (
        <div className="flex items-center gap-1">
          <button
            onClick={(e) => { e.stopPropagation(); handleTest(c.id); }}
            className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-indigo-600"
            title="Test Connection"
          >
            <Zap className="h-4 w-4" />
          </button>
          <button
            onClick={(e) => { e.stopPropagation(); handleDelete(c.id); }}
            className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-red-600"
            title="Delete"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      ),
    },
  ];

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="Connectors"
        description="Manage external data source connections"
        actions={
          <Button onClick={() => setShowCreate(true)}>
            <Plus className="h-4 w-4" />
            Add Connector
          </Button>
        }
      />

      {connectors.length === 0 ? (
        <EmptyState
          icon={<Cable className="h-12 w-12" />}
          title="No connectors configured"
          description="Add connectors to enrich rule execution with external data."
          actionLabel="Add Connector"
          onAction={() => setShowCreate(true)}
        />
      ) : (
        <div className="rounded-lg border border-gray-200 bg-white">
          <DataTable columns={columns} data={connectors} />
        </div>
      )}

      <ConnectorFormModal
        isOpen={showCreate}
        onClose={() => setShowCreate(false)}
      />
    </div>
  );
}
