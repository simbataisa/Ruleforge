import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { WebhookAuthConfigForm } from './WebhookAuthConfigForm';

function renderForm(
  authType: Parameters<typeof WebhookAuthConfigForm>[0]['authType'],
  authConfig: Parameters<typeof WebhookAuthConfigForm>[0]['authConfig'] = {},
) {
  const onChange = vi.fn();
  render(
    <WebhookAuthConfigForm
      authType={authType}
      authConfig={authConfig}
      onChange={onChange}
    />,
  );
  return { onChange };
}

describe('WebhookAuthConfigForm — Custom Header', () => {
  it('renders headerName and headerValue fields when CUSTOM_HEADER selected', () => {
    renderForm('CUSTOM_HEADER');
    expect(screen.getByLabelText(/header name/i)).toBeInTheDocument();
    // The header value input is a password field linked via htmlFor/id
    expect(screen.getByPlaceholderText(/enter header value/i)).toBeInTheDocument();
  });

  it('defaults headerName to X-Custom-Header', () => {
    renderForm('CUSTOM_HEADER');
    const headerNameInput = screen.getByLabelText(/header name/i);
    expect(headerNameInput).toHaveValue('X-Custom-Header');
  });

  it('does not render custom header fields for other auth types', () => {
    renderForm('BEARER_TOKEN');
    expect(screen.queryByLabelText(/header value/i)).not.toBeInTheDocument();
  });
});

describe('WebhookAuthConfigForm — OAuth 2.0', () => {
  it('renders tokenUrl, clientId, clientSecret, scope fields when OAUTH2 selected', () => {
    renderForm('OAUTH2_CLIENT_CREDENTIALS');
    expect(screen.getByLabelText(/token url/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/client id/i)).toBeInTheDocument();
    // Client secret is a password input — use placeholder to avoid ambiguity with eye-toggle aria-label
    expect(screen.getByPlaceholderText(/enter client secret/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/scope/i)).toBeInTheDocument();
  });

  it('shows HTTPS warning when tokenUrl starts with http://', () => {
    renderForm('OAUTH2_CLIENT_CREDENTIALS', { tokenUrl: 'http://insecure.example.com/token' });
    expect(screen.getByRole('alert')).toHaveTextContent('Token URL must use HTTPS');
  });

  it('does not show HTTPS warning for valid https:// tokenUrl', () => {
    renderForm('OAUTH2_CLIENT_CREDENTIALS', { tokenUrl: 'https://secure.example.com/token' });
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('does not show HTTPS warning when tokenUrl is empty', () => {
    renderForm('OAUTH2_CLIENT_CREDENTIALS', { tokenUrl: '' });
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('renders provider hints details element', () => {
    renderForm('OAUTH2_CLIENT_CREDENTIALS');
    const summary = screen.getByText(/where to find these values/i);
    expect(summary).toBeInTheDocument();
    expect(summary.closest('details')).toBeInTheDocument();
  });

  it('calls onChange when token URL is entered', () => {
    const { onChange } = renderForm('OAUTH2_CLIENT_CREDENTIALS');
    const tokenUrlInput = screen.getByLabelText(/token url/i);
    fireEvent.change(tokenUrlInput, { target: { value: 'https://auth.example.com/token' } });
    expect(onChange).toHaveBeenCalledWith(
      'OAUTH2_CLIENT_CREDENTIALS',
      expect.objectContaining({ tokenUrl: 'https://auth.example.com/token' }),
    );
  });
});
