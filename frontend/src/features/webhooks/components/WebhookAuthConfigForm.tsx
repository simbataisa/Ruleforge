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
  { value: 'CUSTOM_HEADER', label: 'Custom Header', description: 'Sends any value in a header you specify. Flexible — for proprietary API auth.' },
  { value: 'OAUTH2_CLIENT_CREDENTIALS', label: 'OAuth 2.0 (Client Credentials)', description: 'Fetches a Bearer token automatically using client credentials. For Salesforce, Azure AD, Okta, and similar.' },
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

      {authType === 'CUSTOM_HEADER' && (
        <>
          <Input
            id="custom-header-name"
            label="Header Name"
            value={authConfig.headerName ?? 'X-Custom-Header'}
            onChange={(e) => update({ headerName: e.target.value })}
            placeholder="e.g. X-Service-Auth"
            disabled={disabled}
          />
          <p className="mt-1 text-xs text-gray-500">
            The exact header name your API expects (case-sensitive).
          </p>
          <div>
            <label htmlFor="custom-header-value" className="mb-1.5 block text-sm font-medium text-gray-700">
              Header Value *
            </label>
            <PasswordInput
              id="custom-header-value"
              value={authConfig.headerValue ?? ''}
              onChange={(v) => update({ headerValue: v })}
              placeholder="Enter header value"
              disabled={disabled}
              ariaLabel="Header Value"
            />
            <p className="mt-1 text-xs text-gray-500">Stored encrypted. Never shown after saving.</p>
          </div>
        </>
      )}

      {authType === 'OAUTH2_CLIENT_CREDENTIALS' && (
        <>
          <div>
            <Input
              id="oauth2-token-url"
              label="Token URL"
              type="url"
              value={authConfig.tokenUrl ?? ''}
              onChange={(e) => update({ tokenUrl: e.target.value })}
              placeholder="https://auth.example.com/oauth/token"
              disabled={disabled}
            />
            {authConfig.tokenUrl && !authConfig.tokenUrl.startsWith('https://') && (
              <p className="mt-1 text-xs text-amber-600" role="alert">⚠️ Must use HTTPS.</p>
            )}
          </div>
          <Input
            id="oauth2-client-id"
            label="Client ID"
            value={authConfig.clientId ?? ''}
            onChange={(e) => update({ clientId: e.target.value })}
            disabled={disabled}
            autoComplete="off"
          />
          <div>
            <label htmlFor="oauth2-client-secret" className="mb-1.5 block text-sm font-medium text-gray-700">
              Client Secret *
            </label>
            <PasswordInput
              id="oauth2-client-secret"
              value={authConfig.clientSecret ?? ''}
              onChange={(v) => update({ clientSecret: v })}
              placeholder="Enter client secret"
              disabled={disabled}
              ariaLabel="Client Secret"
            />
            <p className="mt-1 text-xs text-gray-500">Stored encrypted. Never shown after saving.</p>
          </div>
          <Input
            id="oauth2-scope"
            label="Scope"
            value={authConfig.scope ?? ''}
            onChange={(e) => update({ scope: e.target.value })}
            placeholder="e.g. webhooks:write api:read"
            disabled={disabled}
          />
          <details className="mt-1">
            <summary className="cursor-pointer text-xs text-gray-500 hover:text-gray-700">
              ▶ Where to find these values
            </summary>
            <div className="mt-2 rounded-md bg-gray-50 p-3 text-xs text-gray-500 space-y-1">
              <p><strong>Salesforce:</strong> Setup → Apps → Connected App → Consumer Key/Secret; Token URL: https://login.salesforce.com/services/oauth2/token</p>
              <p><strong>Azure AD:</strong> App registrations → Certificates &amp; secrets; Token URL: https://login.microsoftonline.com/&#123;tenant&#125;/oauth2/v2.0/token</p>
              <p><strong>Okta:</strong> Applications → API services; Token URL: https://&#123;domain&#125;/oauth2/default/v1/token</p>
              <p><strong>Google:</strong> Service Account → Keys; Token URL: https://oauth2.googleapis.com/token</p>
            </div>
          </details>
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
