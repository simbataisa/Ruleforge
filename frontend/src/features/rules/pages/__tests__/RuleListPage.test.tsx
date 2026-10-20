import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test/test-utils';
import { RuleListPage } from '../RuleListPage';
import type { Rule } from '@/api/types/rule.types';

// Ensure localStorage is available in jsdom
const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: vi.fn((key: string) => store[key] ?? null),
    setItem: vi.fn((key: string, value: string) => { store[key] = value; }),
    removeItem: vi.fn((key: string) => { delete store[key]; }),
    clear: vi.fn(() => { store = {}; }),
    get length() { return Object.keys(store).length; },
    key: vi.fn((i: number) => Object.keys(store)[i] ?? null),
  };
})();
Object.defineProperty(window, 'localStorage', { value: localStorageMock });

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const mockRules: Rule[] = [
  {
    id: 'r1',
    spaceId: 's1',
    name: 'Pricing Rule',
    type: 'TABLE',
    status: 'DRAFT',
    latestVersion: 2,
    locked: false,
    tags: ['pricing', 'v2'],
    createdAt: '2026-02-01T00:00:00Z',
    updatedAt: '2026-02-20T00:00:00Z',
  },
  {
    id: 'r2',
    spaceId: 's1',
    name: 'Eligibility Rule',
    type: 'TREE',
    status: 'PUBLISHED',
    latestVersion: 5,
    locked: false,
    tags: ['eligibility'],
    createdAt: '2026-01-15T00:00:00Z',
    updatedAt: '2026-02-18T00:00:00Z',
  },
  {
    id: 'r3',
    spaceId: 's1',
    name: 'Discount Rule',
    type: 'SCRIPT',
    status: 'DRAFT',
    latestVersion: 1,
    locked: false,
    createdAt: '2026-02-10T00:00:00Z',
    updatedAt: '2026-02-22T00:00:00Z',
  },
];

const mockRefetch = vi.fn();

vi.mock('@/api/rules.api', () => ({
  useRules: vi.fn(() => ({
    data: { content: mockRules, totalPages: 1, totalElements: 3 },
    isLoading: false,
    isFetching: false,
    refetch: mockRefetch,
  })),
  useSpaceRules: vi.fn(() => ({
    data: { content: mockRules, totalPages: 1, totalElements: 3 },
    isLoading: false,
    isFetching: false,
    refetch: mockRefetch,
  })),
  useCreateRule: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useCreateSpaceRule: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useCopyRule: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useDeleteRule: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useLockRule: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useArchiveRule: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useRestoreRule: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useUnlockRule: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useBatchExport: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
  useUpdateRule: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
  useTags: vi.fn(() => ({ data: [{ tag: 'pricing', count: 2 }, { tag: 'v2', count: 1 }] })),
}));

vi.mock('@/api/folders.api', () => ({
  useFolders: vi.fn(() => ({ data: [], isLoading: false, refetch: vi.fn() })),
  useCreateFolder: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
}));

const mockAddToast = vi.fn();
vi.mock('@/stores/toast.store', () => ({
  useToastStore: vi.fn((sel: (s: { addToast: typeof mockAddToast }) => unknown) =>
    sel({ addToast: mockAddToast }),
  ),
}));

vi.mock('@/stores/auth.store', () => ({
  useAuthStore: vi.fn((sel: (s: { user: { id: string; role: string } }) => unknown) =>
    sel({ user: { id: 'user-1', role: 'EDITOR' } }),
  ),
}));

// Mock child components that are complex or have their own API calls
vi.mock('@/features/rules/components/FolderTree', () => ({
  FolderTree: () => <div data-testid="folder-tree">FolderTree</div>,
}));

vi.mock('@/features/rules/components/FilterPanel', () => ({
  FilterPanel: () => <div data-testid="filter-panel">FilterPanel</div>,
}));

vi.mock('@/features/rules/components/CreateRuleModal', () => ({
  CreateRuleModal: () => null,
}));

vi.mock('@/features/rules/components/CreateFolderModal', () => ({
  CreateFolderModal: () => null,
}));

