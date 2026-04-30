import { describe, it, expect, vi, beforeEach } from 'vitest';

// Provide a working localStorage mock since jsdom's may be broken
const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: (key: string) => store[key] ?? null,
    setItem: (key: string, value: string) => { store[key] = value; },
    removeItem: (key: string) => { delete store[key]; },
    clear: () => { store = {}; },
    get length() { return Object.keys(store).length; },
    key: (i: number) => Object.keys(store)[i] ?? null,
  };
})();

Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock, writable: true });

describe('API Client', () => {
  beforeEach(() => {
    vi.resetModules();
    localStorageMock.clear();
  });

  it('creates axios instance with correct headers', async () => {
    const { apiClient } = await import('./client');
    expect(apiClient.defaults.headers['Content-Type']).toBe('application/json');
  });

  describe('request interceptor - auth token', () => {
    it('adds Authorization header when token exists in localStorage', async () => {
      localStorageMock.setItem('ruleforge_token', 'test-token-123');

      const { apiClient } = await import('./client');

      const interceptors = apiClient.interceptors.request as unknown as {
        handlers: Array<{ fulfilled: (config: Record<string, unknown>) => Record<string, unknown> }>;
      };
      const handler = interceptors.handlers[0].fulfilled;

      const config = { headers: {} as Record<string, string> };
      const result = handler(config);
      expect((result.headers as Record<string, string>).Authorization).toBe('Bearer test-token-123');
    });

    it('does not add Authorization header when no token', async () => {
      const { apiClient } = await import('./client');

      const interceptors = apiClient.interceptors.request as unknown as {
        handlers: Array<{ fulfilled: (config: Record<string, unknown>) => Record<string, unknown> }>;
      };
      const handler = interceptors.handlers[0].fulfilled;

      const config = { headers: {} as Record<string, string> };
      const result = handler(config);
      expect((result.headers as Record<string, string>).Authorization).toBeUndefined();
    });
  });

  describe('request interceptor - CSRF token', () => {
    it('reads the CSRF cookie without constructing a RegExp', async () => {
      Object.defineProperty(document, 'cookie', {
        configurable: true,
        value: 'session=abc; XSRF-TOKEN=csrf-token-123',
      });

      const OriginalRegExp = globalThis.RegExp;
      vi.stubGlobal('RegExp', vi.fn(() => {
        throw new Error('RegExp should not be used for cookie parsing');
      }));

      try {
        const { apiClient } = await import('./client');

        const interceptors = apiClient.interceptors.request as unknown as {
          handlers: Array<{ fulfilled: (config: Record<string, unknown>) => Record<string, unknown> }>;
        };
        const handler = interceptors.handlers[0].fulfilled;

        const config = { method: 'post', headers: {} as Record<string, string> };
        const result = handler(config);

        expect((result.headers as Record<string, string>)['X-XSRF-TOKEN']).toBe('csrf-token-123');
      } finally {
        vi.stubGlobal('RegExp', OriginalRegExp);
      }
    });
  });

  describe('response interceptor - 401 handling', () => {
    it('removes token and redirects on 401', async () => {
      localStorageMock.setItem('ruleforge_token', 'old-token');

      const { apiClient } = await import('./client');

      const interceptors = apiClient.interceptors.response as unknown as {
        handlers: Array<{
          fulfilled: (response: unknown) => unknown;
          rejected: (error: unknown) => unknown;
        }>;
      };
      // handlers[1] is the error-handling interceptor (handlers[0] is 503 retry)
      const errorHandler = interceptors.handlers[1].rejected;

      const originalHref = window.location.href;
      Object.defineProperty(window, 'location', {
        writable: true,
        value: { href: '' },
      });

      try {
        await errorHandler({ response: { status: 401 } });
      } catch {
        // Expected
      }

      expect(localStorageMock.getItem('ruleforge_token')).toBeNull();
      expect(window.location.href).toBe('/login');

      Object.defineProperty(window, 'location', {
        writable: true,
        value: { href: originalHref },
      });
    });

    it('rejects non-401 errors without redirect', async () => {
      const { apiClient } = await import('./client');

      const interceptors = apiClient.interceptors.response as unknown as {
        handlers: Array<{
          fulfilled: (response: unknown) => unknown;
          rejected: (error: unknown) => unknown;
        }>;
      };
      // handlers[1] is the error-handling interceptor (handlers[0] is 503 retry)
      const errorHandler = interceptors.handlers[1].rejected;

      const error = { response: { status: 500 } };
      await expect(errorHandler(error)).rejects.toEqual(error);
    });
  });
});
