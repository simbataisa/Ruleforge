import { useState, useEffect } from 'react';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { Spinner } from '@/shared/components/Spinner';
import { useToastStore } from '@/stores/toast.store';
import {
  useSsoConfig,
  useUpdateSsoConfig,
  type SsoProviderType,
} from '@/api/sso.api';

export function SsoSettings() {
  const addToast = useToastStore((s) => s.addToast);
  const { data: config, isLoading } = useSsoConfig();
  const updateConfig = useUpdateSsoConfig();

  const [providerType, setProviderType] = useState<SsoProviderType>('OIDC');
  const [enabled, setEnabled] = useState(false);
  const [issuerUrl, setIssuerUrl] = useState('');
  const [clientId, setClientId] = useState('');
  const [clientSecret, setClientSecret] = useState('');
  const [metadataUrl, setMetadataUrl] = useState('');
  const [entityId, setEntityId] = useState('');
  const [certificate, setCertificate] = useState('');

  useEffect(() => {
    if (config) {
      setProviderType(config.providerType);
      setEnabled(config.enabled);
      setIssuerUrl(config.issuerUrl ?? '');
      setClientId(config.clientId ?? '');
      setClientSecret(config.clientSecret ?? '');
      setMetadataUrl(config.metadataUrl ?? '');
      setEntityId(config.entityId ?? '');
      setCertificate(config.certificate ?? '');
    }
  }, [config]);

  const handleSave = async () => {
    try {
      await updateConfig.mutateAsync({
        providerType,
        enabled,
        issuerUrl: issuerUrl || undefined,
        clientId: clientId || undefined,
        clientSecret: clientSecret || undefined,
        metadataUrl: metadataUrl || undefined,
        entityId: entityId || undefined,
        certificate: certificate || undefined,
      });
      addToast({ type: 'success', title: 'SSO configuration saved' });
    } catch {
      addToast({ type: 'error', title: 'Failed to save SSO configuration' });
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Spinner />
      </div>
    );
  }

  return (
    <div className="max-w-lg space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-sm font-medium text-gray-900">
            Single Sign-On (SSO)
          </h3>
          <p className="text-xs text-gray-500 mt-0.5">
            Configure SAML or OIDC for your organization.
          </p>
        </div>
        <label className="relative inline-flex cursor-pointer items-center">
          <input
            type="checkbox"
            checked={enabled}
            onChange={(e) => setEnabled(e.target.checked)}
            className="peer sr-only"
          />
          <div className="h-6 w-11 rounded-full bg-gray-200 after:absolute after:left-[2px] after:top-[2px] after:h-5 after:w-5 after:rounded-full after:border after:border-gray-300 after:bg-white after:transition-all after:content-[''] peer-checked:bg-indigo-600 peer-checked:after:translate-x-full peer-checked:after:border-white" />
        </label>
      </div>

      <div>
        <label htmlFor="sso-type" className="block text-sm font-medium text-gray-700 mb-1">
          Provider Type
        </label>
        <Select
          id="sso-type"
          value={providerType}
          onChange={(e) => setProviderType(e.target.value as SsoProviderType)}
        >
          <option value="OIDC">OpenID Connect (OIDC)</option>
          <option value="SAML">SAML 2.0</option>
        </Select>
      </div>

      {providerType === 'OIDC' && (
        <>
          <div>
            <label htmlFor="sso-issuer" className="block text-sm font-medium text-gray-700 mb-1">
              Issuer URL
            </label>
            <Input
              id="sso-issuer"
              value={issuerUrl}
              onChange={(e) => setIssuerUrl(e.target.value)}
              placeholder="https://accounts.google.com"
            />
          </div>
          <div>
            <label htmlFor="sso-client-id" className="block text-sm font-medium text-gray-700 mb-1">
              Client ID
            </label>
            <Input
              id="sso-client-id"
              value={clientId}
              onChange={(e) => setClientId(e.target.value)}
            />
          </div>
          <div>
            <label htmlFor="sso-client-secret" className="block text-sm font-medium text-gray-700 mb-1">
              Client Secret
            </label>
            <Input
              id="sso-client-secret"
              type="password"
              value={clientSecret}
              onChange={(e) => setClientSecret(e.target.value)}
            />
          </div>
        </>
      )}

      {providerType === 'SAML' && (
        <>
          <div>
            <label htmlFor="sso-metadata" className="block text-sm font-medium text-gray-700 mb-1">
              Metadata URL
            </label>
            <Input
              id="sso-metadata"
              value={metadataUrl}
              onChange={(e) => setMetadataUrl(e.target.value)}
              placeholder="https://idp.example.com/metadata"
            />
          </div>
          <div>
            <label htmlFor="sso-entity" className="block text-sm font-medium text-gray-700 mb-1">
              Entity ID
            </label>
            <Input
              id="sso-entity"
              value={entityId}
              onChange={(e) => setEntityId(e.target.value)}
            />
          </div>
          <div>
            <label htmlFor="sso-cert" className="block text-sm font-medium text-gray-700 mb-1">
              X.509 Certificate
            </label>
            <textarea
              id="sso-cert"
              value={certificate}
              onChange={(e) => setCertificate(e.target.value)}
              rows={4}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              placeholder="Paste your X.509 certificate here..."
            />
          </div>
        </>
      )}

      <div className="pt-2">
        <Button onClick={handleSave} loading={updateConfig.isPending}>
          Save SSO Configuration
        </Button>
      </div>
    </div>
  );
}
