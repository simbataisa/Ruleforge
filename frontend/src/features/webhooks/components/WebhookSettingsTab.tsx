import { useState, useEffect } from 'react';
import { AlertTriangle } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { ConfirmDialog } from '@/shared/components/ConfirmDialog';
import {
  useUpdateWebhook,
  useDeleteWebhook,
  type Webhook,
  type WebhookAuthType,
  type WebhookAuthConfig,
} from '@/api/webhooks.api';
import { useToastStore } from '@/stores/toast.store';
import { useAuthStore } from '@/stores/auth.store';
import { WebhookAuthConfigForm, PasswordInput } from './WebhookAuthConfigForm';

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

/** Strip credential fields that are empty so we don't overwrite existing values. */
function buildAuthConfigPayload(
  authType: WebhookAuthType,
  authConfig: Partial<WebhookAuthConfig>,
): Partial<WebhookAuthConfig> {
  const result: Partial<WebhookAuthConfig> = {};

  if (authType === 'API_KEY') {
    if (authConfig.headerName) result.headerName = authConfig.headerName;
    if (authConfig.apiKey) result.apiKey = authConfig.apiKey;
  } else if (authType === 'BASIC_AUTH') {
    if (authConfig.username) result.username = authConfig.username;
    if (authConfig.password) result.password = authConfig.password;
  } else if (authType === 'BEARER_TOKEN') {
    if (authConfig.token) result.token = authConfig.token;
  } else if (authType === 'SIGNING_SECRET') {
    if (authConfig.secret) result.secret = authConfig.secret;
  } else if (authType === 'CUSTOM_HEADER') {
    if (authConfig.headerName) result.headerName = authConfig.headerName;
    if (authConfig.headerValue) result.headerValue = authConfig.headerValue;
  } else if (authType === 'OAUTH2_CLIENT_CREDENTIALS') {
    if (authConfig.tokenUrl) result.tokenUrl = authConfig.tokenUrl;
    if (authConfig.clientId) result.clientId = authConfig.clientId;
    if (authConfig.clientSecret) result.clientSecret = authConfig.clientSecret;
    if (authConfig.scope) result.scope = authConfig.scope;
  }

  return result;
}

function authTypeSummary(authType: WebhookAuthType | undefined, authConfig: WebhookAuthConfig | undefined, isRestricted: boolean): string {
  const masked = isRestricted ? '[Restricted]' : '••••••••••••';

  switch (authType) {
    case 'NONE':
    case undefined:
      return 'None — No authentication';
    case 'SIGNING_SECRET':
      return `Signing Secret · Secret: ${masked}`;
    case 'API_KEY':
      return `API Key · Header: ${authConfig?.headerName ?? 'X-API-Key'} · Key: ${masked}`;
    case 'BASIC_AUTH':
      return `Basic Auth · Username: ${authConfig?.username ?? '—'} · Password: ${masked}`;
    case 'BEARER_TOKEN':
      return `Bearer Token · Token: ${masked}`;
    case 'CUSTOM_HEADER':
      return `Custom Header · Header: ${authConfig?.headerName ?? '—'} · Value: ${masked}`;
    case 'OAUTH2_CLIENT_CREDENTIALS':
      return `OAuth 2.0 · Token URL: ${authConfig?.tokenUrl ?? '—'} · Client ID: ${authConfig?.clientId ?? '—'} · Secret: ${masked}`;
    default:
      return String(authType);
  }
}

