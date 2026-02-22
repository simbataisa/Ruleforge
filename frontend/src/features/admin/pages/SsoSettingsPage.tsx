import { useState, useEffect } from 'react';
import { Save } from 'lucide-react';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { Spinner } from '@/shared/components/Spinner';
import { useSsoConfig, useUpdateSsoConfig, type SsoProviderType } from '@/api/sso.api';
import { useToastStore } from '@/stores/toast.store';

export function SsoSettingsPage() {
  const addToast = useToastStore((s) => s.addToast);
  const { data: config, isLoading } = useSsoConfig();
  const updateConfig = useUpdateSsoConfig();

  const [providerType, setProviderType] = useState<SsoProviderType>('OIDC');
  const [enabled, setEnabled] = useState(false);
  const [issuerUrl, setIssuerUrl] = useState('');
  const [clientId, setClientId] = useState('');
  const [clientSecret, setClientSecret] = useState('');
  const [metadataUrl, setMetadataUrl] = useState('');

  useEffect(() => {
    if (config) {
      setProviderType(config.providerType);
      setEnabled(config.enabled);
      setIssuerUrl(config.issuerUrl ?? '');
      setClientId(config.clientId ?? '');
      setClientSecret(config.clientSecret ?? '');
      setMetadataUrl(config.metadataUrl ?? '');
    }
  }, [config]);

  const handleSave = () => {
    updateConfig.mutate(
      {
        providerType,
        enabled,
        issuerUrl: issuerUrl || undefined,
        clientId: clientId || undefined,
        clientSecret: clientSecret || undefined,
        metadataUrl: metadataUrl || undefined,
      },
      {
        onSuccess: () => addToast({ type: 'success', title: 'SSO settings saved' }),
        onError: () => addToast({ type: 'error', title: 'Failed to save SSO settings' }),
      },
    );
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="SSO Settings"
        description="Configure Single Sign-On for your organization."
        actions={
          <Button onClick={handleSave} disabled={updateConfig.isPending}>
            <Save className="h-4 w-4" />
            Save
          </Button>
        }
      />

      <div className="max-w-xl space-y-6">
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={enabled}
              onChange={(e) => setEnabled(e.target.checked)}
              className="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
            />
            <span className="font-medium text-gray-700">Enable SSO</span>
          </label>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Provider Type</label>
          <Select
            value={providerType}
            onChange={(e) => setProviderType(e.target.value as SsoProviderType)}
          >
            <option value="OIDC">OIDC (OpenID Connect)</option>
            <option value="SAML">SAML</option>
          </Select>
        </div>

        {providerType === 'SAML' && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Metadata URL</label>
            <Input
              value={metadataUrl}
              onChange={(e) => setMetadataUrl(e.target.value)}
              placeholder="https://idp.example.com/metadata.xml"
            />
          </div>
        )}

        {providerType === 'OIDC' && (
          <>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Issuer URL</label>
              <Input
                value={issuerUrl}
                onChange={(e) => setIssuerUrl(e.target.value)}
                placeholder="https://accounts.google.com"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Client ID</label>
              <Input
                value={clientId}
                onChange={(e) => setClientId(e.target.value)}
                placeholder="your-client-id"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Client Secret</label>
              <Input
                type="password"
                value={clientSecret}
                onChange={(e) => setClientSecret(e.target.value)}
                placeholder="your-client-secret"
              />
            </div>
          </>
        )}
      </div>
    </div>
  );
}
