import { useState } from 'react';
import { Copy, Check, Building2, Package, Folder, FileText } from 'lucide-react';
import { Modal } from '@/shared/components/Modal';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { useCreateWebhook, type WebhookScopeType, type WebhookAuthType, type WebhookAuthConfig, type WebhookRetryConfig } from '@/api/webhooks.api';
import { useOrgSpaces, useOrganization } from '@/api/org.api';
import { useToastStore } from '@/stores/toast.store';
import { WebhookAuthConfigForm } from './WebhookAuthConfigForm';
import { WebhookRetryConfigForm } from './WebhookRetryConfigForm';

const AVAILABLE_EVENTS = [
  'rule.created',
  'rule.updated',
  'rule.deleted',
  'rule.published',
  'rule.executed',
];

interface ScopeCard {
  type: WebhookScopeType;
  icon: React.ReactNode;
  title: string;
  description: string;
}

const SCOPE_CARDS: ScopeCard[] = [
  {
    type: 'ORG',
    icon: <Building2 className="h-6 w-6 text-purple-500" />,
    title: 'Organization',
    description: 'All events in your organization',
  },
  {
    type: 'SPACE',
    icon: <Package className="h-6 w-6 text-blue-500" />,
    title: 'Space',
    description: 'Events in a specific space',
  },
  {
    type: 'FOLDER',
    icon: <Folder className="h-6 w-6 text-yellow-500" />,
    title: 'Folder',
    description: 'Events in a specific folder',
  },
  {
    type: 'RULE',
    icon: <FileText className="h-6 w-6 text-green-500" />,
    title: 'Rule',
    description: 'Events for a specific rule',
  },
];

interface Props {
  onClose: () => void;
  onSuccess: () => void;
}

