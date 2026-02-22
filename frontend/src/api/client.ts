import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { useToastStore } from '@/stores/toast.store';

interface RetryConfig extends InternalAxiosRequestConfig {
  _retryCount?: number;
}

const MAX_RETRIES = 3;
const RETRY_BASE_DELAY = 1000;

function getCookieValue(name: string): string | null {
  const match = document.cookie.match(new RegExp('(?:^|; )' + name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '=([^;]*)'));
  return match ? decodeURIComponent(match[1]) : null;
}

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for auth token and CSRF protection
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('ruleforge_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  // Attach CSRF token on mutating requests
  const method = config.method?.toUpperCase();
  if (method && method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS') {
    const csrfToken = getCookieValue('XSRF-TOKEN')
      || document.querySelector<HTMLMetaElement>('meta[name="csrf-token"]')?.content;
    if (csrfToken) {
      config.headers['X-XSRF-TOKEN'] = csrfToken;
    }
  }

  return config;
});

// Response interceptor for retry logic (503 GET only)
apiClient.interceptors.response.use(undefined, async (error: AxiosError) => {
  const config = error.config as RetryConfig | undefined;

  if (
    config &&
    error.response?.status === 503 &&
    config.method?.toUpperCase() === 'GET'
  ) {
    const retryCount = config._retryCount ?? 0;

    if (retryCount < MAX_RETRIES) {
      config._retryCount = retryCount + 1;
      const delay = RETRY_BASE_DELAY * Math.pow(2, retryCount);
      await new Promise((resolve) => setTimeout(resolve, delay));
      return apiClient(config);
    }
  }

  return Promise.reject(error);
});

// Response interceptor for error handling and toast notifications
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const addToast = useToastStore.getState().addToast;
    const status = error.response?.status;

    if (status === 401) {
      localStorage.removeItem('ruleforge_token');
      window.location.href = '/login';
    } else if (status === 429) {
      const retryAfter = error.response?.headers?.['retry-after'];
      const message = retryAfter
        ? `Please wait ${retryAfter} seconds before retrying.`
        : undefined;
      addToast({
        type: 'warning',
        title: 'Too many requests. Please wait and try again.',
        message,
      });
    } else if (status && status >= 500) {
      addToast({
        type: 'error',
        title: 'Server error. Please try again.',
      });
    } else if (!error.response) {
      addToast({
        type: 'error',
        title: 'Network error. Check your connection.',
      });
    }

    return Promise.reject(error);
  },
);