export function WebhookSettingsTab({ webhook, onUpdated, onDeleted }: Props) {
  const addToast = useToastStore((s) => s.addToast);
  const updateWebhook = useUpdateWebhook();
  const deleteWebhook = useDeleteWebhook();
  const currentUser = useAuthStore((s) => s.user);

  const [name, setName] = useState(webhook.name ?? '');
  const [url, setUrl] = useState(webhook.url);
  const [selectedEvents, setSelectedEvents] = useState<string[]>(webhook.events);
  const [active, setActive] = useState(webhook.active);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  // Inline rotate state for credential fields
  const [rotatingField, setRotatingField] = useState<string | null>(null);
  const [rotateValue, setRotateValue] = useState('');

  // Auth section state
  const [authEditMode, setAuthEditMode] = useState(false);
  const [editAuthType, setEditAuthType] = useState<WebhookAuthType>(webhook.authType ?? 'NONE');
  const [editAuthConfig, setEditAuthConfig] = useState<Partial<WebhookAuthConfig>>({
    headerName: webhook.authConfig?.headerName,
    username: webhook.authConfig?.username,
    tokenUrl: webhook.authConfig?.tokenUrl,
    clientId: webhook.authConfig?.clientId,
    scope: webhook.authConfig?.scope,
    // Credential fields (secret, apiKey, password, token, headerValue) intentionally left blank
  });

  const isViewer = currentUser?.role === 'VIEWER';
  const isOwner = currentUser?.role === 'OWNER';
  const isEditor = currentUser?.role === 'EDITOR';
  const canEdit = isOwner || isEditor;

  // Sync when webhook changes
  useEffect(() => {
    setName(webhook.name ?? '');
    setUrl(webhook.url);
    setSelectedEvents(webhook.events);
    setActive(webhook.active);
    setEditAuthType(webhook.authType ?? 'NONE');
    setEditAuthConfig({
      headerName: webhook.authConfig?.headerName,
      username: webhook.authConfig?.username,
      tokenUrl: webhook.authConfig?.tokenUrl,
      clientId: webhook.authConfig?.clientId,
      scope: webhook.authConfig?.scope,
    });
    setAuthEditMode(false);
    setRotatingField(null);
    setRotateValue('');
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

  const handleSaveAuth = () => {
    const authConfigPayload = buildAuthConfigPayload(editAuthType, editAuthConfig);
    updateWebhook.mutate(
      { id: webhook.id, authType: editAuthType, authConfig: authConfigPayload },
      {
        onSuccess: () => {
          addToast({ type: 'success', title: 'Authentication updated' });
          setAuthEditMode(false);
          onUpdated();
        },
        onError: () => addToast({ type: 'error', title: 'Failed to update authentication' }),
      },
    );
  };

  const handleAuthChange = (authType: WebhookAuthType, authConfig: Partial<WebhookAuthConfig>) => {
    setEditAuthType(authType);
    setEditAuthConfig(authConfig);
  };

  const handleRotateSave = (field: keyof WebhookAuthConfig) => {
    const authConfigPayload: Partial<WebhookAuthConfig> = { [field]: rotateValue };
    if (webhook.authType === 'CUSTOM_HEADER') {
      authConfigPayload.headerName = webhook.authConfig?.headerName ?? 'X-Custom-Header';
    } else if (webhook.authType === 'OAUTH2_CLIENT_CREDENTIALS') {
      authConfigPayload.tokenUrl = webhook.authConfig?.tokenUrl;
      authConfigPayload.clientId = webhook.authConfig?.clientId;
      if (webhook.authConfig?.scope) authConfigPayload.scope = webhook.authConfig.scope;
    }
    updateWebhook.mutate(
      { id: webhook.id, authType: webhook.authType, authConfig: authConfigPayload },
      {
        onSuccess: () => {
          addToast({ type: 'success', title: 'Authentication updated' });
          setRotatingField(null);
          setRotateValue('');
          onUpdated();
        },
        onError: () => addToast({ type: 'error', title: 'Failed to update credential' }),
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

      {/* Authentication card */}
      <div className="rounded-lg border border-gray-200 p-5 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-gray-900">Authentication</h3>
          {canEdit && !authEditMode && (
            <Button variant="outline" size="sm" onClick={() => setAuthEditMode(true)}>
              Edit
            </Button>
          )}
        </div>

        {authEditMode ? (
          <div className="space-y-4">
            <WebhookAuthConfigForm
              authType={editAuthType}
              authConfig={editAuthConfig}
              onChange={handleAuthChange}
              disabled={updateWebhook.isPending}
            />
            <p className="text-xs text-gray-400">
              Leave credential fields blank to keep current values.
            </p>
            <div className="flex justify-end gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setAuthEditMode(false);
                  setEditAuthType(webhook.authType ?? 'NONE');
                  setEditAuthConfig({
                    headerName: webhook.authConfig?.headerName,
                    username: webhook.authConfig?.username,
                    tokenUrl: webhook.authConfig?.tokenUrl,
                    clientId: webhook.authConfig?.clientId,
                    scope: webhook.authConfig?.scope,
                  });
                }}
                disabled={updateWebhook.isPending}
              >
                Cancel
              </Button>
              {canEdit && (
                <Button
                  size="sm"
                  onClick={handleSaveAuth}
                  loading={updateWebhook.isPending}
                  disabled={updateWebhook.isPending}
                >
                  Save
                </Button>
              )}
            </div>
          </div>
        ) : (
          <>
            {(webhook.authType !== 'CUSTOM_HEADER' && webhook.authType !== 'OAUTH2_CLIENT_CREDENTIALS') && (
              <p className="text-sm text-gray-700">
                {authTypeSummary(webhook.authType, webhook.authConfig, isViewer)}
              </p>
            )}

            {webhook.authType === 'CUSTOM_HEADER' && (
              isViewer ? (
                <p className="text-sm text-gray-700">Authentication: Custom Header</p>
              ) : (
                <div className="space-y-2 text-sm text-gray-700">
                  <div className="flex items-center gap-2">
                    <span className="font-medium w-28 shrink-0">Header Name:</span>
                    <span>{webhook.authConfig?.headerName ?? '—'}</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <span className="font-medium w-28 shrink-0">Header Value:</span>
                    {rotatingField === 'headerValue' ? (
                      <div className="flex items-center gap-2 flex-1">
                        <PasswordInput
                          id="rotate-header-value"
                          value={rotateValue}
                          onChange={(v) => setRotateValue(v)}
                          placeholder="Enter new header value"
                          ariaLabel="New header value"
                        />
                        <Button size="sm" onClick={() => handleRotateSave('headerValue')} loading={updateWebhook.isPending} disabled={!rotateValue || updateWebhook.isPending}>Save</Button>
                        <Button size="sm" variant="outline" onClick={() => { setRotatingField(null); setRotateValue(''); }} disabled={updateWebhook.isPending}>Cancel</Button>
                      </div>
                    ) : (
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-gray-400">[REDACTED]</span>
                        {canEdit && (
                          <Button size="sm" variant="outline" onClick={() => { setRotatingField('headerValue'); setRotateValue(''); }}>
                            Rotate Value ↺
                          </Button>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              )
            )}

            {webhook.authType === 'OAUTH2_CLIENT_CREDENTIALS' && (
              isViewer ? (
                <p className="text-sm text-gray-700">Authentication: OAuth 2.0 Client Credentials</p>
              ) : (
                <div className="space-y-2 text-sm text-gray-700">
                  <div className="flex items-center gap-2">
                    <span className="font-medium w-28 shrink-0">Token URL:</span>
                    <span className="break-all">{webhook.authConfig?.tokenUrl ?? '—'}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="font-medium w-28 shrink-0">Client ID:</span>
                    <span>{webhook.authConfig?.clientId ?? '—'}</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <span className="font-medium w-28 shrink-0">Client Secret:</span>
                    {rotatingField === 'clientSecret' ? (
                      <div className="flex items-center gap-2 flex-1">
                        <PasswordInput
                          id="rotate-client-secret"
                          value={rotateValue}
                          onChange={(v) => setRotateValue(v)}
                          placeholder="Enter new client secret"
                          ariaLabel="New client secret"
                        />
                        <Button size="sm" onClick={() => handleRotateSave('clientSecret')} loading={updateWebhook.isPending} disabled={!rotateValue || updateWebhook.isPending}>Save</Button>
                        <Button size="sm" variant="outline" onClick={() => { setRotatingField(null); setRotateValue(''); }} disabled={updateWebhook.isPending}>Cancel</Button>
                      </div>
                    ) : (
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-gray-400">[REDACTED]</span>
                        {canEdit && (
                          <Button size="sm" variant="outline" onClick={() => { setRotatingField('clientSecret'); setRotateValue(''); }}>
                            Rotate Secret ↺
                          </Button>
                        )}
                      </div>
                    )}
                  </div>
                  {webhook.authConfig?.scope && (
                    <div className="flex items-center gap-2">
                      <span className="font-medium w-28 shrink-0">Scope:</span>
                      <span>{webhook.authConfig.scope}</span>
                    </div>
                  )}
                </div>
              )
            )}
          </>
        )}
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