export function CreateWebhookModal({ onClose, onSuccess }: Props) {
  const addToast = useToastStore((s) => s.addToast);
  const createWebhook = useCreateWebhook();
  const { data: organization } = useOrganization();
  const { data: spaces } = useOrgSpaces();

  // Step state
  const [step, setStep] = useState<'scope' | 'details' | 'secret'>('scope');

  // Form state
  const [scopeType, setScopeType] = useState<WebhookScopeType | null>(null);
  const [scopeId, setScopeId] = useState('');
  const [name, setName] = useState('');
  const [url, setUrl] = useState('');
  const [selectedEvents, setSelectedEvents] = useState<string[]>([]);
  const [active, setActive] = useState(true);

  // Auth & retry state
  const [authType, setAuthType] = useState<WebhookAuthType>('NONE');
  const [authConfig, setAuthConfig] = useState<Partial<WebhookAuthConfig>>({});
  const [retryConfig, setRetryConfig] = useState<Partial<WebhookRetryConfig>>({});
  const [authError, setAuthError] = useState<string | null>(null);

  // Secret reveal state
  const [newSecret, setNewSecret] = useState('');
  const [copied, setCopied] = useState(false);

  const handleScopeSelect = (type: WebhookScopeType) => {
    setScopeType(type);
    setScopeId('');
    // For ORG scope, auto-set scopeId to org id
    if (type === 'ORG' && organization?.id) {
      setScopeId(organization.id);
    }
    setStep('details');
  };

  const toggleEvent = (event: string) => {
    setSelectedEvents((prev) =>
      prev.includes(event) ? prev.filter((e) => e !== event) : [...prev, event],
    );
  };

  const isDetailsValid = () => {
    if (!url || selectedEvents.length === 0) return false;
    if (!url.startsWith('https://')) return false;
    if (scopeType !== 'ORG' && !scopeId) return false;
    return true;
  };

  const validateAuth = (): string | null => {
    if (authType === 'API_KEY' && !authConfig.apiKey?.trim()) return 'API Key is required';
    if (authType === 'BASIC_AUTH' && !authConfig.username?.trim()) return 'Username is required';
    if (authType === 'BASIC_AUTH' && !authConfig.password?.trim()) return 'Password is required';
    if (authType === 'BEARER_TOKEN' && !authConfig.token?.trim()) return 'Token is required';
    return null;
  };

  const handleSubmit = () => {
    if (!scopeType) return;

    const authValidationError = validateAuth();
    if (authValidationError) {
      setAuthError(authValidationError);
      return;
    }
    setAuthError(null);

    const data = {
      url,
      events: selectedEvents,
      active,
      scopeType,
      scopeId: scopeType === 'ORG' && organization?.id ? organization.id : scopeId,
      ...(name ? { name } : {}),
      authType,
      ...(authType !== 'NONE' && { authConfig }),
      ...(Object.keys(retryConfig).length > 0 && { retryConfig }),
    };

    createWebhook.mutate(data, {
      onSuccess: (result) => {
        // result is CreateWebhookResponse { webhook, secret }
        const secret = result?.secretOnCreation ?? '';
        if (secret) {
          setNewSecret(secret);
          setStep('secret');
        } else {
          addToast({ type: 'success', title: 'Webhook created' });
          onSuccess();
        }
      },
      onError: () => addToast({ type: 'error', title: 'Failed to create webhook' }),
    });
  };

  const handleCopySecret = () => {
    navigator.clipboard.writeText(newSecret)
      .then(() => {
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      })
      .catch(() => {
        addToast({ type: 'error', title: 'Copy failed', message: 'Please copy the secret manually.' });
      });
  };

  const resetAuthState = () => {
    setAuthType('NONE');
    setAuthConfig({});
    setRetryConfig({});
    setAuthError(null);
  };

  const handleSecretDismiss = () => {
    resetAuthState();
    addToast({ type: 'success', title: 'Webhook created successfully' });
    onSuccess();
  };

  if (step === 'secret') {
    return (
      <Modal open onClose={handleSecretDismiss} title="Webhook Created">
        <div className="space-y-4">
          <div className="rounded-lg bg-amber-50 border border-amber-200 p-4">
            <p className="text-sm font-semibold text-amber-800 mb-2">
              Copy your webhook secret now — it will not be shown again.
            </p>
            <p className="text-xs text-amber-700 mb-3">
              Use this secret to verify webhook signatures on incoming requests.
            </p>
            <div className="flex items-center gap-2">
              <code className="flex-1 bg-white border border-amber-200 rounded px-3 py-2 text-xs font-mono text-gray-900 break-all">
                {newSecret}
              </code>
              <button
                onClick={handleCopySecret}
                className="flex-shrink-0 p-2 rounded-md bg-amber-100 hover:bg-amber-200 text-amber-800 transition-colors"
                aria-label="Copy secret"
              >
                {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
              </button>
            </div>
          </div>
          <div className="flex justify-end">
            <Button onClick={handleSecretDismiss}>Done</Button>
          </div>
        </div>
      </Modal>
    );
  }

  if (step === 'scope') {
    return (
      <Modal open onClose={onClose} title="Create Webhook — Choose Scope">
        <div className="grid grid-cols-2 gap-3">
          {SCOPE_CARDS.map((card) => (
            <button
              key={card.type}
              onClick={() => handleScopeSelect(card.type)}
              className="flex flex-col items-start gap-2 rounded-lg border-2 border-gray-200 p-4 text-left transition-colors hover:border-indigo-300 hover:bg-indigo-50 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              {card.icon}
              <div>
                <p className="text-sm font-semibold text-gray-900">{card.title}</p>
                <p className="text-xs text-gray-500 mt-0.5">{card.description}</p>
              </div>
            </button>
          ))}
        </div>
      </Modal>
    );
  }

  // Step: details
  return (
    <Modal
      open
      onClose={onClose}
      title={`Create Webhook — ${scopeType ?? ''}`}
      footer={
        <>
          <Button variant="outline" onClick={() => setStep('scope')}>
            Back
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={!isDetailsValid() || createWebhook.isPending}
            loading={createWebhook.isPending}
          >
            Create
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        {/* Scope target selector */}
        {scopeType === 'SPACE' && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Space</label>
            <select
              value={scopeId}
              onChange={(e) => setScopeId(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="">Select a space...</option>
              {(spaces ?? []).map((s) => (
                <option key={s.id} value={s.id}>{s.name}</option>
              ))}
            </select>
          </div>
        )}

        {(scopeType === 'FOLDER' || scopeType === 'RULE') && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {scopeType === 'FOLDER' ? 'Folder' : 'Rule'} ID
            </label>
            <Input
              value={scopeId}
              onChange={(e) => setScopeId(e.target.value)}
              placeholder={`Enter ${scopeType === 'FOLDER' ? 'folder' : 'rule'} ID`}
            />
            <p className="mt-1 text-xs text-gray-500">
              Enter the ID of the {scopeType === 'FOLDER' ? 'folder' : 'rule'} to scope this webhook to.
            </p>
          </div>
        )}

        {/* Name (optional) */}
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

        {/* URL */}
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

        {/* Events */}
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

        {/* Active toggle */}
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

        {/* Authentication */}
        <div className="border-t pt-4">
          <p className="text-sm font-medium text-gray-900 mb-3">Authentication</p>
          <WebhookAuthConfigForm
            authType={authType}
            authConfig={authConfig}
            onChange={(type, config) => {
              setAuthType(type);
              setAuthConfig(config);
              setAuthError(null);
            }}
          />
          {authError && <p className="text-sm text-red-600 mt-2">{authError}</p>}
        </div>

        {/* Retry config */}
        <WebhookRetryConfigForm
          retryConfig={retryConfig}
          onChange={setRetryConfig}
        />
      </div>
    </Modal>
  );
}
