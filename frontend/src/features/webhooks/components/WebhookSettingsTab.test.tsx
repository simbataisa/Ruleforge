import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { WebhookSettingsTab } from './WebhookSettingsTab';
import type { Webhook } from '@/api/webhooks.api';

const mutateMock = vi.fn();
let mockRole = 'OWNER';

vi.mock('@/api/webhooks.api', async () => {
  const actual = await vi.importActual<typeof import('@/api/webhooks.api')>('@/api/webhooks.api');
  return {
    ...actual,
    useUpdateWebhook: () => ({ mutate: mutateMock, isPending: false }),
    useDeleteWebhook: () => ({ mutate: vi.fn(), isPending: false }),
  };
});

vi.mock('@/stores/toast.store', () => ({
  useToastStore: vi.fn((sel: (s: { addToast: () => void }) => unknown) =>
    sel({ addToast: vi.fn() }),
  ),
}));

vi.mock('@/stores/auth.store', () => ({
  useAuthStore: vi.fn((sel: (s: { user: { id: string; role: string } }) => unknown) =>
    sel({ user: { id: 'user-1', role: mockRole } }),
  ),
}));

function webhook(overrides: Partial<Webhook> = {}): Webhook {
  return {
    id: 'webhook-1',
    url: 'https://hooks.example.com/ruleforge',
    events: ['rule.published'],
    secret: '',
    active: true,
    createdAt: '2026-04-01T00:00:00Z',
    updatedAt: '2026-04-01T00:00:00Z',
    scopeType: 'ORG',
    scopeId: 'org-1',
    name: 'Production webhook',
    circuitStatus: 'CLOSED',
    consecutiveFailures: 0,
    ...overrides,
  };
}

describe('WebhookSettingsTab — OAuth2 and Custom Header', () => {
  beforeEach(() => {
    mutateMock.mockReset();
    mockRole = 'OWNER';
  });

  it('preloads OAuth2 non-sensitive fields in edit mode', () => {
    render(
      <WebhookSettingsTab
        webhook={webhook({
          authType: 'OAUTH2_CLIENT_CREDENTIALS',
          authConfig: {
            tokenUrl: 'https://auth.example.com/token',
            clientId: 'client-id',
            clientSecret: '[REDACTED]',
            scope: 'api:read',
          },
        })}
        onUpdated={vi.fn()}
        onDeleted={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Edit' }));

    expect(screen.getByLabelText(/token url/i)).toHaveValue('https://auth.example.com/token');
    expect(screen.getByLabelText(/client id/i)).toHaveValue('client-id');
    expect(screen.getByLabelText(/scope/i)).toHaveValue('api:read');
  });

  it('rotates OAuth2 client secret with required companion fields', () => {
    render(
      <WebhookSettingsTab
        webhook={webhook({
          authType: 'OAUTH2_CLIENT_CREDENTIALS',
          authConfig: {
            tokenUrl: 'https://auth.example.com/token',
            clientId: 'client-id',
            clientSecret: '[REDACTED]',
            scope: 'api:read',
          },
        })}
        onUpdated={vi.fn()}
        onDeleted={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: /rotate secret/i }));
    fireEvent.change(screen.getByPlaceholderText(/enter new client secret/i), {
      target: { value: 'new-client-secret' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(mutateMock).toHaveBeenCalledWith(
      {
        id: 'webhook-1',
        authType: 'OAUTH2_CLIENT_CREDENTIALS',
        authConfig: {
          tokenUrl: 'https://auth.example.com/token',
          clientId: 'client-id',
          clientSecret: 'new-client-secret',
          scope: 'api:read',
        },
      },
      expect.any(Object),
    );
  });

  it('rotates Custom Header value with headerName and headerValue', () => {
    render(
      <WebhookSettingsTab
        webhook={webhook({
          authType: 'CUSTOM_HEADER',
          authConfig: {
            headerName: 'X-Service-Key',
            headerValue: '[REDACTED]',
          },
        })}
        onUpdated={vi.fn()}
        onDeleted={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: /rotate value/i }));
    fireEvent.change(screen.getByPlaceholderText(/enter new header value/i), {
      target: { value: 'new-header-value' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(mutateMock).toHaveBeenCalledWith(
      {
        id: 'webhook-1',
        authType: 'CUSTOM_HEADER',
        authConfig: {
          headerName: 'X-Service-Key',
          headerValue: 'new-header-value',
        },
      },
      expect.any(Object),
    );
  });

  it('shows only auth type label to VIEWER users', () => {
    mockRole = 'VIEWER';

    render(
      <WebhookSettingsTab
        webhook={webhook({
          authType: 'OAUTH2_CLIENT_CREDENTIALS',
          authConfig: {
            tokenUrl: 'https://auth.example.com/token',
            clientId: 'client-id',
            clientSecret: '[REDACTED]',
            scope: 'api:read',
          },
        })}
        onUpdated={vi.fn()}
        onDeleted={vi.fn()}
      />,
    );

    expect(screen.getByText('Authentication: OAuth 2.0 Client Credentials')).toBeInTheDocument();
    expect(screen.queryByText(/token url:/i)).not.toBeInTheDocument();
    expect(screen.queryByText('[REDACTED]')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /rotate secret/i })).not.toBeInTheDocument();
  });
});
