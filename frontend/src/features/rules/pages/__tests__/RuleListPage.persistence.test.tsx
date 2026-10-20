import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test/test-utils';
import { RuleListPage } from '../RuleListPage';
import { useRules } from '@/api/rules.api';
import type { Rule } from '@/api/types/rule.types';

// Node's experimental global localStorage shadows jsdom's; use an in-memory one (as the other page tests do)
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
Object.defineProperty(window, 'localStorage', { value: localStorageMock });

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

const draftRule: Rule = {
  id: 'r1',
  spaceId: 's1',
  name: 'Loan Eligibility',
  type: 'TABLE',
  status: 'DRAFT',
  latestVersion: 1,
  locked: false,
  createdAt: '2026-02-01T00:00:00Z',
  updatedAt: '2026-02-20T00:00:00Z',
} as Rule;

const mockRefetch = vi.fn();
const mockDelete = vi.fn();

vi.mock('@/api/rules.api', () => ({
  useRules: vi.fn(),
  useCreateRule: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useCreateSpaceRule: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useCopyRule: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useDeleteRule: vi.fn(() => ({ mutate: mockDelete, isPending: false })),
  useTags: vi.fn(() => ({ data: [] })),
}));

vi.mock('@/api/org.api', () => ({
  useSpace: vi.fn(() => ({ data: { id: 's1', name: 'Retail Lending' } })),
}));

vi.mock('@/api/folders.api', () => ({
  useFolders: vi.fn(() => ({ data: [], isLoading: false, refetch: vi.fn() })),
  useCreateFolder: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
}));

vi.mock('@/stores/toast.store', () => ({
  useToastStore: vi.fn((sel: (s: { addToast: () => void }) => unknown) => sel({ addToast: vi.fn() })),
}));

vi.mock('@/stores/auth.store', () => ({
  useAuthStore: vi.fn((sel: (s: { user: { id: string } }) => unknown) => sel({ user: { id: 'user-1' } })),
}));

vi.mock('@/features/rules/components/CreateRuleModal', () => ({ CreateRuleModal: () => null }));
vi.mock('@/features/rules/components/CreateFolderModal', () => ({ CreateFolderModal: () => null }));
vi.mock('@/features/rules/components/ImportRuleModal', () => ({ ImportRuleModal: () => null }));
vi.mock('@/features/rules/components/ExportRuleButton', () => ({ ExportRuleButton: () => null }));
vi.mock('@/features/rules/components/RequestApprovalModal', () => ({ RequestApprovalModal: () => null }));

function mockRulesResult(overrides: Record<string, unknown> = {}) {
  vi.mocked(useRules).mockReturnValue({
    data: { content: [draftRule], totalPages: 3, totalElements: 60 },
    isLoading: false,
    isFetching: false,
    isError: false,
    refetch: mockRefetch,
    ...overrides,
  } as unknown as ReturnType<typeof useRules>);
}

describe('RuleListPage URL-persisted state', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    localStorage.clear();
    mockRulesResult();
  });

  it('restores search, filters, sort, page and space from the URL', () => {
    renderWithProviders(<RuleListPage />, {
      wrapperOptions: {
        initialEntries: ['/rules?q=loan&types=TABLE&statuses=DRAFT&page=2&sort=name:asc&spaceId=s1'],
      },
    });

    expect(useRules).toHaveBeenLastCalledWith({
      page: 1,
      size: 25,
      sort: 'name,asc',
      search: 'loan',
      types: ['TABLE'],
      statuses: ['DRAFT'],
      spaceId: 's1',
    });
    expect(screen.getByPlaceholderText(/search rules/i)).toHaveValue('loan');
    expect(screen.getByRole('combobox', { name: 'Sort rules' })).toHaveValue('name:asc');
  });

  it('remembers the list URL so the editor can link back to it', () => {
    renderWithProviders(<RuleListPage />, {
      wrapperOptions: { initialEntries: ['/rules?q=loan&statuses=DRAFT'] },
    });

    expect(sessionStorage.getItem('ruleforge:rules-list-path')).toBe('/rules?q=loan&statuses=DRAFT');
  });

  it('shows an error state with retry instead of "No rules yet" when loading fails', async () => {
    mockRulesResult({ data: undefined, isError: true });
    renderWithProviders(<RuleListPage />, { wrapperOptions: { initialEntries: ['/rules'] } });

    expect(screen.getByText("Couldn't load rules")).toBeInTheDocument();
    expect(screen.queryByText('No rules yet')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(mockRefetch).toHaveBeenCalled();
  });

  it('asks for confirmation in a dialog before deleting', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm');
    renderWithProviders(<RuleListPage />, { wrapperOptions: { initialEntries: ['/rules'] } });

    const row = screen.getByText('Loan Eligibility').closest('tr')!;
    await userEvent.click(row.querySelector('td:last-child button')!);
    await userEvent.click(screen.getByRole('button', { name: /delete/i }));

    expect(confirmSpy).not.toHaveBeenCalled();
    expect(screen.getByText(/are you sure you want to delete "Loan Eligibility"/i)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Delete Rule' }));
    expect(mockDelete).toHaveBeenCalledWith('r1', expect.anything());
  });

  it('opens the action menu from card view', async () => {
    localStorage.setItem('ruleforge:rules-view-mode', 'card');
    renderWithProviders(<RuleListPage />, { wrapperOptions: { initialEntries: ['/rules'] } });

    expect(screen.queryByRole('button', { name: /duplicate/i })).not.toBeInTheDocument();
    const card = screen.getByText('Loan Eligibility').closest('.group')!;
    const buttons = card.querySelectorAll('button');
    await userEvent.click(buttons[buttons.length - 1]);

    expect(screen.getByRole('button', { name: /duplicate/i })).toBeInTheDocument();
  });
});
