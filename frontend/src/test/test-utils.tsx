import { render, type RenderOptions } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import type { ReactElement, ReactNode } from 'react';

interface WrapperOptions {
  initialEntries?: string[];
}

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
      },
      mutations: {
        retry: false,
      },
    },
  });
}

export function renderWithProviders(
  ui: ReactElement,
  options?: Omit<RenderOptions, 'wrapper'> & { wrapperOptions?: WrapperOptions },
) {
  const { wrapperOptions, ...renderOptions } = options ?? {};

  const router = createMemoryRouter(
    [{ path: '*', element: ui }],
    { initialEntries: wrapperOptions?.initialEntries ?? ['/'] },
  );

  const queryClient = createTestQueryClient();

  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        {children}
      </QueryClientProvider>
    );
  }

  return render(<RouterProvider router={router} />, {
    wrapper: Wrapper,
    ...renderOptions,
  });
}

export { createTestQueryClient };
