import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test/test-utils';
import { RuleListPage } from './RuleListPage';

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

const mockRules = [
  {
    id: 'rule-1',
    spaceId: 'space-1',
    name: 'Pricing Rule',
    type: 'TABLE',
    status: 'PUBLISHED',
    latestVersion: 3,
    locked: false,
    createdAt: '2025-01-15T10:00:00Z',
    updatedAt: '2025-06-01T14:30:00Z',
  },
  {
    id: 'rule-2',
    spaceId: 'space-1',
    name: 'Eligibility Check',
    type: 'TREE',
    status: 'DRAFT',
    latestVersion: 1,
    locked: false,
    createdAt: '2025-03-20T08:00:00Z',
    updatedAt: '2025-04-10T12:00:00Z',
  },
];

vi.mock('@/api/rules.api', () => ({
  useRules: () => ({
    data: { content: mockRules, page: 0, size: 20, totalElements: 2, totalPages: 1 },
    isLoading: false,
    isFetching: false,
    refetch: vi.fn(),
  }),
  useSpaceRules: () => ({
    data: { content: mockRules, page: 0, size: 20, totalElements: 2, totalPages: 1 },
    isLoading: false,
    isFetching: false,
    refetch: vi.fn(),
  }),
  useCreateRule: () => ({ mutate: vi.fn(), isPending: false }),
  useCreateSpaceRule: () => ({ mutate: vi.fn(), isPending: false }),
  useCopyRule: () => ({ mutate: vi.fn(), isPending: false }),
  useDeleteRule: () => ({ mutate: vi.fn(), isPending: false }),
  useLockRule: () => ({ mutate: vi.fn(), isPending: false }),
  useArchiveRule: () => ({ mutate: vi.fn(), isPending: false }),
  useRestoreRule: () => ({ mutate: vi.fn(), isPending: false }),
  useUnlockRule: () => ({ mutate: vi.fn(), isPending: false }),
  useBatchExport: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useUpdateRule: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useTags: () => ({ data: [] }),
}));

vi.mock('@/api/folders.api', () => ({
  useFolders: () => ({ data: [], isLoading: false, refetch: vi.fn() }),
  useCreateFolder: () => ({ mutate: vi.fn(), isPending: false }),
}));

vi.mock('@/stores/toast.store', () => ({
  useToastStore: vi.fn((sel: (s: { addToast: () => void }) => unknown) =>
    sel({ addToast: vi.fn() }),
  ),
}));

vi.mock('@/stores/auth.store', () => ({
  useAuthStore: vi.fn((sel: (s: { user: { id: string; role: string } }) => unknown) =>
    sel({ user: { id: 'user-1', role: 'EDITOR' } }),
  ),
}));

vi.mock('@/features/rules/components/FolderTree', () => ({
  FolderTree: () => <div data-testid="folder-tree">FolderTree</div>,
}));

vi.mock('@/features/rules/components/FilterPanel', () => ({
  FilterPanel: () => <div data-testid="filter-panel">FilterPanel</div>,
}));

vi.mock('@/features/rules/components/CreateRuleModal', () => ({
  CreateRuleModal: ({ open }: { open: boolean }) =>
    open ? <div data-testid="create-rule-modal">CreateRuleModal</div> : null,
}));

vi.mock('@/features/rules/components/CreateFolderModal', () => ({
  CreateFolderModal: ({ open }: { open: boolean }) =>
    open ? <div data-testid="create-folder-modal">CreateFolderModal</div> : null,
}));

vi.mock('@/features/rules/components/ImportRuleModal', () => ({
  ImportRuleModal: () => null,
}));

vi.mock('@/features/rules/components/ExportRuleButton', () => ({
  ExportRuleButton: () => null,
}));

vi.mock('@/features/rules/components/RequestApprovalModal', () => ({
  RequestApprovalModal: () => null,
}));

describe('RuleListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the page header with title and create button', () => {
    renderWithProviders(<RuleListPage />);
    expect(screen.getByText('Rules')).toBeInTheDocument();
    expect(screen.getByText('Create Rule')).toBeInTheDocument();
  });

  it('renders the rules in the data table', () => {
    renderWithProviders(<RuleListPage />);
    expect(screen.getByText('Pricing Rule')).toBeInTheDocument();
    expect(screen.getByText('Eligibility Check')).toBeInTheDocument();
  });

  it('renders search input', () => {
    renderWithProviders(<RuleListPage />);
    expect(screen.getByPlaceholderText(/search rules/i)).toBeInTheDocument();
  });

  it('renders the folder tree sidebar', () => {
    renderWithProviders(<RuleListPage />);
    expect(screen.getByTestId('folder-tree')).toBeInTheDocument();
  });

  it('opens create rule modal on button click', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RuleListPage />);

    expect(screen.queryByTestId('create-rule-modal')).not.toBeInTheDocument();
    await user.click(screen.getByText('Create Rule'));
    expect(screen.getByTestId('create-rule-modal')).toBeInTheDocument();
  });
});
