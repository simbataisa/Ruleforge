import { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { Button } from '@/shared/components/Button';
import type { WebhookAuthConfig, WebhookAuthType } from '@/api/webhooks.api';

interface WebhookAuthConfigFormProps {
  authType: WebhookAuthType;
  authConfig: Partial<WebhookAuthConfig>;
  onChange: (authType: WebhookAuthType, authConfig: Partial<WebhookAuthConfig>) => void;
  disabled?: boolean;
}

const AUTH_TYPE_OPTIONS: Array<{ value: WebhookAuthType; label: string; description: string }> = [
  { value: 'NONE', label: 'None', description: 'No authentication. Use for public or VPN-protected endpoints.' },
  { value: 'SIGNING_SECRET', label: 'Signing Secret', description: 'RuleForge signs each request with HMAC-SHA256.' },
  { value: 'API_KEY', label: 'API Key', description: 'Sends a key in a custom header (e.g., X-API-Key).' },
  { value: 'BASIC_AUTH', label: 'Basic Auth', description: 'Sends username and password via Authorization: Basic.' },
  { value: 'BEARER_TOKEN', label: 'Bearer Token', description: 'Sends a static token via Authorization: Bearer.' },
];

interface PasswordInputProps {
  id: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  disabled?: boolean;
  ariaLabel?: string;
}

function PasswordInput({ id, value, onChange, placeholder, disabled, ariaLabel }: PasswordInputProps) {
  const [visible, setVisible] = useState(false);

  const show = () => {
    setVisible(true);
    setTimeout(() => setVisible(false), 30000);
  };

  return (
    <div className="relative">
      <input
        id={id}
        type={visible ? 'text' : 'password'}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        disabled={disabled}
        aria-label={ariaLabel}
        autoComplete="new-password"
        className="block w-full rounded-lg border border-gray-300 bg-white px-3 py-2 pr-10 text-sm text-gray-900 placeholder:text-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 disabled:cursor-not-allowed disabled:bg-gray-50 disabled:text-gray-500"
      />
      <Button
        type="button"
        variant="ghost"
        size="sm"
        className="absolute right-0 top-0 h-full px-3 hover:bg-transparent"
        onClick={() => (visible ? setVisible(false) : show())}
        aria-label={visible ? `Hide ${ariaLabel ?? 'value'}` : `Show ${ariaLabel ?? 'value'}`}
        disabled={disabled}
        tabIndex={-1}
      >
        {visible
          ? <EyeOff className="h-4 w-4 text-gray-400" />
          : <Eye className="h-4 w-4 text-gray-400" />}
      </Button>
    </div>
  );
}

export function WebhookAuthConfigForm({ authType, authConfig, onChange, disabled }: WebhookAuthConfigFormProps) {
  const selectedOption = AUTH_TYPE_OPTIONS.find((o) => o.value === authType);

  const handleTypeChange = (newType: WebhookAuthType) => {
    onChange(newType, {});
  };

  const update = (patch: Partial<WebhookAuthConfig>) => {
    onChange(authType, { ...authConfig, ...patch });
  };

  return (
    <div className="space-y-3">
      <div>
        <Select
          label="Auth Type"
          id="auth-type"
          value={authType}
          onChange={(e) => handleTypeChange(e.target.value as WebhookAuthType)}
          disabled={disabled}
        >
          {AUTH_TYPE_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </Select>
        {selectedOption && (
          <p className="mt-1 text-xs text-gray-500">{selectedOption.description}</p>
        )}
      </div>

      {authType === 'API_KEY' && (
        <>
          <Input
            id="header-name"
            label="Header Name"
            value={authConfig.headerName ?? 'X-API-Key'}
            onChange={(e) => update({ headerName: e.target.value })}
            placeholder="X-API-Key"
            disabled={disabled}
          />
          <div>
            <label htmlFor="api-key" className="mb-1.5 block text-sm font-medium text-gray-700">
              API Key *
            </label>
            <PasswordInput
              id="api-key"
              value={authConfig.apiKey ?? ''}
              onChange={(v) => update({ apiKey: v })}
              placeholder="Enter API key"
              disabled={disabled}
              ariaLabel="API Key"
            />
          </div>
          <p className="text-xs text-gray-500">
            Sent as: <code className="rounded bg-gray-100 px-1">{authConfig.headerName || 'X-API-Key'}: [key]</code>
          </p>
        </>
      )}

      {authType === 'BASIC_AUTH' && (
        <>
          <Input
            id="username"
            label="Username *"
            value={authConfig.username ?? ''}
            onChange={(e) => update({ username: e.target.value })}
            placeholder="Enter username"
            disabled={disabled}
            autoComplete="username"
          />
          <div>
            <label htmlFor="password" className="mb-1.5 block text-sm font-medium text-gray-700">
              Password *
            </label>
            <PasswordInput
              id="password"
              value={authConfig.password ?? ''}
              onChange={(v) => update({ password: v })}
              placeholder="Enter password"
              disabled={disabled}
              ariaLabel="Password"
            />
          </div>
          <p className="text-xs text-gray-500">
            Sent as: <code className="rounded bg-gray-100 px-1">Authorization: Basic [base64(username:password)]</code>
          </p>
        </>
      )}

      {authType === 'BEARER_TOKEN' && (
        <>
          <div>
            <label htmlFor="bearer-token" className="mb-1.5 block text-sm font-medium text-gray-700">
              Token *
            </label>
            <PasswordInput
              id="bearer-token"
              value={authConfig.token ?? ''}
              onChange={(v) => update({ token: v })}
              placeholder="Enter bearer token"
              disabled={disabled}
              ariaLabel="Bearer Token"
            />
          </div>
          <p className="text-xs text-gray-500">
            Sent as: <code className="rounded bg-gray-100 px-1">Authorization: Bearer [token]</code>
          </p>
        </>
      )}

      {authType === 'SIGNING_SECRET' && (
        <>
          <div>
            <label htmlFor="signing-secret" className="mb-1.5 block text-sm font-medium text-gray-700">
              Secret
            </label>
            <PasswordInput
              id="signing-secret"
              value={authConfig.secret ?? ''}
              onChange={(v) => update({ secret: v })}
              placeholder="Auto-generated if left blank"
              disabled={disabled}
              ariaLabel="Signing Secret"
            />
          </div>
          <p className="text-xs text-gray-500">
            Adds <code className="rounded bg-gray-100 px-1">X-RuleForge-Signature</code> header to each request using HMAC-SHA256.
          </p>
        </>
      )}

      {authType === 'NONE' && (
        <p className="text-xs text-gray-500">
          No authentication header will be added to outbound requests.
        </p>
      )}
    </div>
  );
}
