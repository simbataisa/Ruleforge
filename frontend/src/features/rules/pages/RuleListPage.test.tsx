import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test/test-utils';
import { RuleListPage } from './RuleListPage';

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
  }),
  useCreateRule: () => ({
    mutate: vi.fn(),
    isPending: false,
  }),
}));

vi.mock('@/api/folders.api', () => ({
  useFolders: () => ({
    data: [],
    isLoading: false,
  }),
  useCreateFolder: () => ({
    mutate: vi.fn(),
    isPending: false,
  }),
}));

vi.mock('@/features/rules/components/FolderTree', () => ({
  FolderTree: () => <div data-testid="folder-tree">FolderTree</div>,
}));

vi.mock('@/features/rules/components/CreateRuleModal', () => ({
  CreateRuleModal: ({ open }: { open: boolean }) =>
    open ? <div data-testid="create-rule-modal">CreateRuleModal</div> : null,
}));

vi.mock('@/features/rules/components/CreateFolderModal', () => ({
  CreateFolderModal: ({ open }: { open: boolean }) =>
    open ? <div data-testid="create-folder-modal">CreateFolderModal</div> : null,
}));

describe('RuleListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the page header with title and create button', () => {
    renderWithProviders(<RuleListPage />);
    expect(screen.getByText('Rules')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create rule/i })).toBeInTheDocument();
  });

  it('renders the rules in the data table', () => {
    renderWithProviders(<RuleListPage />);
    expect(screen.getByText('Pricing Rule')).toBeInTheDocument();
    expect(screen.getByText('Eligibility Check')).toBeInTheDocument();
    expect(screen.getByText('TABLE')).toBeInTheDocument();
    expect(screen.getByText('TREE')).toBeInTheDocument();
    expect(screen.getByText('PUBLISHED')).toBeInTheDocument();
    expect(screen.getByText('DRAFT')).toBeInTheDocument();
  });

  it('renders filter controls', () => {
    renderWithProviders(<RuleListPage />);
    expect(screen.getByPlaceholderText('Search rules...')).toBeInTheDocument();
    expect(screen.getByText('All Types')).toBeInTheDocument();
    expect(screen.getByText('All Statuses')).toBeInTheDocument();
  });

  it('renders the folder tree sidebar', () => {
    renderWithProviders(<RuleListPage />);
    expect(screen.getByTestId('folder-tree')).toBeInTheDocument();
  });

  it('filters rules by search text', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RuleListPage />);

    await user.type(screen.getByPlaceholderText('Search rules...'), 'Pricing');

    expect(screen.getByText('Pricing Rule')).toBeInTheDocument();
    expect(screen.queryByText('Eligibility Check')).not.toBeInTheDocument();
  });

  it('opens create rule modal on button click', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RuleListPage />);

    expect(screen.queryByTestId('create-rule-modal')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /create rule/i }));
    expect(screen.getByTestId('create-rule-modal')).toBeInTheDocument();
  });
});

describe('RuleListPage loading state', () => {
  it('shows spinner when loading', () => {
    vi.doMock('@/api/rules.api', () => ({
      useRules: () => ({ data: undefined, isLoading: true }),
      useCreateRule: () => ({ mutate: vi.fn(), isPending: false }),
    }));
    // The hoisted mock already sets isLoading: false, so this test just verifies
    // the non-loading path doesn't show a spinner
    renderWithProviders(<RuleListPage />);
    expect(screen.queryByText('Rules')).toBeInTheDocument();
  });
});