vi.mock('@/features/rules/components/ImportRuleModal', () => ({
  ImportRuleModal: () => null,
}));

vi.mock('@/features/rules/components/ExportRuleButton', () => ({
  ExportRuleButton: () => null,
}));

vi.mock('@/features/rules/components/RequestApprovalModal', () => ({
  RequestApprovalModal: ({ open }: { open: boolean }) =>
    open ? <div data-testid="approval-modal">ApprovalModal</div> : null,
}));

describe('RuleListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorageMock.clear();
  });

  it('renders rule names in the table', () => {
    renderWithProviders(<RuleListPage />);
    expect(screen.getByText('Pricing Rule')).toBeInTheDocument();
    expect(screen.getByText('Eligibility Rule')).toBeInTheDocument();
    expect(screen.getByText('Discount Rule')).toBeInTheDocument();
  });

  it('renders search input', () => {
    renderWithProviders(<RuleListPage />);
    const searchInput = screen.getByPlaceholderText(/search rules/i);
    expect(searchInput).toBeInTheDocument();
  });

  it('renders checkboxes for row selection', () => {
    renderWithProviders(<RuleListPage />);
    // Select-all checkbox + one per rule = 4
    const checkboxes = screen.getAllByRole('checkbox');
    expect(checkboxes.length).toBe(4); // 1 header + 3 rows
  });

  it('selects a row when checkbox is clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RuleListPage />);

    const checkboxes = screen.getAllByRole('checkbox');
    // Click first rule's checkbox (index 1, 0 is select-all)
    await user.click(checkboxes[1]);
    expect(checkboxes[1]).toBeChecked();
  });

  it('select all checkbox selects all rows', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RuleListPage />);

    const checkboxes = screen.getAllByRole('checkbox');
    // Click select-all (index 0)
    await user.click(checkboxes[0]);

    // All row checkboxes should be checked
    expect(checkboxes[1]).toBeChecked();
    expect(checkboxes[2]).toBeChecked();
    expect(checkboxes[3]).toBeChecked();
  });

  it('shows batch action bar when rows are selected', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RuleListPage />);

    // Select all via header checkbox
    const checkboxes = screen.getAllByRole('checkbox');
    await user.click(checkboxes[0]);

    // BatchActionBar shows selected count
    expect(screen.getByText(/3 rules selected/i)).toBeInTheDocument();
  });

  it('shows "Request Approval" in batch bar when all selected are DRAFT', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RuleListPage />);

    // Select only the two DRAFT rules (r1 and r3)
    const checkboxes = screen.getAllByRole('checkbox');
    await user.click(checkboxes[1]); // r1 - DRAFT
    await user.click(checkboxes[3]); // r3 - DRAFT

    expect(screen.getByText(/request approval/i)).toBeInTheDocument();
  });

  it('hides "Request Approval" in batch bar when selection includes non-DRAFT', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RuleListPage />);

    // Select all (includes r2 which is PUBLISHED)
    const checkboxes = screen.getAllByRole('checkbox');
    await user.click(checkboxes[0]);

    // allDraft should be false, so Request Approval button should not appear
    // Only Export button should be visible in batch bar
    const toolbar = screen.getByRole('toolbar');
    expect(within(toolbar).queryByText(/request approval/i)).not.toBeInTheDocument();
    expect(within(toolbar).getByText(/export/i)).toBeInTheDocument();
  });

  it('renders pagination info', () => {
    renderWithProviders(<RuleListPage />);
    expect(screen.getByText(/showing 1-3 of 3 rules/i)).toBeInTheDocument();
  });

  it('renders Create Rule button', () => {
    renderWithProviders(<RuleListPage />);
    expect(screen.getByText('Create Rule')).toBeInTheDocument();
  });

  it('renders view toggle buttons (table/card)', () => {
    renderWithProviders(<RuleListPage />);
    expect(screen.getByTitle('Table view')).toBeInTheDocument();
    expect(screen.getByTitle('Card view')).toBeInTheDocument();
  });
});
