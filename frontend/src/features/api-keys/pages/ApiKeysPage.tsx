import { useState, useMemo } from 'react';
import { Plus, Copy, Check, Key, AlertTriangle } from 'lucide-react';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { Badge } from '@/shared/components/Badge';
import { DataTable } from '@/shared/components/DataTable';
import { Modal } from '@/shared/components/Modal';
import { Spinner } from '@/shared/components/Spinner';
import { EmptyState } from '@/shared/components/EmptyState';
import { useApiKeys, useCreateApiKey, useRevokeApiKey } from '@/api/apikeys.api';
import { useToastStore } from '@/stores/toast.store';
import type { ApiKey, CreateApiKeyResponse } from '@/api/types/apikey.types';

type ApiKeyType = 'SOLVER' | 'MANAGEMENT';

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

function isExpired(expiresAt?: string): boolean {
  if (!expiresAt) return false;
  return new Date(expiresAt) < new Date();
}

export function ApiKeysPage() {
  const addToast = useToastStore((s) => s.addToast);

  const [showCreate, setShowCreate] = useState(false);
  const [showRevoke, setShowRevoke] = useState<ApiKey | null>(null);
  const [showKeyResult, setShowKeyResult] = useState<CreateApiKeyResponse | null>(null);
  const [copied, setCopied] = useState(false);

  // Create form state
  const [createName, setCreateName] = useState('');
  const [createType, setCreateType] = useState<ApiKeyType>('SOLVER');
  const [createExpiration, setCreateExpiration] = useState('');

  const { data: apiKeysData, isLoading } = useApiKeys();
  const createApiKey = useCreateApiKey();
  const revokeApiKey = useRevokeApiKey();

  const apiKeys: ApiKey[] = useMemo(() => {
    return Array.isArray(apiKeysData) ? apiKeysData : [];
  }, [apiKeysData]);

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    if (!createName.trim()) return;

    const permissions =
      createType === 'SOLVER' ? ['rule:solve'] : ['rule:manage', 'rule:solve'];
    const expiresInDays = createExpiration
      ? Math.ceil(
          (new Date(createExpiration).getTime() - Date.now()) /
            (1000 * 60 * 60 * 24),
        )
      : undefined;

    createApiKey.mutate(
      { name: createName.trim(), permissions, expiresInDays },
      {
        onSuccess: (response) => {
          setShowCreate(false);
          setCreateName('');
          setCreateType('SOLVER');
          setCreateExpiration('');
          setShowKeyResult(response);
          addToast({ type: 'success', title: 'API key created' });
        },
        onError: () => {
          addToast({ type: 'error', title: 'Failed to create API key' });
        },
      },
    );
  };

  const handleRevoke = () => {
    if (!showRevoke) return;
    revokeApiKey.mutate(showRevoke.id, {
      onSuccess: () => {
        setShowRevoke(null);
        addToast({ type: 'success', title: 'API key revoked' });
      },
      onError: () => {
        addToast({ type: 'error', title: 'Failed to revoke API key' });
      },
    });
  };

  const handleCopyKey = (key: string) => {
    navigator.clipboard.writeText(key);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const getKeyType = (permissions: string[]): string => {
    if (permissions.includes('rule:manage')) return 'MANAGEMENT';
    return 'SOLVER';
  };

  const columns = [
    {
      key: 'name' as const,
      header: 'Name',
      render: (key: ApiKey) => (
        <div className="flex items-center gap-2">
          <Key className="h-4 w-4 text-gray-400" />
          <span className="font-medium text-gray-900">{key.name}</span>
        </div>
      ),
    },
    {
      key: 'prefix' as const,
      header: 'Key Prefix',
      render: (key: ApiKey) => (
        <code className="text-xs bg-gray-100 px-2 py-0.5 rounded text-gray-600 font-mono">
          {key.prefix}...
        </code>
      ),
    },
    {
      key: 'permissions' as const,
      header: 'Type',
      render: (key: ApiKey) => {
        const type = getKeyType(key.permissions);
        return (
          <Badge
            className={
              type === 'MANAGEMENT'
                ? 'bg-purple-100 text-purple-700'
                : 'bg-blue-100 text-blue-700'
            }
          >
            {type}
          </Badge>
        );
      },
    },
    {
      key: 'createdAt' as const,
      header: 'Created',
      render: (key: ApiKey) => (
        <span className="text-gray-500 text-sm">{formatDate(key.createdAt)}</span>
      ),
    },
    {
      key: 'expiresAt' as const,
      header: 'Expires',
      render: (key: ApiKey) => {
        if (!key.expiresAt) {
          return <span className="text-gray-400 text-sm">Never</span>;
        }
        const expired = isExpired(key.expiresAt);
        return (
          <span
            className={`text-sm ${expired ? 'text-red-600 font-medium' : 'text-gray-500'}`}
          >
            {formatDate(key.expiresAt)}
            {expired && ' (Expired)'}
          </span>
        );
      },
    },
    {
      key: 'id' as const,
      header: '',
      render: (key: ApiKey) => (
        <Button
          size="sm"
          variant="danger"
          onClick={(e) => {
            e.stopPropagation();
            setShowRevoke(key);
          }}
        >
          Revoke
        </Button>
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

  return (
    <div className="flex flex-col h-full">
      <PageHeader
        title="API Keys"
        actions={
          <Button onClick={() => setShowCreate(true)}>
            <Plus className="h-4 w-4" />
            Create API Key
          </Button>
        }
      />

      <div className="flex-1 overflow-auto p-6">
        {apiKeys.length === 0 ? (
          <EmptyState
            title="No API keys"
            description="Create an API key to start integrating with the RuleForge API."
          />
        ) : (
          <DataTable data={apiKeys} columns={columns} />
        )}
      </div>

      {/* Create API Key Modal */}
      <Modal
        open={showCreate}
        onClose={() => setShowCreate(false)}
        title="Create API Key"
      >
        <form onSubmit={handleCreate} className="space-y-4">
          <div>
            <label
              htmlFor="key-name"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Name
            </label>
            <Input
              id="key-name"
              value={createName}
              onChange={(e) => setCreateName(e.target.value)}
              placeholder="My API Key"
              autoFocus
              required
            />
          </div>
          <div>
            <label
              htmlFor="key-type"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Type
            </label>
            <Select
              id="key-type"
              value={createType}
              onChange={(e) => setCreateType(e.target.value as ApiKeyType)}
            >
              <option value="SOLVER">Solver (execute rules only)</option>
              <option value="MANAGEMENT">
                Management (manage + execute rules)
              </option>
            </Select>
          </div>
          <div>
            <label
              htmlFor="key-expiration"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Expiration Date (optional)
            </label>
            <Input
              id="key-expiration"
              type="date"
              value={createExpiration}
              onChange={(e) => setCreateExpiration(e.target.value)}
              min={new Date().toISOString().split('T')[0]}
            />
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => setShowCreate(false)}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              loading={createApiKey.isPending}
              disabled={!createName.trim()}
            >
              Create Key
            </Button>
          </div>
        </form>
      </Modal>

      {/* Show Key Result Modal */}
      <Modal
        open={showKeyResult !== null}
        onClose={() => setShowKeyResult(null)}
        title="API Key Created"
      >
        <div className="space-y-4">
          <div className="flex items-start gap-3 bg-amber-50 border border-amber-200 rounded-lg p-3">
            <AlertTriangle className="h-5 w-5 text-amber-500 flex-shrink-0 mt-0.5" />
            <p className="text-sm text-amber-800">
              Make sure to copy your API key now. You will not be able to see it
              again.
            </p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              API Key
            </label>
            <div className="flex items-center gap-2">
              <code className="flex-1 bg-gray-900 text-green-400 px-3 py-2 rounded font-mono text-sm break-all">
                {showKeyResult?.key}
              </code>
              <Button
                size="sm"
                variant="outline"
                onClick={() => handleCopyKey(showKeyResult?.key ?? '')}
              >
                {copied ? (
                  <Check className="h-4 w-4 text-green-500" />
                ) : (
                  <Copy className="h-4 w-4" />
                )}
              </Button>
            </div>
          </div>
          <div className="flex justify-end pt-2">
            <Button onClick={() => setShowKeyResult(null)}>Done</Button>
          </div>
        </div>
      </Modal>

      {/* Revoke Confirmation Modal */}
      <Modal
        open={showRevoke !== null}
        onClose={() => setShowRevoke(null)}
        title="Revoke API Key"
      >
        <div className="space-y-4">
          <p className="text-sm text-gray-600">
            Are you sure you want to revoke the API key{' '}
            <span className="font-semibold text-gray-900">
              {showRevoke?.name}
            </span>
            ? This action cannot be undone. Any applications using this key will
            lose access immediately.
          </p>
          <div className="flex justify-end gap-3 pt-2">
            <Button variant="outline" onClick={() => setShowRevoke(null)}>
              Cancel
            </Button>
            <Button
              variant="danger"
              onClick={handleRevoke}
              loading={revokeApiKey.isPending}
            >
              Revoke Key
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
